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
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static com.marcnuri.helm.jni.TestFiles.assumePosix;
import static com.marcnuri.helm.jni.TestFiles.permissions;
import static com.marcnuri.helm.jni.TestFiles.setPermissions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * @author Marc Nuri
 */
class CacheDirectoryTest {

  @TempDir
  private Path tempDir;

  // A controlled environment: no test depends on the real one (e.g. LOCALAPPDATA on Windows, XDG_CACHE_HOME)
  private final Map<String, String> env = new HashMap<>();

  @Nested
  class DefaultDirectory {

    @Test
    void linuxUsesXdgCacheHome() {
      env.put("XDG_CACHE_HOME", tempDir.toString());
      assertThat(CacheDirectory.defaultDirectory("linux", env::get, "/home/user"))
        .isEqualTo(tempDir.resolve("helm-java"));
    }

    @Test
    void linuxIgnoresRelativeXdgCacheHome() {
      env.put("XDG_CACHE_HOME", "relative/cache");
      assertThat(CacheDirectory.defaultDirectory("linux", env::get, tempDir.toString()))
        .isEqualTo(tempDir.resolve(".cache").resolve("helm-java"));
    }

    @Test
    void linuxDefaultsToUserHomeCache() {
      assertThat(CacheDirectory.defaultDirectory("linux", env::get, tempDir.toString()))
        .isEqualTo(tempDir.resolve(".cache").resolve("helm-java"));
    }

    @Test
    void macOsDefaultsToUserHomeCache() {
      assertThat(CacheDirectory.defaultDirectory("darwin", env::get, tempDir.toString()))
        .isEqualTo(tempDir.resolve(".cache").resolve("helm-java"));
    }

    @Test
    void windowsUsesLocalAppData() {
      env.put("LOCALAPPDATA", tempDir.toString());
      assertThat(CacheDirectory.defaultDirectory("windows", env::get, "/home/user"))
        .isEqualTo(tempDir.resolve("helm-java").resolve("cache"));
    }

    @Test
    void windowsDefaultsToUserProfile() {
      assertThat(CacheDirectory.defaultDirectory("windows", env::get, tempDir.toString()))
        .isEqualTo(tempDir.resolve("AppData").resolve("Local").resolve("helm-java").resolve("cache"));
    }
  }

  @Nested
  class Select {

    private CacheDirectory selected;

    @AfterEach
    void deleteTemporaryDirectory() throws IOException {
      if (selected != null && selected.isTemporary()) {
        Files.deleteIfExists(selected.getDirectory());
      }
    }

    @Test
    void usesDirectorySetWithSystemProperty() throws IOException {
      selected = CacheDirectory.select(tempDir.resolve("custom").toString(), "linux", env::get, tempDir.toString());
      assertThat(selected.getDirectory()).isEqualTo(tempDir.resolve("custom")).isDirectory();
      assertThat(selected.isTemporary()).isFalse();
    }

    @Test
    void failsWhenDirectorySetWithSystemPropertyCantBeCreated() throws IOException {
      assumePosix();
      final Path readOnly = Files.createDirectory(tempDir.resolve("read-only"));
      setPermissions(readOnly, "r-x------");
      assumeTrue(!Files.isWritable(readOnly), "Running as a user that can write any directory (root)");
      final String configured = readOnly.resolve("cache").toString();
      assertThatThrownBy(() -> CacheDirectory.select(configured, "linux", env::get, tempDir.toString()))
        .isInstanceOf(IOException.class)
        .hasMessageStartingWith("Unable to use " + configured + " as the native library cache directory: it can't be created")
        .hasMessageEndingWith("(another directory can be set with the com.marcnuri.jkube-helm.cacheDir system property)");
    }

    @Test
    void failsWhenDirectorySetWithSystemPropertyIsUnsafe() throws IOException {
      assumePosix();
      final Path unsafe = Files.createDirectory(tempDir.resolve("unsafe"));
      setPermissions(unsafe, "rwxrwxrwx");
      assertThatThrownBy(() -> CacheDirectory.select(unsafe.toString(), "linux", env::get, tempDir.toString()))
        .isInstanceOf(CacheDirectory.UnsafeDirectoryException.class);
    }

