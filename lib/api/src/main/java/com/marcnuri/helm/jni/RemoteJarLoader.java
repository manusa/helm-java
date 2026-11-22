/*
 * Copyright 2024 Marc Nuri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.marcnuri.helm.jni;

import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Downloads the native library JAR for the current platform from the Maven repository, verifies it against the
 * repository's SHA-1 checksum, and caches it in the per-user {@link CacheDirectory}.
 *
 * <p>When a JAR is verified and cached, its checksum and size are recorded next to it ({@code <jar>.verified}). A
 * cached JAR is used without hashing it again if it's still a private file (a regular file that, on POSIX file
 * systems, belongs to the current user and isn't writable by others) of the recorded size; otherwise it's downloaded
 * and verified again.
 *
 * @author Marc Nuri
 */
class RemoteJarLoader {

  private static final String VERSION = "com.marcnuri.jkube-helm.version";
  private static final String GROUP_ID = "com.marcnuri.jkube-helm.groupId";
  private static final String SNAPSHOTS = "com.marcnuri.jkube-helm.repository.snapshots";
  private static final String RELEASES = "com.marcnuri.jkube-helm.repository.releases";
  // For testing
  private static final String FORCE_UPDATE = "com.marcnuri.jkube-helm.forceUpdate";
  private static final String SKIP = "com.marcnuri.jkube-helm.skipRemoteJar";
  // Without them, an unresponsive repository blocks the native library lookup forever
  static final int CONNECT_TIMEOUT_MILLIS = 10_000;
  static final int READ_TIMEOUT_MILLIS = 30_000;
  private static final String CHECKSUM_EXTENSION = ".sha1";
  private static final String RECORD_EXTENSION = ".verified";
  private static final Pattern SHA1 = Pattern.compile("[0-9a-f]{40}");
  private static final Pattern SNAPSHOT_TIMESTAMP = Pattern.compile("\\d{8}\\.\\d{6}");
  private static final Pattern SNAPSHOT_BUILD_NUMBER = Pattern.compile("\\d+");
  // Downloads still in progress keep writing (or time out after READ_TIMEOUT_MILLIS), so a .part file not modified
  // for this long was left behind by a JVM that stopped mid-download
  static final long STALE_PART_FILE_MILLIS = TimeUnit.HOURS.toMillis(1);
  private static final String PART_EXTENSION = ".part";
  // The names partFile() generates for a JAR or its record: <file>.<random UUID>.part
  private static final Pattern PART_FILE = Pattern.compile(
    ".+\\.jar(\\" + RECORD_EXTENSION + ")?\\.[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\" + PART_EXTENSION);

  private RemoteJarLoader() {
  }

  /**
   * Downloads (or reuses the cached) native library JAR for the current platform.
   *
   * @return a class loader for the JAR, or {@code null} if the remote JAR is disabled or the platform is unknown.
   * @throws IOException if the JAR (or the snapshot metadata) can't be downloaded or verified, or if the cache
   *                     directory can't be used.
   */
  static ClassLoader remoteJar() throws IOException {
    // For testing
    if (System.getProperty(SKIP) != null) {
      return null;
    }
    final Properties nativeLibraries = new Properties();
    try (final InputStream stream = NativeLibrary.class.getResourceAsStream("/META-INF/native-libraries.properties")) {
      nativeLibraries.load(stream);
    }
    final String version;
    // Version can be overridden (for testing) using a system property
    if (System.getProperty(VERSION) != null) {
      version = System.getProperty(VERSION);
    } else {
      version = nativeLibraries.getProperty(VERSION);
    }
    final String repository = version.endsWith("-SNAPSHOT") ?
      nativeLibraries.getProperty(SNAPSHOTS) :
      nativeLibraries.getProperty(RELEASES);
    final String osName = Platform.osName();
    final String archName = Platform.archName();
    if (osName == null || archName == null) {
      return null;
    }
    final CacheDirectory cacheDirectory = CacheDirectory.select();
    try {
      return remoteJar(repository, nativeLibraries.getProperty(GROUP_ID), version, osName, archName,
        cacheDirectory.getDirectory());
    } finally {
      if (cacheDirectory.isTemporary()) {
        deleteOnExit(cacheDirectory.getDirectory());
      }
    }
  }

  /**
   * Registers the files in the (temporary) directory to be deleted when the JVM exits, whether the download
   * succeeded or not. They're registered after the directory, so they're deleted before it (in reverse order). On
   * Windows, the loaded JAR is still open then (its class loader is never closed), so it and the directory are left
   * behind in the temporary directory.
   */
  private static void deleteOnExit(Path temporaryDirectory) {
    try (DirectoryStream<Path> files = Files.newDirectoryStream(temporaryDirectory)) {
      for (Path file : files) {
        file.toFile().deleteOnExit();
      }
    } catch (IOException exception) {
      // Best effort
    }
  }

  static ClassLoader remoteJar(String repository, String groupId, String version, String osName, String archName,
    Path cacheDirectory) throws IOException {
    final String groupUrl = repository + "/" + groupId.replace('.', '/') + "/" + osName + "-" + archName + "/" + version;
    final String jarName;
    if (version.endsWith("-SNAPSHOT")) {
      jarName = latestSnapshot(version, osName, archName, groupUrl);
    } else {
      jarName = osName + "-" + archName + "-" + version + ".jar";
    }
    final URL jarUrl = new URL(groupUrl + "/" + jarName);
    return new URLClassLoader(new URL[]{cache(jarUrl, cacheDirectory.resolve(jarName))}, NativeLibrary.class.getClassLoader());
  }

  private static String latestSnapshot(String version, String osName, String archName, String groupUrl)
    throws IOException {
    final String metadataXml = groupUrl + "/maven-metadata.xml";
    final String timestamp;
    final String buildNumber;
    try (InputStream stream = openStream(new URL(metadataXml), CONNECT_TIMEOUT_MILLIS, READ_TIMEOUT_MILLIS)) {
      final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
      factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
      final DocumentBuilder documentBuilder = factory.newDocumentBuilder();
      // The default handler also prints parse errors to stderr; they're already reported with the exception
      documentBuilder.setErrorHandler(new ThrowingErrorHandler());
      final Document xml = documentBuilder.parse(stream, metadataXml);
      final XPath xPath = XPathFactory.newInstance().newXPath();
      timestamp = xPath.evaluate("//metadata/versioning/snapshot/timestamp", xml).trim();
      buildNumber = xPath.evaluate("//metadata/versioning/snapshot/buildNumber", xml).trim();
    } catch (ParserConfigurationException | SAXException | XPathExpressionException exception) {
      throw new IOException("Unable to read " + metadataXml, exception);
    }
    if (timestamp.isEmpty() || buildNumber.isEmpty()) {
      throw new IOException("No snapshot version in " + metadataXml);
    }
    // They become part of a file name in the cache directory
    if (!SNAPSHOT_TIMESTAMP.matcher(timestamp).matches() || !SNAPSHOT_BUILD_NUMBER.matcher(buildNumber).matches()) {
      throw new IOException("Invalid snapshot version in " + metadataXml + ": " + timestamp + "-" + buildNumber);
    }
    final String versionFragment = version.replace("-SNAPSHOT", "");
    return osName + "-" + archName + "-" + versionFragment + "-" + timestamp + "-" + buildNumber + ".jar";
  }

  static URL cache(URL jarUrl, Path jarFile) throws IOException {
    return cache(jarUrl, jarFile, CONNECT_TIMEOUT_MILLIS, READ_TIMEOUT_MILLIS);
  }

  /**
   * Returns the cached JAR if it's still a private file of the size recorded when it was verified, otherwise
   * downloads it, verifies it against the repository's {@code .sha1} checksum, and caches it.
   *
   * <p>The directory of {@code jarFile} must be private to the current user (see {@link CacheDirectory}).
   */
  static URL cache(URL jarUrl, Path jarFile, int connectTimeoutMillis, int readTimeoutMillis) throws IOException {
    final Path recordFile = jarFile.resolveSibling(jarFile.getFileName() + RECORD_EXTENSION);
    if (System.getProperty(FORCE_UPDATE) == null && isCached(jarFile, recordFile)) {
      return jarFile.toUri().toURL();
    }
    deleteStalePartFiles(jarFile.getParent());
    final String expectedChecksum = readChecksum(
      new URL(jarUrl + CHECKSUM_EXTENSION), connectTimeoutMillis, readTimeoutMillis);
    // Download next to the target and move it into place, so that neither this JVM on a later run nor a
    // concurrent JVM ever sees (or deletes) a partial JAR
    final Path partFile = partFile(jarFile);
    try {
      final URLConnection connection = openConnection(jarUrl, connectTimeoutMillis, readTimeoutMillis);
      final MessageDigest digest = sha1();
      final long downloaded;
      try (
        ReadableByteChannel inChannel = Channels.newChannel(new DigestInputStream(connection.getInputStream(), digest));
        FileChannel outChannel = createOwnerOnly(partFile)
      ) {
        downloaded = outChannel.transferFrom(inChannel, 0, Long.MAX_VALUE);
        // A connection dropped mid-body reads as a normal end of stream. This first check gives a clearer error for
        // responses with a Content-Length; the checksum also covers chunked and close-delimited ones
        final long expected = connection.getContentLengthLong();
        if (expected >= 0 && downloaded != expected) {
          throw new IOException("Incomplete download of " + jarUrl + ": got " + downloaded + " of " + expected + " bytes");
        }
      }
      final String actualChecksum = hex(digest.digest());
      if (!actualChecksum.equals(expectedChecksum)) {
        throw new IOException("Checksum mismatch for " + jarUrl + ": expected SHA-1 " + expectedChecksum +
          ", got " + actualChecksum);
      }
      // The record first: a JAR without (a matching) one is never used from the cache
      writeRecord(recordFile, actualChecksum, downloaded);
    } catch (IOException | RuntimeException exception) {
      deletePartFile(partFile, exception);
      throw exception;
    }
    try {
      move(partFile, jarFile);
    } catch (IOException exception) {
      deletePartFile(partFile, exception);
      // A concurrent JVM may have moved its (identical) JAR into place first and still hold it open, which
      // prevents replacing it on Windows: use that one, if it matches the checksum just fetched
      if (!CacheDirectory.isPrivateFile(jarFile) || !expectedChecksum.equals(sha1(jarFile))) {
        throw exception;
      }
    }
    return jarFile.toUri().toURL();
  }

  /**
   * Whether the JAR and its record are private files (see {@link CacheDirectory#isPrivateFile(Path)}) and the JAR
   * still has the size recorded when it was verified.
   *
   * <p>The JAR isn't hashed again: other users can't write it, and this catches an incomplete or corrupt replacement
   * by its size (a same-user process that replaces it with another file of the same size isn't detected).
   */
  private static boolean isCached(Path jarFile, Path recordFile) throws IOException {
    if (!CacheDirectory.isPrivateFile(jarFile) || !CacheDirectory.isPrivateFile(recordFile)) {
      return false;
    }
    final Properties record = new Properties();
    try (InputStream stream = Files.newInputStream(recordFile, LinkOption.NOFOLLOW_LINKS)) {
      record.load(stream);
    } catch (IllegalArgumentException exception) {
      // A corrupt record (e.g. a malformed Unicode escape): download again, which replaces it
      return false;
    }
    final String size = record.getProperty("size");
    return parseChecksum(record.getProperty("sha1", "")) != null && size != null &&
      size.equals(Long.toString(Files.size(jarFile)));
  }

  private static String readChecksum(URL checksumUrl, int connectTimeoutMillis, int readTimeoutMillis)
    throws IOException {
    final ByteArrayOutputStream content = new ByteArrayOutputStream();
    try (InputStream stream = openStream(checksumUrl, connectTimeoutMillis, readTimeoutMillis)) {
      final byte[] buffer = new byte[256];
      int read;
      // A checksum file is a few dozen bytes: don't read an unexpected large response into memory
      while ((read = stream.read(buffer)) != -1 && content.size() < 4096) {
        content.write(buffer, 0, read);
      }
    }
    final String checksum = parseChecksum(new String(content.toByteArray(), StandardCharsets.US_ASCII));
    if (checksum == null) {
      throw new IOException("Invalid SHA-1 checksum file " + checksumUrl);
    }
    return checksum;
  }

  /**
   * Parses a {@code .sha1} file: the hex digest, optionally followed by whitespace and the file name.
   *
   * @return the lower-case digest, or {@code null} if the content isn't a SHA-1 checksum.
   */
  private static String parseChecksum(String content) {
    final String[] tokens = content.trim().split("\\s+", 2);
    final String checksum = tokens[0].toLowerCase(Locale.ROOT);
    return SHA1.matcher(checksum).matches() ? checksum : null;
  }

  private static void writeRecord(Path recordFile, String checksum, long size) throws IOException {
    final Path partFile = partFile(recordFile);
    try {
      try (FileChannel channel = createOwnerOnly(partFile)) {
        channel.write(ByteBuffer.wrap(("sha1=" + checksum + "\nsize=" + size + "\n").getBytes(StandardCharsets.US_ASCII)));
      }
      move(partFile, recordFile);
    } catch (IOException exception) {
      deletePartFile(partFile, exception);
      throw exception;
    }
  }

  /**
   * Creates a new file ({@code O_EXCL}) only its owner can read or write, whatever the umask.
   */
  private static FileChannel createOwnerOnly(Path file) throws IOException {
    return FileChannel.open(file, EnumSet.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE),
      CacheDirectory.ownerOnlyFile());
  }

