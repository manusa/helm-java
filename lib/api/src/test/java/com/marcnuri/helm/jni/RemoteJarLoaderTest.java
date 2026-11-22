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

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.xml.sax.SAXException;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.marcnuri.helm.jni.TestFiles.assumePosix;
import static com.marcnuri.helm.jni.TestFiles.permissions;
import static com.marcnuri.helm.jni.TestFiles.setPermissions;
import static com.marcnuri.helm.jni.TestFiles.writePrivate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Marc Nuri
 */
// SEPARATE_THREAD: the timeout fails the test even if a socket read ignores the interrupt
@Timeout(value = 30, unit = TimeUnit.SECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class RemoteJarLoaderTest {

  private static final String FORCE_UPDATE = "com.marcnuri.jkube-helm.forceUpdate";

  @TempDir
  private Path tempDir;

  private byte[] jar;
  private String jarSha1;
  private Path jarFile;
  private Path recordFile;

  private CountDownLatch release;
  private ExecutorService executor;
  private HttpServer server;
  private final Map<String, Responder> responses = new ConcurrentHashMap<>();

  @BeforeEach
  void setUp() throws IOException {
    jar = jar();
    jarSha1 = sha1(jar);
    jarFile = tempDir.resolve("the.jar");
    recordFile = tempDir.resolve("the.jar.verified");
    release = new CountDownLatch(1);
    executor = Executors.newCachedThreadPool();
    server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
    server.setExecutor(executor);
    // Every path not registered in responses gets a 404
    server.createContext("/", exchange -> {
      final Responder responder = responses.get(exchange.getRequestURI().getPath());
      if (responder == null) {
        exchange.sendResponseHeaders(404, -1);
        exchange.close();
      } else {
        responder.respond(exchange);
      }
    });
    server.start();
    responses.put("/responds", body("the-content".getBytes(StandardCharsets.UTF_8)));
    servesJar("/jar");
    final Responder neverResponds = exchange -> {
      try {
        release.await();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } finally {
        exchange.close();
      }
    };
    responses.put("/never-responds", neverResponds);
    responses.put("/never-responds.sha1", neverResponds);
  }

  @AfterEach
  void tearDown() {
    release.countDown();
    server.stop(0);
    executor.shutdownNow();
  }

  @Nested
  class OpenStream {

    @Test
    void readsResponse() throws IOException {
      try (InputStream stream = RemoteJarLoader.openStream(url("/responds"), 5_000, 5_000)) {
        assertThat(read(stream)).isEqualTo("the-content");
      }
    }

    @Test
    void failsWhenServerStopsResponding() throws IOException {
      final URL url = url("/never-responds");
      final long start = System.nanoTime();
      assertThatThrownBy(() -> RemoteJarLoader.openStream(url, 5_000, 200))
        .isInstanceOf(SocketTimeoutException.class);
      assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)).isLessThan(10_000L);
    }
  }

  @Nested
  class Download {

    @Test
    void cachesJarWithMatchingChecksum() throws IOException {
      assertThat(RemoteJarLoader.cache(url("/jar"), jarFile)).isEqualTo(jarFile.toUri().toURL());
      assertThat(jarFile).hasBinaryContent(jar);
      assertThat(recordFile).hasContent("sha1=" + jarSha1 + "\nsize=" + jar.length);
      assertThat(tempDir).isDirectoryNotContaining("glob:**.part");
    }

    @Test
    void cachedFilesAreOwnerOnly() throws IOException {
      assumePosix();
      RemoteJarLoader.cache(url("/jar"), jarFile);
      // Whatever the umask (e.g. 002 on hosts where users share a group)
      assertThat(permissions(jarFile)).isEqualTo("rw-------");
      assertThat(permissions(recordFile)).isEqualTo("rw-------");
    }

    @Test
    void acceptsChecksumFileWithFileName() throws IOException {
      responses.put("/named.jar", body(jar));
      responses.put("/named.jar.sha1", body((jarSha1.toUpperCase() + "  named.jar\n").getBytes(StandardCharsets.UTF_8)));
      RemoteJarLoader.cache(url("/named.jar"), jarFile);
      assertThat(jarFile).hasBinaryContent(jar);
    }

    @Test
    void checksumMismatchLeavesNoFile() throws IOException {
      responses.put("/tampered.jar", body("not-the-jar".getBytes(StandardCharsets.UTF_8)));
      responses.put("/tampered.jar.sha1", body(jarSha1.getBytes(StandardCharsets.UTF_8)));
      final URL url = url("/tampered.jar");
      assertThatThrownBy(() -> RemoteJarLoader.cache(url, jarFile))
        .isInstanceOf(IOException.class)
        .hasMessageStartingWith("Checksum mismatch for " + url + ": expected SHA-1 " + jarSha1);
      assertThat(tempDir).isEmptyDirectory();
    }

    @Test
    void missingChecksumLeavesNoFile() throws IOException {
      responses.put("/unchecked.jar", body(jar));
      final URL url = url("/unchecked.jar");
      assertThatThrownBy(() -> RemoteJarLoader.cache(url, jarFile))
        .isInstanceOf(FileNotFoundException.class)
        .hasMessageEndingWith("/unchecked.jar.sha1");
      assertThat(tempDir).isEmptyDirectory();
    }

    @Test
    void invalidChecksumLeavesNoFile() throws IOException {
      responses.put("/invalid.jar", body(jar));
      responses.put("/invalid.jar.sha1", body("<html>Not found</html>".getBytes(StandardCharsets.UTF_8)));
      final URL url = url("/invalid.jar");
      assertThatThrownBy(() -> RemoteJarLoader.cache(url, jarFile))
        .isInstanceOf(IOException.class)
        .hasMessage("Invalid SHA-1 checksum file " + url + ".sha1");
      assertThat(tempDir).isEmptyDirectory();
    }

    @Test
    void truncatedDownloadLeavesNoFile() throws IOException {
      responses.put("/truncated.jar", exchange -> {
        // Declares more bytes than it sends, then drops the connection
        exchange.sendResponseHeaders(200, jar.length);
        exchange.getResponseBody().write(jar, 0, jar.length / 2);
        exchange.getResponseBody().flush();
        exchange.close();
      });
      responses.put("/truncated.jar.sha1", body(jarSha1.getBytes(StandardCharsets.UTF_8)));
      final URL url = url("/truncated.jar");
      assertThatThrownBy(() -> RemoteJarLoader.cache(url, jarFile))
        .isInstanceOf(IOException.class)
        .hasMessageStartingWith("Incomplete download of " + url);
      assertThat(tempDir).isEmptyDirectory();
    }

    @Test
    void truncatedChunkedDownloadLeavesNoFile() throws IOException {
      responses.put("/chunked.jar", exchange -> {
        // A chunked response (no Content-Length) with only part of the JAR: what the client gets when such a
        // response is cut off, since it reads a dropped chunked or close-delimited body as a normal end of stream
        exchange.sendResponseHeaders(200, 0);
        try (OutputStream os = exchange.getResponseBody()) {
          os.write(jar, 0, jar.length / 2);
        }
      });
      responses.put("/chunked.jar.sha1", body(jarSha1.getBytes(StandardCharsets.UTF_8)));
      final URL url = url("/chunked.jar");
      assertThatThrownBy(() -> RemoteJarLoader.cache(url, jarFile))
        .isInstanceOf(IOException.class)
        .hasMessageStartingWith("Checksum mismatch for " + url);
      assertThat(tempDir).isEmptyDirectory();
    }

    @Test
    void unresponsiveServerTimesOutAndLeavesNoFile() throws IOException {
      final URL url = url("/never-responds");
      assertThatThrownBy(() -> RemoteJarLoader.cache(url, jarFile, 5_000, 200))
        .isInstanceOf(SocketTimeoutException.class);
      assertThat(tempDir).isEmptyDirectory();
    }

    @Test
    void failsWhenJarCantBeMovedIntoPlace() throws IOException {
      // The fallback for a JAR another JVM holds open on Windows only applies to a private file matching the checksum
      Files.write(Files.createDirectory(jarFile).resolve("not-empty"), new byte[]{1});
      final URL url = url("/jar");
      assertThatThrownBy(() -> RemoteJarLoader.cache(url, jarFile))
        .isInstanceOf(IOException.class);
      assertThat(tempDir).isDirectoryNotContaining("glob:**.part");
    }

    @Test
    void deletesStalePartFiles() throws IOException {
      final long staleTime = System.currentTimeMillis() - RemoteJarLoader.STALE_PART_FILE_MILLIS - 60_000;
      final Path staleJar = Files.write(tempDir.resolve("other.jar." + UUID.randomUUID() + ".part"), new byte[]{1});
      final Path staleRecord = Files.write(tempDir.resolve("other.jar.verified." + UUID.randomUUID() + ".part"), new byte[]{1});
      // Not created by helm-java (e.g. with a cacheDir shared with other tools)
      final Path foreign = Files.write(tempDir.resolve("download.part"), new byte[]{1});
      for (Path file : new Path[]{staleJar, staleRecord, foreign}) {
        Files.setLastModifiedTime(file, FileTime.fromMillis(staleTime));
      }
      // A download in progress (another JVM) keeps its file recently modified
      final Path inProgress = Files.write(tempDir.resolve("the.jar." + UUID.randomUUID() + ".part"), new byte[]{1});
      RemoteJarLoader.cache(url("/jar"), jarFile);
      assertThat(staleJar).doesNotExist();
      assertThat(staleRecord).doesNotExist();
      assertThat(foreign).exists();
      assertThat(inProgress).exists();
    }
  }

  @Nested
  class CacheHit {

    @Test
    void usesRecordedJarWithoutContactingServer() throws IOException {
      writePrivate(jarFile, jar);
      writeRecord(jarSha1, jar.length);
      final long start = System.nanoTime();
      assertThat(RemoteJarLoader.cache(url("/never-responds"), jarFile, 5_000, 5_000))
        .isEqualTo(jarFile.toUri().toURL());
      assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)).isLessThan(2_000L);
      assertThat(jarFile).hasBinaryContent(jar);
    }

    @Test
    void usesSameSizeReplacementWithoutHashingIt() throws IOException {
      // Not hashed on every use: a same-user process can replace the JAR with another of the same size (out of
      // scope: it can already run any code as that user). Other users can't write it.
      final byte[] replacement = new byte[jar.length];
      writePrivate(jarFile, replacement);
      writeRecord(jarSha1, jar.length);
      RemoteJarLoader.cache(url("/never-responds"), jarFile, 5_000, 200);
      assertThat(jarFile).hasBinaryContent(replacement);
    }

    @Test
    void replacesJarOfAnotherSize() throws IOException {
      // e.g. incomplete or corrupt
      writePrivate(jarFile, "corrupt".getBytes(StandardCharsets.UTF_8));
      writeRecord(jarSha1, jar.length);
      RemoteJarLoader.cache(url("/jar"), jarFile);
      assertThat(jarFile).hasBinaryContent(jar);
    }

    @Test
    void doesNotUseJarOfAnotherSizeWhenServerIsUnreachable() throws IOException {
      writePrivate(jarFile, "corrupt".getBytes(StandardCharsets.UTF_8));
      writeRecord(jarSha1, jar.length);
      final URL url = url("/never-responds");
      assertThatThrownBy(() -> RemoteJarLoader.cache(url, jarFile, 5_000, 200))
        .isInstanceOf(SocketTimeoutException.class);
    }

    @Test
    void replacesJarWithoutRecord() throws IOException {
      // e.g. a JAR not downloaded (and verified) by this version
      writePrivate(jarFile, "planted".getBytes(StandardCharsets.UTF_8));
      RemoteJarLoader.cache(url("/jar"), jarFile);
      assertThat(jarFile).hasBinaryContent(jar);
      assertThat(recordFile).hasContent("sha1=" + jarSha1 + "\nsize=" + jar.length);
    }

    @Test
    void replacesJarWithInvalidRecord() throws IOException {
      writePrivate(jarFile, jar);
      writePrivate(recordFile, ("size=" + jar.length).getBytes(StandardCharsets.UTF_8));
      RemoteJarLoader.cache(url("/jar"), jarFile);
      assertThat(recordFile).hasContent("sha1=" + jarSha1 + "\nsize=" + jar.length);
    }

    @Test
    void replacesJarWithMalformedRecord() throws IOException {
      writePrivate(jarFile, jar);
      // Properties.load rejects a malformed Unicode escape with an IllegalArgumentException
      writePrivate(recordFile, ("sha1=\\u12zz\nsize=" + jar.length).getBytes(StandardCharsets.ISO_8859_1));
      RemoteJarLoader.cache(url("/jar"), jarFile);
      assertThat(recordFile).hasContent("sha1=" + jarSha1 + "\nsize=" + jar.length);
    }

    @Test
    void replacesJarWritableByOthers() throws IOException {
      assumePosix();
      writePrivate(jarFile, jar);
      writeRecord(jarSha1, jar.length);
      setPermissions(jarFile, "rw-rw-r--");
      RemoteJarLoader.cache(url("/jar"), jarFile);
      assertThat(permissions(jarFile)).isEqualTo("rw-------");
    }

    @Test
    void replacesJarWithRecordWritableByOthers() throws IOException {
      assumePosix();
      writePrivate(jarFile, jar);
      writeRecord(jarSha1, jar.length);
      setPermissions(recordFile, "rw-rw-rw-");
      RemoteJarLoader.cache(url("/jar"), jarFile);
      assertThat(permissions(recordFile)).isEqualTo("rw-------");
    }

    @Test
    void replacesSymbolicLinkedJar() throws IOException {
      final Path elsewhere = writePrivate(tempDir.resolve("elsewhere.jar"), jar);
      writeRecord(jarSha1, jar.length);
      TestFiles.createSymbolicLink(jarFile, elsewhere);
      RemoteJarLoader.cache(url("/jar"), jarFile);
      assertThat(Files.isSymbolicLink(jarFile)).isFalse();
      assertThat(jarFile).hasBinaryContent(jar);
    }
  }

  @Nested
  class ForceUpdate {

    private String previousForceUpdate;

    @BeforeEach
    void setForceUpdate() {
      previousForceUpdate = System.setProperty(FORCE_UPDATE, "true");
    }

    @AfterEach
    void restoreForceUpdate() {
      if (previousForceUpdate == null) {
        System.clearProperty(FORCE_UPDATE);
      } else {
        System.setProperty(FORCE_UPDATE, previousForceUpdate);
      }
    }

    @Test
    void replacesRecordedJar() throws IOException {
      final byte[] cached = "cached".getBytes(StandardCharsets.UTF_8);
      writePrivate(jarFile, cached);
      writeRecord(sha1(cached), cached.length);
      RemoteJarLoader.cache(url("/jar"), jarFile);
      assertThat(jarFile).hasBinaryContent(jar);
      assertThat(recordFile).hasContent("sha1=" + jarSha1 + "\nsize=" + jar.length);
    }

    @Test
    void keepsCachedJarWhenDownloadFails() throws IOException {
      writePrivate(jarFile, "cached".getBytes(StandardCharsets.UTF_8));
      responses.put("/broken.jar", body("not-the-jar".getBytes(StandardCharsets.UTF_8)));
      responses.put("/broken.jar.sha1", body(jarSha1.getBytes(StandardCharsets.UTF_8)));
      final URL url = url("/broken.jar");
      assertThatThrownBy(() -> RemoteJarLoader.cache(url, jarFile))
        .isInstanceOf(IOException.class);
      assertThat(jarFile).hasContent("cached");
      assertThat(tempDir).isDirectoryNotContaining("glob:**.part");
    }
  }

  @Nested
  class RemoteJar {

    @BeforeEach
    void setUp() throws IOException {
      servesJar("/repo/com/example/linux-amd64/1.0.0/linux-amd64-1.0.0.jar");
      responses.put("/repo/com/example/linux-amd64/1.1-SNAPSHOT/maven-metadata.xml", body(("<metadata><versioning>" +
        "<snapshot><timestamp>20261121.103929</timestamp><buildNumber>401</buildNumber></snapshot>" +
        "</versioning></metadata>").getBytes(StandardCharsets.UTF_8)));
      servesJar("/repo/com/example/linux-amd64/1.1-SNAPSHOT/linux-amd64-1.1-20261121.103929-401.jar");
      responses.put("/repo/com/example/linux-amd64/1.2-SNAPSHOT/maven-metadata.xml",
        body("not xml".getBytes(StandardCharsets.UTF_8)));
      responses.put("/repo/com/example/linux-amd64/1.3-SNAPSHOT/maven-metadata.xml", body(
        "<metadata><versioning><lastUpdated>20261121103929</lastUpdated></versioning></metadata>"
          .getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void downloadsReleaseJarIntoCacheDirectory() throws IOException {
      try (URLClassLoader classLoader = (URLClassLoader) RemoteJarLoader.remoteJar(
        url("/repo").toString(), "com.example", "1.0.0", "linux", "amd64", tempDir)) {
        final Path downloaded = tempDir.resolve("linux-amd64-1.0.0.jar");
        assertThat(classLoader.getURLs()).containsExactly(downloaded.toUri().toURL());
        assertThat(downloaded).hasBinaryContent(jar);
      }
    }

    @Test
    void downloadsLatestSnapshotJarFromMetadata() throws IOException {
      try (URLClassLoader classLoader = (URLClassLoader) RemoteJarLoader.remoteJar(
        url("/repo").toString(), "com.example", "1.1-SNAPSHOT", "linux", "amd64", tempDir)) {
        final Path downloaded = tempDir.resolve("linux-amd64-1.1-20261121.103929-401.jar");
        assertThat(classLoader.getURLs()).containsExactly(downloaded.toUri().toURL());
        assertThat(downloaded).hasBinaryContent(jar);
      }
    }

    @Test
    void missingReleaseJarThrowsWithUrl() {
      assertThatThrownBy(() -> RemoteJarLoader.remoteJar(
        url("/repo").toString(), "com.example", "2.0.0", "linux", "amd64", tempDir))
        .isInstanceOf(FileNotFoundException.class)
        .hasMessageEndingWith("/repo/com/example/linux-amd64/2.0.0/linux-amd64-2.0.0.jar.sha1");
      assertThat(tempDir).isEmptyDirectory();
    }

    @Test
    void missingSnapshotMetadataThrowsWithUrl() {
      assertThatThrownBy(() -> RemoteJarLoader.remoteJar(
        url("/repo").toString(), "com.example", "2.0-SNAPSHOT", "linux", "amd64", tempDir))
        .isInstanceOf(FileNotFoundException.class)
        .hasMessageEndingWith("/repo/com/example/linux-amd64/2.0-SNAPSHOT/maven-metadata.xml");
    }

    @Test
    void invalidSnapshotMetadataThrowsWithUrlWithoutPrintingIt() throws IOException {
      final PrintStream previousErr = System.err;
      final ByteArrayOutputStream err = new ByteArrayOutputStream();
      System.setErr(new PrintStream(err));
      try {
        assertThatThrownBy(() -> RemoteJarLoader.remoteJar(
          url("/repo").toString(), "com.example", "1.2-SNAPSHOT", "linux", "amd64", tempDir))
          .isInstanceOf(IOException.class)
          .hasMessageEndingWith("/repo/com/example/linux-amd64/1.2-SNAPSHOT/maven-metadata.xml")
          .hasCauseInstanceOf(SAXException.class);
      } finally {
        System.setErr(previousErr);
      }
      assertThat(err.toString()).isEmpty();
    }

    @Test
    void snapshotMetadataWithoutSnapshotVersionThrowsWithUrl() throws IOException {
      assertThatThrownBy(() -> RemoteJarLoader.remoteJar(
        url("/repo").toString(), "com.example", "1.3-SNAPSHOT", "linux", "amd64", tempDir))
        .isInstanceOf(IOException.class)
        .hasMessage("No snapshot version in " + url("/repo") + "/com/example/linux-amd64/1.3-SNAPSHOT/maven-metadata.xml");
    }
  }

  @Nested
  class SnapshotVersion {

    @BeforeEach
    void setUp() {
      responses.put("/repo/com/example/linux-amd64/1.4-SNAPSHOT/maven-metadata.xml", body(("<metadata><versioning>" +
        "<snapshot><timestamp>../../../escape</timestamp><buildNumber>1</buildNumber></snapshot>" +
        "</versioning></metadata>").getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void invalidSnapshotVersionThrowsWithUrl() throws IOException {
      // It becomes part of a file name in the cache directory
      assertThatThrownBy(() -> RemoteJarLoader.remoteJar(
        url("/repo").toString(), "com.example", "1.4-SNAPSHOT", "linux", "amd64", tempDir))
        .isInstanceOf(IOException.class)
        .hasMessage("Invalid snapshot version in " + url("/repo") +
          "/com/example/linux-amd64/1.4-SNAPSHOT/maven-metadata.xml: ../../../escape-1");
      assertThat(tempDir).isEmptyDirectory();
    }
  }

  @Nested
  class Timeouts {

    // The downloads use these; with 0 (no timeout), an unresponsive repository blocks the lookup forever
    @Test
    void connectTimeoutIsBounded() {
      assertThat(RemoteJarLoader.CONNECT_TIMEOUT_MILLIS).isPositive();
    }

    @Test
    void readTimeoutIsBounded() {
      assertThat(RemoteJarLoader.READ_TIMEOUT_MILLIS).isPositive();
    }
  }

  private void writeRecord(String sha1, long size) throws IOException {
    writePrivate(recordFile, ("sha1=" + sha1 + "\nsize=" + size + "\n").getBytes(StandardCharsets.UTF_8));
  }

  private void servesJar(String path) {
    responses.put(path, body(jar));
    responses.put(path + ".sha1", body(jarSha1.getBytes(StandardCharsets.UTF_8)));
  }

  private static Responder body(byte[] body) {
    return exchange -> {
      exchange.sendResponseHeaders(200, body.length);
      try (OutputStream os = exchange.getResponseBody()) {
        os.write(body);
      }
    };
  }

  private static byte[] jar() throws IOException {
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (ZipOutputStream zip = new ZipOutputStream(out)) {
      zip.putNextEntry(new ZipEntry("META-INF/services/com.marcnuri.helm.jni.NativeLibrary"));
      zip.write("com.example.NativeLibrary\n".getBytes(StandardCharsets.UTF_8));
      zip.closeEntry();
    }
    return out.toByteArray();
  }

  private static String sha1(byte[] bytes) {
    try {
      final StringBuilder hex = new StringBuilder();
      for (byte b : MessageDigest.getInstance("SHA-1").digest(bytes)) {
        hex.append(String.format("%02x", b));
      }
      return hex.toString();
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException(exception);
    }
  }

  private URL url(String path) throws IOException {
    return new URL("http", server.getAddress().getHostString(), server.getAddress().getPort(), path);
  }

  private static String read(InputStream stream) throws IOException {
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    final byte[] buffer = new byte[1024];
    int read;
    while ((read = stream.read(buffer)) != -1) {
      out.write(buffer, 0, read);
    }
    return new String(out.toByteArray(), StandardCharsets.UTF_8);
  }

  @FunctionalInterface
  private interface Responder {
    void respond(HttpExchange exchange) throws IOException;
  }
}
