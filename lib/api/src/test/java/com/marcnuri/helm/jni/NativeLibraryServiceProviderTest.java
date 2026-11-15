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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * @author Marc Nuri
 */
class NativeLibraryServiceProviderTest {

  private static final String NATIVE_LIBRARY = "com.marcnuri.jkube-helm.nativeLibrary";
  private static final String TEMP_DIR = "com.marcnuri.jkube-helm.tempDir";

  @TempDir
  private Path tempDir;

  private String previousNativeLibrary;
  private String previousTempDir;

  @BeforeEach
  void saveSystemProperties() {
    previousNativeLibrary = System.clearProperty(NATIVE_LIBRARY);
    previousTempDir = System.clearProperty(TEMP_DIR);
  }

  @AfterEach
  void restoreSystemProperties() {
    restore(NATIVE_LIBRARY, previousNativeLibrary);
    restore(TEMP_DIR, previousTempDir);
  }

  @Nested
  class ServiceLoaderOrder {

    private URLClassLoader classLoader;

    @BeforeEach
    void setUp() throws IOException {
      // The provider for another platform is registered (and iterated by the ServiceLoader) first
      classLoader = servicesClassLoader(OtherPlatformNativeLibrary.class, CurrentPlatformNativeLibrary.class);
    }

    @AfterEach
    void tearDown() throws IOException {
      classLoader.close();
    }

    @Test
    void selectsProviderForCurrentPlatform() {
      assertThat(NativeLibrary.serviceProviderLibrary(classLoader))
        .isInstanceOf(CurrentPlatformNativeLibrary.class);
    }

    @Test
    void selectsProviderSetWithSystemProperty() {
      System.setProperty(NATIVE_LIBRARY, OtherPlatformNativeLibrary.class.getName());
      assertThat(NativeLibrary.serviceProviderLibrary(classLoader))
        .isInstanceOf(OtherPlatformNativeLibrary.class);
    }

    @Test
    void ignoresBlankSystemProperty() {
      System.setProperty(NATIVE_LIBRARY, "");
      assertThat(NativeLibrary.serviceProviderLibrary(classLoader))
        .isInstanceOf(CurrentPlatformNativeLibrary.class);
    }

    @Test
    void throwsWhenProviderSetWithSystemPropertyIsNotRegistered() {
      System.setProperty(NATIVE_LIBRARY, "com.example.Missing");
      assertThatThrownBy(() -> NativeLibrary.serviceProviderLibrary(classLoader))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageStartingWith("NativeLibrary implementation com.example.Missing (set with the com.marcnuri.jkube-helm.nativeLibrary system property) not found");
    }
  }

  @Nested
  class MixedVersions {

    @Test
    void prefersProviderForCurrentPlatformOverPreviousUndeclaredProvider() throws IOException {
      // An undeclared provider (e.g. a platform JAR prior to OS/arch declarations) is registered first
      try (URLClassLoader classLoader = servicesClassLoader(
        UndeclaredNativeLibrary.class, OtherPlatformNativeLibrary.class, CurrentPlatformNativeLibrary.class)) {
        assertThat(NativeLibrary.serviceProviderLibrary(classLoader))
          .isInstanceOf(CurrentPlatformNativeLibrary.class);
      }
    }

    @Test
    void fallsBackToUndeclaredProvider() throws IOException {
      try (URLClassLoader classLoader = servicesClassLoader(
        OtherPlatformNativeLibrary.class, UndeclaredNativeLibrary.class)) {
        assertThat(NativeLibrary.serviceProviderLibrary(classLoader))
          .isInstanceOf(UndeclaredNativeLibrary.class);
      }
    }
  }

  @Nested
  class NoCompatibleProvider {

    @Test
    void throwsWithPlatformAndAvailableProviders() throws IOException {
      try (URLClassLoader classLoader = servicesClassLoader(OtherPlatformNativeLibrary.class)) {
        assertThatThrownBy(() -> NativeLibrary.serviceProviderLibrary(classLoader))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("No NativeLibrary implementation found for the current platform (os.name=" +
            System.getProperty("os.name") + ", os.arch=" + System.getProperty("os.arch") + ")")
          .hasMessageContaining(OtherPlatformNativeLibrary.class.getName());
      }
    }
  }

