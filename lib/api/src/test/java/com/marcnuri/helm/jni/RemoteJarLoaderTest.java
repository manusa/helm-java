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

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Marc Nuri
 */
// SEPARATE_THREAD: the timeout fails the test even if a socket read ignores the interrupt
@Timeout(value = 30, unit = TimeUnit.SECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class RemoteJarLoaderTest {

  @TempDir
  private Path tempDir;

  private CountDownLatch release;
  private ExecutorService executor;
  private HttpServer server;

  @BeforeEach
  void setUp() throws IOException {
    release = new CountDownLatch(1);
    executor = Executors.newCachedThreadPool();
    server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
    server.setExecutor(executor);
    server.createContext("/responds", exchange -> {
      final byte[] body = "the-content".getBytes(StandardCharsets.UTF_8);
      exchange.sendResponseHeaders(200, body.length);
      try (OutputStream os = exchange.getResponseBody()) {
        os.write(body);
      }
    });
    server.createContext("/truncated", exchange -> {
      // Declares more bytes than it sends, then drops the connection
      exchange.sendResponseHeaders(200, 1024);
      exchange.getResponseBody().write("partial".getBytes(StandardCharsets.UTF_8));
      exchange.getResponseBody().flush();
      exchange.close();
    });
    server.createContext("/never-responds", exchange -> {
      try {
        release.await();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } finally {
        exchange.close();
      }
    });
    server.start();
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
  class Cache {

    @Test
    void downloadsToTarget() throws IOException {
      final Path jarFile = tempDir.resolve("the.jar");
      assertThat(RemoteJarLoader.cache(url("/responds"), jarFile)).isEqualTo(jarFile.toUri().toURL());
      assertThat(jarFile).hasContent("the-content");
      assertThat(tempDir).isDirectoryNotContaining("glob:**.part");
    }

    @Test
    void truncatedDownloadLeavesNoFile() throws IOException {
      final Path jarFile = tempDir.resolve("the.jar");
      final URL url = url("/truncated");
      assertThatThrownBy(() -> RemoteJarLoader.cache(url, jarFile))
        .isInstanceOf(IOException.class);
      assertThat(tempDir).isEmptyDirectory();
    }

    @Test
    void unresponsiveServerTimesOutAndLeavesNoFile() throws IOException {
      final Path jarFile = tempDir.resolve("the.jar");
      final URL url = url("/never-responds");
      assertThatThrownBy(() -> RemoteJarLoader.cache(url, jarFile, 5_000, 200))
        .isInstanceOf(SocketTimeoutException.class);
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
}
