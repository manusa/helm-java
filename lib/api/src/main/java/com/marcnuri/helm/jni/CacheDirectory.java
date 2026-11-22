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
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * The per-user directory where {@link RemoteJarLoader} caches the downloaded native library JARs.
 *
 * <p>The cached JARs contain native code that is loaded into the JVM, so other users must not be able to write the
 * directory or its files. On POSIX file systems the directory is created owner-only ({@code rwx------}), and an
 * existing one is refused if it's a symbolic link, belongs to another user, or is group- or world-writable. On other
 * file systems (Windows), the default location is in the user's profile, which other users can't write.
 *
 * <p>If the default location can't be used (no home directory, a read-only file system, a parent owned by someone
 * else, a directory owned by root), a new private temporary directory is used instead, so the JAR is downloaded
 * again by each JVM. A directory set with the {@code com.marcnuri.jkube-helm.cacheDir} system property, and any
 * unsafe directory, fail instead.
 *
 * @author Marc Nuri
 */
final class CacheDirectory {

  /**
   * System property to set the directory where the downloaded native library JARs are cached.
   */
  static final String CACHE_DIR = "com.marcnuri.jkube-helm.cacheDir";

  private static final String PROBE_PREFIX = "helm-java-owner";
  private static final String PROBE_SUFFIX = ".probe";
  private static final Logger LOGGER = Logger.getLogger(CacheDirectory.class.getName());

  private static final Set<PosixFilePermission> OWNER_ONLY_DIRECTORY = PosixFilePermissions.fromString("rwx------");
  private static final Set<PosixFilePermission> OWNER_ONLY_FILE = PosixFilePermissions.fromString("rw-------");

  private final Path directory;
  private final boolean temporary;

  private CacheDirectory(Path directory, boolean temporary) {
    this.directory = directory;
    this.temporary = temporary;
  }

  Path getDirectory() {
    return directory;
  }

  /**
   * Whether this is a per-JVM temporary directory (the default location couldn't be used).
   */
  boolean isTemporary() {
    return temporary;
  }

  /**
   * The cache directory set with the {@code com.marcnuri.jkube-helm.cacheDir} system property, or the default one.
   */
  static CacheDirectory select() throws IOException {
    return select(System.getProperty(CACHE_DIR), Platform.osName(), System::getenv, System.getProperty("user.home"));
  }

  static CacheDirectory select(String configured, String osName, Function<String, String> getenv, String userHome)
    throws IOException {
    if (configured != null && !configured.trim().isEmpty()) {
      final Path directory;
      try {
        directory = Paths.get(configured);
      } catch (InvalidPathException exception) {
        throw new IOException("Invalid native library cache directory (set with the " + CACHE_DIR +
          " system property): " + configured, exception);
      }
      return new CacheDirectory(ensurePrivate(directory), false);
    }
    Path defaultDirectory;
    try {
      defaultDirectory = defaultDirectory(osName, getenv, userHome);
    } catch (InvalidPathException exception) {
      defaultDirectory = null;
    }
    final String reason;
    // e.g. containers running with an arbitrary user id that has no home directory (user.home=?)
    if (defaultDirectory != null && defaultDirectory.isAbsolute()) {
      try {
        return new CacheDirectory(ensurePrivate(defaultDirectory), false);
      } catch (UnsafeDirectoryException exception) {
        throw exception;
      } catch (IOException exception) {
        // Unusable (not unsafe): use a private directory for this JVM only
        reason = exception.getMessage();
      }
    } else {
      reason = "no per-user directory for user.home=" + userHome;
    }
    final Path temporary;
    try {
      temporary = Files.createTempDirectory("helm-java-cache");
    } catch (IOException exception) {
      throw new IOException("Unable to use the default native library cache directory (" + reason +
        "), nor a temporary directory in java.io.tmpdir (" + exception + "); set one with the " + CACHE_DIR +
        " system property", exception);
    }
    temporary.toFile().deleteOnExit();
    LOGGER.warning(() -> "Downloading the native library to the temporary directory " + temporary +
      ", downloaded again by each JVM, because the default cache directory can't be used: " + reason);
    return new CacheDirectory(temporary, true);
  }