    @Test
    void ignoresBlankSystemProperty() throws IOException {
      selected = CacheDirectory.select(" ", "linux", env::get, tempDir.toString());
      assertThat(selected.getDirectory()).isEqualTo(tempDir.resolve(".cache").resolve("helm-java"));
    }

    @Test
    void createsDefaultDirectory() throws IOException {
      selected = CacheDirectory.select(null, "linux", env::get, tempDir.toString());
      assertThat(selected.getDirectory()).isEqualTo(tempDir.resolve(".cache").resolve("helm-java")).isDirectory();
      assertThat(selected.isTemporary()).isFalse();
    }

    @Test
    void usesTemporaryDirectoryWithoutAbsoluteUserHome() throws IOException {
      selected = CacheDirectory.select(null, "linux", env::get, "?");
      assertTemporary(selected);
    }

    @Test
    void warnsWhenUsingTemporaryDirectory() throws IOException {
      final Logger logger = Logger.getLogger(CacheDirectory.class.getName());
      final List<LogRecord> records = new ArrayList<>();
      final Handler handler = new Handler() {
        @Override
        public void publish(LogRecord logRecord) {
          records.add(logRecord);
        }

        @Override
        public void flush() {
          // NO-OP
        }

        @Override
        public void close() {
          // NO-OP
        }
      };
      logger.addHandler(handler);
      try {
        selected = CacheDirectory.select(null, "linux", env::get, "?");
      } finally {
        logger.removeHandler(handler);
      }
      assertThat(records).singleElement()
        .returns(Level.WARNING, LogRecord::getLevel)
        .extracting(LogRecord::getMessage).asString()
        .contains(selected.getDirectory().toString(), "no per-user directory for user.home=?");
    }

    @Test
    void usesTemporaryDirectoryWithoutUserHome() throws IOException {
      selected = CacheDirectory.select(null, "linux", env::get, null);
      assertTemporary(selected);
    }

    @Test
    void usesTemporaryDirectoryWithNonexistentUserHome() throws IOException {
      // e.g. HOME=/nonexistent for system users
      final Path root = tempDir.getRoot();
      assumeTrue(!Files.isWritable(root), "Running as a user that can write the file system root");
      selected = CacheDirectory.select(null, "linux", env::get, root.resolve("nonexistent-helm-java-home").toString());
      assertTemporary(selected);
    }

    @Test
    void treatsRootOwnedDirectoryAsUnusableNotUnsafe() throws IOException {
      assumePosix();
      // e.g. a directory created while building a container image: safe, but not writable by this user
      final Path rootOwned = Paths.get("/usr");
      // By user id (under user namespaces root-owned files show as another user, e.g. nobody, uid 65534)
      assumeTrue(Files.isDirectory(rootOwned) &&
        Integer.valueOf(0).equals(Files.getAttribute(rootOwned, "unix:uid", LinkOption.NOFOLLOW_LINKS)) &&
        !Files.isWritable(rootOwned), "Needs a root-owned directory this user can't write");
      // So that as the default directory it's replaced by a temporary one instead of failing
      assertThatThrownBy(() -> CacheDirectory.ensurePrivate(rootOwned))
        .isInstanceOf(IOException.class)
        .isNotInstanceOf(CacheDirectory.UnsafeDirectoryException.class)
        .hasMessageContaining("it's owned by root");
    }

    @Test
    void usesTemporaryDirectoryWhenDefaultCantBeCreated() throws IOException {
      assumePosix();
      // e.g. a read-only home, or a ~/.cache created by root
      setPermissions(tempDir, "r-x------");
      try {
        assumeTrue(!Files.isWritable(tempDir), "Running as a user that can write any directory (root)");
        selected = CacheDirectory.select(null, "linux", env::get, tempDir.toString());
        assertTemporary(selected);
      } finally {
        setPermissions(tempDir, "rwx------");
      }
    }

    @Test
    void usesTemporaryDirectoryWhenDefaultIsNotWritable() throws IOException {
      assumePosix();
      final Path defaultDirectory = Files.createDirectories(tempDir.resolve(".cache").resolve("helm-java"));
      setPermissions(defaultDirectory, "r-x------");
      try {
        assumeTrue(!Files.isWritable(defaultDirectory), "Running as a user that can write any directory (root)");
        selected = CacheDirectory.select(null, "linux", env::get, tempDir.toString());
        assertTemporary(selected);
      } finally {
        setPermissions(defaultDirectory, "rwx------");
      }
    }

