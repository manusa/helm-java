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
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import java.util.UUID;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * @author Marc Nuri
 */
class RemoteJarLoader {

  private static final String TEMP_DIR = "java.io.tmpdir";
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

  private RemoteJarLoader() {
  }

  static ClassLoader remoteJar() {
    // For testing
    if (System.getProperty(SKIP) != null) {
      return null;
    }
    final Properties nativeLibraries = new Properties();
    try (final InputStream stream = NativeLibrary.class.getResourceAsStream("/META-INF/native-libraries.properties")) {
      nativeLibraries.load(stream);
      final String version;
      // Version can be overridden (for testing) using a system property
      if (System.getProperty(VERSION) != null) {
        version = System.getProperty(VERSION);
      } else {
        version = nativeLibraries.getProperty(VERSION);
      }
      final String groupId = nativeLibraries.getProperty(GROUP_ID);
      final boolean isSnapshot = version.endsWith("-SNAPSHOT");
      final String repository = isSnapshot ?
        nativeLibraries.getProperty(SNAPSHOTS) :
        nativeLibraries.getProperty(RELEASES);
      final String osName = Platform.osName();
      final String archName = Platform.archName();
      if (osName != null && archName != null) {
        final String groupUrl = repository + "/" + groupId.replace('.', '/') + "/" + osName + "-" + archName + "/" + version;
        final String jarName;
        if (isSnapshot) {
          jarName = latestSnapshot(version, osName, archName, groupUrl);
        } else {
          jarName = osName + "-" + archName + "-" + version + ".jar";
        }
        final URL jarUrl = new URL(groupUrl + "/" + jarName);
        final Path jarFile = Paths.get(System.getProperty(TEMP_DIR), jarName);
        return new URLClassLoader(new URL[]{cache(jarUrl, jarFile)}, NativeLibrary.class.getClassLoader());
      }
    } catch (IOException | ParserConfigurationException | SAXException | XPathExpressionException exception) {
      // NO OP
    }
    return null;
  }

  private static String latestSnapshot(String version, String osName, String archName, String groupUrl)
    throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {
    final String metadataXml = groupUrl + "/maven-metadata.xml";
    final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
    factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
    final Document xml;
    try (InputStream stream = openStream(new URL(metadataXml), CONNECT_TIMEOUT_MILLIS, READ_TIMEOUT_MILLIS)) {
      xml = factory.newDocumentBuilder().parse(stream, metadataXml);
    }
    final XPath xPath = XPathFactory.newInstance().newXPath();
    final String versionFragment = version.replace("-SNAPSHOT", "");
    final String timestamp = xPath.evaluate("//metadata/versioning/snapshot/timestamp", xml);
    final String buildNumber = xPath.evaluate("//metadata/versioning/snapshot/buildNumber", xml);
    return osName + "-" + archName + "-" + versionFragment + "-" + timestamp + "-" + buildNumber + ".jar";
  }

  static URL cache(URL jarUrl, Path jarFile) throws IOException {
    return cache(jarUrl, jarFile, CONNECT_TIMEOUT_MILLIS, READ_TIMEOUT_MILLIS);
  }

  static URL cache(URL jarUrl, Path jarFile, int connectTimeoutMillis, int readTimeoutMillis) throws IOException {
    if (System.getProperty(FORCE_UPDATE) != null || !jarFile.toFile().exists() || !jarFile.toFile().isFile()) {
      // Download next to the target and move it into place, so that neither this JVM on a later run nor a
      // concurrent JVM ever sees (or deletes) a partial JAR
      final Path partFile = jarFile.resolveSibling(jarFile.getFileName() + "." + UUID.randomUUID() + ".part");
      final long downloaded;
      try {
        final URLConnection connection = openConnection(jarUrl, connectTimeoutMillis, readTimeoutMillis);
        try (
          ReadableByteChannel inChannel = Channels.newChannel(connection.getInputStream());
          FileOutputStream fos = new FileOutputStream(partFile.toFile());
          FileChannel outChannel = fos.getChannel()
        ) {
          downloaded = outChannel.transferFrom(inChannel, 0, Long.MAX_VALUE);
          // A connection dropped mid-body reads as a normal end of stream. This cheap first check only covers
          // responses with a Content-Length (not chunked or close-delimited ones): the ZIP check below covers all
          final long expected = connection.getContentLengthLong();
          if (expected >= 0 && downloaded != expected) {
            throw new IOException("Incomplete download of " + jarUrl + ": got " + downloaded + " of " + expected + " bytes");
          }
        }
        // The ZIP central directory is at the end of the file, so a truncated download fails to open
        try (ZipFile ignored = new ZipFile(partFile.toFile())) {
          // Valid
        } catch (ZipException exception) {
          throw new IOException("Invalid download of " + jarUrl + ": truncated or not a valid ZIP", exception);
        }
      } catch (IOException | RuntimeException exception) {
        deletePartFile(partFile, exception);
        throw exception;
      }
      try {
        move(partFile, jarFile);
      } catch (IOException exception) {
        deletePartFile(partFile, exception);
        // A concurrent JVM may have moved its (complete) JAR into place first and still hold it open, which
        // prevents replacing it on Windows: use that one, if it is the same size as the one just downloaded
        // (a partial JAR left by a version without the .part download is not)
        if (!jarFile.toFile().isFile() || jarFile.toFile().length() != downloaded) {
          throw exception;
        }
      }
    }
    return jarFile.toUri().toURL();
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
}