  /**
   * The default cache directory for the given platform.
   *
   * <ul>
   *   <li>Windows: {@code %LOCALAPPDATA%\helm-java\cache}, or {@code <user.home>\AppData\Local\helm-java\cache}.</li>
   *   <li>Others: {@code $XDG_CACHE_HOME/helm-java} if set to an absolute path, or {@code <user.home>/.cache/helm-java}.</li>
   * </ul>
   */
  static Path defaultDirectory(String osName, Function<String, String> getenv, String userHome) {
    if (userHome == null) {
      // Relative, so not used by select()
      userHome = "";
    }
    if ("windows".equals(osName)) {
      final String localAppData = getenv.apply("LOCALAPPDATA");
      final Path base = isAbsolute(localAppData) ? Paths.get(localAppData) : Paths.get(userHome, "AppData", "Local");
      return base.resolve("helm-java").resolve("cache");
    }
    final String xdgCacheHome = getenv.apply("XDG_CACHE_HOME");
    final Path base = isAbsolute(xdgCacheHome) ? Paths.get(xdgCacheHome) : Paths.get(userHome, ".cache");
    return base.resolve("helm-java");
  }

  /**
   * Creates the directory if missing (owner-only on POSIX file systems) and checks that it's usable and that other
   * users can't write it.
   *
   * @return the directory.
   * @throws UnsafeDirectoryException if it can't be trusted (a symbolic link, another user's, writable by others).
   * @throws IOException              if it can't be used (it can't be created, isn't a directory, isn't writable).
   */
  static Path ensurePrivate(Path directory) throws IOException {
    if (Files.isSymbolicLink(directory)) {
      throw new UnsafeDirectoryException(directory, "it's a symbolic link");
    }
    if (!Files.exists(directory, LinkOption.NOFOLLOW_LINKS)) {
      try {
        if (isPosix()) {
          Files.createDirectories(directory, PosixFilePermissions.asFileAttribute(OWNER_ONLY_DIRECTORY));
        } else {
          Files.createDirectories(directory);
        }
      } catch (IOException exception) {
        throw unusable(directory, "it can't be created (" + exception + ")", exception);
      }
    }
    if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) {
      throw unusable(directory, "it's not a directory", null);
    }
    if (isPosix()) {
      final PosixFileAttributes attributes =
        Files.readAttributes(directory, PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
      // Checked first: it doesn't need the current user
      if (isWritableByOthers(attributes)) {
        throw new UnsafeDirectoryException(directory, "it's group- or world-writable (" +
          PosixFilePermissions.toString(attributes.permissions()) + "), restrict it with chmod 700");
      }
      final UserPrincipal currentUser;
      try {
        currentUser = currentUser();
      } catch (IOException exception) {
        throw unusable(directory, "the user this JVM runs as can't be determined (" + exception + ")", exception);
      }
      if (!attributes.owner().equals(currentUser)) {
        if (isOwnedByRoot(directory, attributes)) {
          // Other users can't write it, but neither can this one (e.g. created while building a container image)
          throw unusable(directory, "it's owned by root", null);
        }
        throw new UnsafeDirectoryException(directory, "it's owned by " + attributes.owner().getName());
      }
    }
    if (!Files.isWritable(directory)) {
      throw unusable(directory, "it's not writable", null);
    }
    return directory;
  }

  /**
   * Whether the file is a regular file (not a symbolic link) that, on POSIX file systems, belongs to the current
   * user and isn't writable by others.
   */
  static boolean isPrivateFile(Path file) throws IOException {
    if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
      return false;
    }
    if (isPosix()) {
      final PosixFileAttributes attributes =
        Files.readAttributes(file, PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
      return !isWritableByOthers(attributes) && attributes.owner().equals(currentUser());
    }
    return true;
  }