    @Test
    void failsWhenDefaultIsUnsafe() throws IOException {
      assumePosix();
      final Path defaultDirectory = Files.createDirectories(tempDir.resolve(".cache").resolve("helm-java"));
      setPermissions(defaultDirectory, "rwxrwx---");
      assertThatThrownBy(() -> CacheDirectory.select(null, "linux", env::get, tempDir.toString()))
        .isInstanceOf(CacheDirectory.UnsafeDirectoryException.class)
        .hasMessageContaining("it's group- or world-writable (rwxrwx---)");
    }

    private void assertTemporary(CacheDirectory cacheDirectory) throws IOException {
      assertThat(cacheDirectory.isTemporary()).isTrue();
      assertThat(cacheDirectory.getDirectory()).isDirectory();
      assertThat(cacheDirectory.getDirectory().toRealPath().getParent())
        .isEqualTo(Paths.get(System.getProperty("java.io.tmpdir")).toRealPath());
      if (TestFiles.isPosix()) {
        assertThat(permissions(cacheDirectory.getDirectory())).isEqualTo("rwx------");
      }
    }
  }

  @Nested
  class EnsurePrivate {

    @BeforeEach
    void posixOnly() {
      assumePosix();
    }

    @Test
    void createsMissingDirectoriesOwnerOnly() throws IOException {
      final Path directory = tempDir.resolve("parent").resolve("cache");
      assertThat(CacheDirectory.ensurePrivate(directory)).isEqualTo(directory);
      assertThat(permissions(directory)).isEqualTo("rwx------");
      assertThat(permissions(directory.getParent())).isEqualTo("rwx------");
    }

    @Test
    void acceptsExistingPrivateDirectoryWithoutWritingToIt() throws IOException {
      final Path directory = Files.createDirectory(tempDir.resolve("cache"));
      setPermissions(directory, "rwx------");
      // Creating (even deleting right away) a file in it would change its modification time
      // Whole seconds: some file systems (HFS+, FAT) don't store finer timestamps
      final FileTime modified = FileTime.from(System.currentTimeMillis() / 1000 - 3_600, TimeUnit.SECONDS);
      Files.setLastModifiedTime(directory, modified);
      assertThat(CacheDirectory.ensurePrivate(directory)).isEqualTo(directory);
      assertThat(Files.getLastModifiedTime(directory)).isEqualTo(modified);
    }

    @Test
    void acceptsReadableByOthers() throws IOException {
      final Path directory = Files.createDirectory(tempDir.resolve("cache"));
      setPermissions(directory, "rwxr-xr-x");
      assertThat(CacheDirectory.ensurePrivate(directory)).isEqualTo(directory);
    }

    @Test
    void refusesGroupWritableWithoutWritingToIt() throws IOException {
      final Path directory = Files.createDirectory(tempDir.resolve("cache"));
      setPermissions(directory, "rwxrwx---");
      // Before a directory is trusted, another user able to rename files in it could swap a file created there (e.g.
      // one to look up the current user): creating, even deleting right away, a file would change its modification time
      // Whole seconds: some file systems (HFS+, FAT) don't store finer timestamps
      final FileTime modified = FileTime.from(System.currentTimeMillis() / 1000 - 3_600, TimeUnit.SECONDS);
      Files.setLastModifiedTime(directory, modified);
      assertThatThrownBy(() -> CacheDirectory.ensurePrivate(directory))
        .isInstanceOf(CacheDirectory.UnsafeDirectoryException.class)
        .hasMessageStartingWith("Unable to use " + directory + " as the native library cache directory: " +
          "it's group- or world-writable (rwxrwx---)");
      assertThat(Files.getLastModifiedTime(directory)).isEqualTo(modified);
    }