  @Nested
  class TempDirectory {

    @Test
    void defaultsToJavaIoTmpdir() throws IOException {
      final Path result = new CurrentPlatformNativeLibrary().createTempDirectory();
      try {
        assertThat(result.getParent().toRealPath())
          .isEqualTo(Paths.get(System.getProperty("java.io.tmpdir")).toRealPath());
      } finally {
        Files.deleteIfExists(result);
      }
    }

    @Test
    void usesDirectorySetWithSystemProperty() {
      final Path baseDir = tempDir.resolve("extract").resolve("here");
      System.setProperty(TEMP_DIR, baseDir.toString());
      final Path result = new CurrentPlatformNativeLibrary().createTempDirectory();
      assertThat(result)
        .isDirectory()
        .hasParent(baseDir);
      assertThat(result.getFileName().toString()).startsWith("helm-java");
    }

    @Test
    void usesSymbolicLinkToDirectorySetWithSystemProperty() throws IOException {
      final Path target = Files.createDirectory(tempDir.resolve("target"));
      final Path link;
      try {
        link = Files.createSymbolicLink(tempDir.resolve("link"), target);
      } catch (UnsupportedOperationException exception) {
        assumeTrue(false, "Symbolic links not supported: " + exception);
        return;
      } catch (FileSystemException exception) {
        // Windows without the privilege to create symbolic links; a failure anywhere else fails the test
        assumeFalse(System.getProperty("os.name").startsWith("Windows"), "Symbolic links not allowed: " + exception);
        throw exception;
      }
      System.setProperty(TEMP_DIR, link.toString());
      final Path result = new CurrentPlatformNativeLibrary().createTempDirectory();
      assertThat(result)
        .isDirectory()
        .hasParent(link);
    }

    @Test
    void throwsWhenDirectorySetWithSystemPropertyIsAFile() throws IOException {
      final Path file = Files.createFile(tempDir.resolve("a-file"));
      System.setProperty(TEMP_DIR, file.toString());
      final NativeLibrary nativeLibrary = new CurrentPlatformNativeLibrary();
      assertThatThrownBy(nativeLibrary::createTempDirectory)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Unable to create temporary directory in " + file +
          " (set with the com.marcnuri.jkube-helm.tempDir system property)");
    }
  }

  private static void restore(String key, String value) {
    if (value == null) {
      System.clearProperty(key);
    } else {
      System.setProperty(key, value);
    }
  }

  private URLClassLoader servicesClassLoader(Class<?>... providers) throws IOException {
    final Path root = Files.createTempDirectory(tempDir, "services");
    final Path services = Files.createDirectories(root.resolve("META-INF").resolve("services"));
    final StringBuilder content = new StringBuilder();
    for (Class<?> provider : providers) {
      content.append(provider.getName()).append('\n');
    }
    Files.write(services.resolve(NativeLibrary.class.getName()), content.toString().getBytes(StandardCharsets.UTF_8));
    return new URLClassLoader(new URL[]{root.toUri().toURL()}, NativeLibraryServiceProviderTest.class.getClassLoader());
  }

  public static final class CurrentPlatformNativeLibrary implements NativeLibrary {
    @Override
    public String getBinaryName() {
      return "helm-current";
    }

    @Override
    public String getOs() {
      return Platform.osName();
    }

    @Override
    public String getArch() {
      return Platform.archName();
    }
  }

  public static final class UndeclaredNativeLibrary implements NativeLibrary {
    @Override
    public String getBinaryName() {
      return "helm-undeclared";
    }
  }

  public static final class OtherPlatformNativeLibrary implements NativeLibrary {
    @Override
    public String getBinaryName() {
      return "helm-other";
    }

    @Override
    public String getOs() {
      return "linux".equals(Platform.osName()) ? "windows" : "linux";
    }

    @Override
    public String getArch() {
      return Platform.archName();
    }
  }
}