  /**
   * The attributes to create a file only its owner can read or write ({@code rw-------}) with, whatever the umask.
   */
  static FileAttribute<?>[] ownerOnlyFile() {
    if (isPosix()) {
      return new FileAttribute<?>[]{PosixFilePermissions.asFileAttribute(OWNER_ONLY_FILE)};
    }
    return new FileAttribute<?>[0];
  }

  private static boolean isPosix() {
    return FileSystems.getDefault().supportedFileAttributeViews().contains("posix");
  }

  private static boolean isWritableByOthers(PosixFileAttributes attributes) {
    return attributes.permissions().contains(PosixFilePermission.GROUP_WRITE) ||
      attributes.permissions().contains(PosixFilePermission.OTHERS_WRITE);
  }

  /**
   * The user this JVM runs as (its effective user): more reliable than looking up {@code user.name}, which doesn't
   * always match it (e.g. containers running with an arbitrary user id).
   *
   * <p>Never determined with a file in the directory being checked: before that directory is trusted, another user
   * who can rename files in it could swap such a file between its creation and the owner lookup. Instead:
   * <ol>
   *   <li>Where available (Linux), the owner of {@code /proc/self}: the process' effective user, which no other user
   *   can change. Nothing is written.</li>
   *   <li>Otherwise, the owner of a probe file created (and deleted right away) in {@code java.io.tmpdir}, which is
   *   sticky, or private to the user (as on macOS), so other users can't rename or replace it. A
   *   {@code java.io.tmpdir} deliberately set to a non-sticky directory other users can write isn't protected.</li>
   * </ol>
   */
  private static UserPrincipal currentUser() throws IOException {
    final Path self = Paths.get("/proc/self");
    try {
      if (Files.isDirectory(self)) {
        return Files.getOwner(self);
      }
    } catch (IOException | SecurityException exception) {
      // No usable /proc: probe
    }
    final Path probe = Files.createTempFile(PROBE_PREFIX, PROBE_SUFFIX);
    try {
      return Files.getOwner(probe, LinkOption.NOFOLLOW_LINKS);
    } finally {
      Files.deleteIfExists(probe);
    }
  }

  /**
   * Whether the file is owned by root: by user id where the {@code unix} attribute view is available (e.g. when root
   * has no name because {@code /etc/passwd} is missing), otherwise by name. Under user namespaces or NFS root squash
   * the user id itself is a different one (e.g. 65534), so root-owned files aren't recognised there; they're then
   * refused as another user's, which fails safe.
   */
  private static boolean isOwnedByRoot(Path file, PosixFileAttributes attributes) {
    try {
      final Object uid = Files.getAttribute(file, "unix:uid", LinkOption.NOFOLLOW_LINKS);
      if (uid instanceof Integer) {
        return (Integer) uid == 0;
      }
    } catch (IOException | UnsupportedOperationException | IllegalArgumentException exception) {
      // No unix attribute view: by name
    }
    return "root".equals(attributes.owner().getName());
  }

  private static boolean isAbsolute(String path) {
    try {
      return path != null && !path.trim().isEmpty() && Paths.get(path).isAbsolute();
    } catch (InvalidPathException exception) {
      return false;
    }
  }

  private static IOException unusable(Path directory, String reason, IOException cause) {
    return new IOException(message(directory, reason), cause);
  }

  private static String message(Path directory, String reason) {
    return "Unable to use " + directory + " as the native library cache directory: " + reason +
      " (another directory can be set with the " + CACHE_DIR + " system property)";
  }

  /**
   * The directory can't be trusted: other users may be able to write it.
   */
  static final class UnsafeDirectoryException extends IOException {
    UnsafeDirectoryException(Path directory, String reason) {
      super(message(directory, reason));
    }
  }
}
