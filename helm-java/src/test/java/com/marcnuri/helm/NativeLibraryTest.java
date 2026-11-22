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

package com.marcnuri.helm;

import com.marcnuri.helm.jni.NativeLibrary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Marc Nuri
 */
class NativeLibraryTest {

  private static final String CACHE_DIR = "com.marcnuri.jkube-helm.cacheDir";

  @Test
  void getInstanceFromClassPath() {
    System.setProperty("com.marcnuri.jkube-helm.skipRemoteJar", "true");
    try {
      NativeLibrary nativeLibrary = NativeLibrary.getInstance();
      assertThat(nativeLibrary).isNotNull();
    } finally {
      System.clearProperty("com.marcnuri.jkube-helm.skipRemoteJar");
    }
  }

  @Test
  void getSnapshotInstanceFromRemoteJar() throws IOException {
    final ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
    // An empty cache directory of its own: always downloads, and leaves nothing in the user's cache. Not a @TempDir:
    // the downloaded JAR stays open (its class loader is never closed), so on Windows it can't be deleted, and a
    // @TempDir that can't be deleted fails the test
    final Path cacheDir = Files.createTempDirectory("helm-java-test-cache");
    final String previousCacheDir = System.setProperty(CACHE_DIR, cacheDir.toString());
    // Use a PUBLISHED snapshot version
    System.setProperty("com.marcnuri.jkube-helm.version", "0.0-SNAPSHOT");
    try {
      Thread.currentThread().setContextClassLoader(new URLClassLoader(new java.net.URL[0], null));
      assertThat(NativeLibrary.serviceProviderLibrary(null)).isNull();
      NativeLibrary nativeLibrary = NativeLibrary.getInstance();
      assertThat(nativeLibrary).isNotNull();
    } finally {
      Thread.currentThread().setContextClassLoader(currentClassLoader);
      System.clearProperty("com.marcnuri.jkube-helm.version");
      restoreCacheDir(previousCacheDir);
      deleteQuietly(cacheDir);
    }
  }

  @Test
  void getReleaseInstanceFromRemoteJar() throws IOException {
    final ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
    // An empty cache directory of its own: always downloads, and leaves nothing in the user's cache. Not a @TempDir:
    // the downloaded JAR stays open (its class loader is never closed), so on Windows it can't be deleted, and a
    // @TempDir that can't be deleted fails the test
    final Path cacheDir = Files.createTempDirectory("helm-java-test-cache");
    final String previousCacheDir = System.setProperty(CACHE_DIR, cacheDir.toString());
    // Use a PUBLISHED release version
    System.setProperty("com.marcnuri.jkube-helm.version", "0.0.1");
    try {
      Thread.currentThread().setContextClassLoader(new URLClassLoader(new java.net.URL[0], null));
      assertThat(NativeLibrary.serviceProviderLibrary(null)).isNull();
      NativeLibrary nativeLibrary = NativeLibrary.getInstance();
      assertThat(nativeLibrary).isNotNull();
    } finally {
      Thread.currentThread().setContextClassLoader(currentClassLoader);
      System.clearProperty("com.marcnuri.jkube-helm.version");
      restoreCacheDir(previousCacheDir);
      deleteQuietly(cacheDir);
    }
  }

  @Test
  void getInstanceNotAvailableReportsRemoteJarFailureAsCause(@TempDir Path tempDir) {
    final ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
    final String previousCacheDir = System.setProperty(CACHE_DIR, tempDir.toString());
    // An UNPUBLISHED version: a 404 from Maven Central, or a connection error when offline
    System.setProperty("com.marcnuri.jkube-helm.version", "0.0.0-unpublished");
    try {
      Thread.currentThread().setContextClassLoader(new URLClassLoader(new URL[0], null));
      assertThatThrownBy(NativeLibrary::getInstance)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("No NativeLibrary implementation found, please add one of the supported dependencies to your project")
        .cause().isInstanceOf(IOException.class);
    } finally {
      Thread.currentThread().setContextClassLoader(currentClassLoader);
      System.clearProperty("com.marcnuri.jkube-helm.version");
      restoreCacheDir(previousCacheDir);
    }
  }

  @Test
  void getInstanceNotAvailable() {
    final ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
    System.setProperty("com.marcnuri.jkube-helm.skipRemoteJar", "true");
    try {
      Thread.currentThread().setContextClassLoader(new URLClassLoader(new java.net.URL[0], null));
      assertThatThrownBy(NativeLibrary::getInstance)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("No NativeLibrary implementation found, please add one of the supported dependencies to your project")
        .hasNoCause();
    } finally {
      System.clearProperty("com.marcnuri.jkube-helm.skipRemoteJar");
      Thread.currentThread().setContextClassLoader(currentClassLoader);
    }
  }

  private static void restoreCacheDir(String previousCacheDir) {
    if (previousCacheDir == null) {
      System.clearProperty(CACHE_DIR);
    } else {
      System.setProperty(CACHE_DIR, previousCacheDir);
    }
  }

  /**
   * Deletes what can be deleted (on Windows, not the JAR a class loader still holds open).
   */
  private static void deleteQuietly(Path directory) {
    try (Stream<Path> files = Files.walk(directory)) {
      files.sorted(Comparator.reverseOrder()).forEach(file -> file.toFile().delete());
    } catch (IOException exception) {
      // Best effort
    }
  }
}
