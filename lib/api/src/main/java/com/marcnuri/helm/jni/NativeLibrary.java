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

import com.sun.jna.Native;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.ServiceLoader;

import static com.marcnuri.helm.jni.RemoteJarLoader.remoteJar;

/**
 * @author Marc Nuri
 */
public interface NativeLibrary {

  static NativeLibrary getInstance() {
    // Load from ClassPath (should work on Maven)
    NativeLibrary nativeLibrary = serviceProviderLibrary(null);
    if (nativeLibrary != null) {
      return nativeLibrary;
    }
    // Load from remote JAR (should work on Gradle if not air-gapped)
    IOException remoteJarFailure = null;
    try {
      final ClassLoader remoteJar = remoteJar();
      if (remoteJar != null && (nativeLibrary = serviceProviderLibrary(remoteJar)) != null) {
        return nativeLibrary;
      }
    } catch (IOException exception) {
      remoteJarFailure = exception;
    }
    throw new IllegalStateException(
      "No NativeLibrary implementation found, please add one of the supported dependencies to your project",
      remoteJarFailure);
  }

  /**
   * Returns the registered {@link NativeLibrary} implementation for the current platform.
   *
   * <p>The first implementation whose declared {@link #getOs()} and {@link #getArch()} match the running JVM is
   * preferred. If there is none, the first implementation that declares neither is used. Implementations declaring
   * another platform are never selected.
   * The {@code com.marcnuri.jkube-helm.nativeLibrary} system property can be set to the fully qualified class name
   * of a registered implementation to use that one instead.
   *
   * @param classLoader the class loader to look up the implementations with, or {@code null} for the context class loader.
   * @return the selected implementation, or {@code null} if no implementations are registered.
   * @throws IllegalStateException if implementations are registered, but none can be selected.
   */
  static NativeLibrary serviceProviderLibrary(ClassLoader classLoader) {
    return NativeLibrarySelector.select(
      ServiceLoader.load(NativeLibrary.class, classLoader == null ? Thread.currentThread().getContextClassLoader() : classLoader),
      System.getProperty(NativeLibrarySelector.NATIVE_LIBRARY),
      System.getProperty("os.name"),
      System.getProperty("os.arch")
    );
  }

  String getBinaryName();

  /**
   * The operating system the native binary is built for ({@code darwin}, {@code linux}, {@code windows}).
   *
   * @return the operating system, or {@code null} if the binary can be loaded on any operating system.
   */
  default String getOs() {
    return null;
  }

  /**
   * The architecture the native binary is built for ({@code amd64}, {@code arm64}).
   *
   * @return the architecture, or {@code null} if the binary can be loaded on any architecture.
   */
  default String getArch() {
    return null;
  }

  default HelmLib load() {
    final Path temp = createTempDirectory();
    final Path tempBinary = temp.resolve(getBinaryName());
    tempBinary.toFile().deleteOnExit();
    try (final InputStream stream = Objects.requireNonNull(getClass().getResourceAsStream("/" + getBinaryName()))) {
      Files.copy(stream, tempBinary, StandardCopyOption.REPLACE_EXISTING);
      final HelmLib helmLib = Native.load(tempBinary.toAbsolutePath().toString(), HelmLib.class);
      // Cleanup any resources that might have been left behind
      Runtime.getRuntime().addShutdownHook(new Thread(helmLib::RepoServerStopAll));
      return helmLib;
    } catch (IOException exception) {
      throw new IllegalStateException("Unable to load native library " + getBinaryName(), exception);
    }
  }

  /**
   * Creates the directory where the native binary is extracted.
   *
   * <p>The directory is created under the {@code com.marcnuri.jkube-helm.tempDir} system property if set
   * (creating it if missing), or under {@code java.io.tmpdir} otherwise.
   *
   * @return the created directory.
   */
  default Path createTempDirectory() {
    final String baseDir = System.getProperty(NativeLibrarySelector.TEMP_DIR);
    if (baseDir == null || baseDir.trim().isEmpty()) {
      try {
        final Path temp = Files.createTempDirectory("helm-java");
        temp.toFile().deleteOnExit();
        return temp;
      } catch (IOException exception) {
        throw new IllegalStateException("Unable to create temporary directory", exception);
      }
    }
    try {
      final Path base = Paths.get(baseDir);
      // On Java 8, Files.createDirectories throws FileAlreadyExistsException for an existing symbolic link to a
      // directory (e.g. /tmp on macOS); newer JDKs accept it, so keep this check while Java 8 is supported
      if (!Files.isDirectory(base)) {
        Files.createDirectories(base);
      }
      final Path temp = Files.createTempDirectory(base, "helm-java");
      temp.toFile().deleteOnExit();
      return temp;
    } catch (IOException | InvalidPathException exception) {
      throw new IllegalStateException("Unable to create temporary directory in " + baseDir +
        " (set with the " + NativeLibrarySelector.TEMP_DIR + " system property)", exception);
    }
  }

}
