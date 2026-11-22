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

import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * File helpers shared by the {@code lib/api} tests.
 *
 * @author Marc Nuri
 */
final class TestFiles {

  private TestFiles() {
  }

  static boolean isPosix() {
    return FileSystems.getDefault().supportedFileAttributeViews().contains("posix");
  }

  /**
   * Skips the test unless the default file system supports POSIX permissions.
   */
  static void assumePosix() {
    assumeTrue(isPosix(), "POSIX file permissions not supported");
  }

  static String permissions(Path path) throws IOException {
    return PosixFilePermissions.toString(Files.getPosixFilePermissions(path));
  }

  static void setPermissions(Path path, String permissions) throws IOException {
    Files.setPosixFilePermissions(path, PosixFilePermissions.fromString(permissions));
  }

  /**
   * Writes the file and, on POSIX file systems, makes it owner-only ({@code rw-------}) whatever the umask, like the
   * files the cache creates.
   */
  static Path writePrivate(Path file, byte[] content) throws IOException {
    Files.write(file, content);
    if (isPosix()) {
      setPermissions(file, "rw-------");
    }
    return file;
  }

  /**
   * Creates a symbolic link, or skips the test where they can't be created.
   */
  static void createSymbolicLink(Path link, Path target) throws IOException {
    try {
      Files.createSymbolicLink(link, target);
    } catch (UnsupportedOperationException exception) {
      assumeTrue(false, "Symbolic links not supported: " + exception);
    } catch (FileSystemException exception) {
      // Windows without the privilege to create symbolic links; a failure anywhere else fails the test
      assumeFalse(System.getProperty("os.name").startsWith("Windows"), "Symbolic links not allowed: " + exception);
      throw exception;
    }
  }
}