    @Test
    void refusesGroupWritableBeforeLookingUpTheOwner() throws IOException {
      // Not writable by its owner: looking up the current user with a file in it would fail first
      final Path directory = Files.createDirectory(tempDir.resolve("cache"));
      setPermissions(directory, "r-xrwx---");
      try {
        assertThatThrownBy(() -> CacheDirectory.ensurePrivate(directory))
          .isInstanceOf(CacheDirectory.UnsafeDirectoryException.class)
          .hasMessageContaining("it's group- or world-writable (r-xrwx---)");
      } finally {
        setPermissions(directory, "rwx------");
      }
    }

    @Test
    void looksUpTheOwnerWithoutWritingToTheDirectory() throws IOException {
      // Before a directory is trusted, another user able to rename files in it could swap a file created there
      final Path directory = Files.createDirectory(tempDir.resolve("cache"));
      setPermissions(directory, "r-x------");
      try {
        assumeTrue(!Files.isWritable(directory), "Running as a user that can write any directory (root)");
        assertThatThrownBy(() -> CacheDirectory.ensurePrivate(directory))
          .isInstanceOf(IOException.class)
          .hasMessageContaining("it's not writable");
      } finally {
        setPermissions(directory, "rwx------");
      }
    }

    @Test
    void refusesWorldWritable() throws IOException {
      final Path directory = Files.createDirectory(tempDir.resolve("cache"));
      setPermissions(directory, "rwx---rwx");
      assertThatThrownBy(() -> CacheDirectory.ensurePrivate(directory))
        .isInstanceOf(CacheDirectory.UnsafeDirectoryException.class)
        .hasMessageContaining("it's group- or world-writable (rwx---rwx)");
    }

    @Test
    void refusesSymbolicLink() throws IOException {
      final Path target = Files.createDirectory(tempDir.resolve("target"));
      setPermissions(target, "rwx------");
      final Path link = tempDir.resolve("link");
      TestFiles.createSymbolicLink(link, target);
      assertThatThrownBy(() -> CacheDirectory.ensurePrivate(link))
        .isInstanceOf(CacheDirectory.UnsafeDirectoryException.class)
        .hasMessageContaining("it's a symbolic link");
    }

    @Test
    void refusesFileAsUnusable() throws IOException {
      final Path file = Files.createFile(tempDir.resolve("a-file"));
      assertThatThrownBy(() -> CacheDirectory.ensurePrivate(file))
        .isInstanceOf(IOException.class)
        .isNotInstanceOf(CacheDirectory.UnsafeDirectoryException.class)
        .hasMessageContaining("it's not a directory");
    }

    @Test
    void refusesNotWritableAsUnusable() throws IOException {
      final Path directory = Files.createDirectory(tempDir.resolve("cache"));
      setPermissions(directory, "r-x------");
      try {
        assumeTrue(!Files.isWritable(directory), "Running as a user that can write any directory (root)");
        assertThatThrownBy(() -> CacheDirectory.ensurePrivate(directory))
          .isInstanceOf(IOException.class)
          .isNotInstanceOf(CacheDirectory.UnsafeDirectoryException.class)
          .hasMessageContaining("it's not writable");
      } finally {
        setPermissions(directory, "rwx------");
      }
    }
  }

  @Nested
  class IsPrivateFile {

    @Test
    void acceptsOwnRegularFile() throws IOException {
      final Path file = TestFiles.writePrivate(tempDir.resolve("a-file"), new byte[]{1});
      assertThat(CacheDirectory.isPrivateFile(file)).isTrue();
    }

    @Test
    void rejectsGroupWritableFile() throws IOException {
      assumePosix();
      final Path file = Files.createFile(tempDir.resolve("a-file"));
      setPermissions(file, "rw-rw-r--");
      assertThat(CacheDirectory.isPrivateFile(file)).isFalse();
    }

    @Test
    void rejectsSymbolicLink() throws IOException {
      final Path target = Files.createFile(tempDir.resolve("target"));
      final Path link = tempDir.resolve("link");
      TestFiles.createSymbolicLink(link, target);
      assertThat(CacheDirectory.isPrivateFile(link)).isFalse();
    }

    @Test
    void rejectsDirectory() throws IOException {
      assertThat(CacheDirectory.isPrivateFile(Files.createDirectory(tempDir.resolve("a-directory")))).isFalse();
    }

    @Test
    void rejectsMissingFile() throws IOException {
      assertThat(CacheDirectory.isPrivateFile(tempDir.resolve("missing"))).isFalse();
    }
  }
}