  private static String sha1(Path file) throws IOException {
    final MessageDigest digest = sha1();
    try (InputStream stream = new DigestInputStream(Files.newInputStream(file, LinkOption.NOFOLLOW_LINKS), digest)) {
      final byte[] buffer = new byte[64 * 1024];
      while (stream.read(buffer) != -1) {
        // Digest
      }
    }
    return hex(digest.digest());
  }

  private static MessageDigest sha1() {
    try {
      return MessageDigest.getInstance("SHA-1");
    } catch (NoSuchAlgorithmException exception) {
      // Every Java platform implementation is required to support SHA-1
      throw new IllegalStateException(exception);
    }
  }

  private static String hex(byte[] bytes) {
    final StringBuilder hex = new StringBuilder(bytes.length * 2);
    for (byte b : bytes) {
      hex.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
    }
    return hex.toString();
  }

  private static Path partFile(Path file) {
    return file.resolveSibling(file.getFileName() + "." + UUID.randomUUID() + PART_EXTENSION);
  }

  /**
   * Deletes the {@code .part} files this class creates in the (private) cache directory that haven't been modified
   * for {@link #STALE_PART_FILE_MILLIS}: a JVM killed mid-download leaves them behind, and their names are never
   * reused.
   */
  private static void deleteStalePartFiles(Path cacheDirectory) {
    final long staleBefore = System.currentTimeMillis() - STALE_PART_FILE_MILLIS;
    try (DirectoryStream<Path> files = Files.newDirectoryStream(cacheDirectory)) {
      for (Path file : files) {
        final String name = file.getFileName().toString();
        if (PART_FILE.matcher(name).matches() &&
          Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) &&
          Files.getLastModifiedTime(file, LinkOption.NOFOLLOW_LINKS).toMillis() < staleBefore) {
          Files.deleteIfExists(file);
        }
      }
    } catch (IOException exception) {
      // Best effort: a leftover file wastes space but is never used
    }
  }

  private static void deletePartFile(Path partFile, Exception exception) {
    try {
      Files.deleteIfExists(partFile);
    } catch (IOException cleanupException) {
      exception.addSuppressed(cleanupException);
    }
  }

  private static void move(Path source, Path target) throws IOException {
    try {
      Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
    } catch (AtomicMoveNotSupportedException exception) {
      Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
    }
  }

  /**
   * Same as {@link URL#openStream()}, but fails instead of blocking forever when the server doesn't
   * accept the connection or stops sending data.
   */
  static InputStream openStream(URL url, int connectTimeoutMillis, int readTimeoutMillis) throws IOException {
    return openConnection(url, connectTimeoutMillis, readTimeoutMillis).getInputStream();
  }

  private static URLConnection openConnection(URL url, int connectTimeoutMillis, int readTimeoutMillis)
    throws IOException {
    final URLConnection connection = url.openConnection();
    connection.setConnectTimeout(connectTimeoutMillis);
    connection.setReadTimeout(readTimeoutMillis);
    return connection;
  }

  /**
   * Fails on any parse error without printing it (the default handler prints errors to stderr).
   */
  private static final class ThrowingErrorHandler implements ErrorHandler {
    @Override
    public void warning(SAXParseException exception) {
      // Not an error
    }

    @Override
    public void error(SAXParseException exception) throws SAXException {
      throw exception;
    }

    @Override
    public void fatalError(SAXParseException exception) throws SAXException {
      throw exception;
    }
  }
}
