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

import java.util.ArrayList;
import java.util.List;

/**
 * Chooses which of the available {@link NativeLibrary} implementations to load.
 *
 * @author Marc Nuri
 */
final class NativeLibrarySelector {

  /**
   * System property to force a specific {@link NativeLibrary} implementation (fully qualified class name).
   */
  static final String NATIVE_LIBRARY = "com.marcnuri.jkube-helm.nativeLibrary";
  /**
   * System property to set the base directory where the native library is extracted (defaults to {@code java.io.tmpdir}).
   */
  static final String TEMP_DIR = "com.marcnuri.jkube-helm.tempDir";

  private NativeLibrarySelector() {
  }

  /**
   * Selects the {@link NativeLibrary} to use from the provided implementations.
   *
   * <p>If {@code forcedNativeLibrary} is set (not blank), the implementation with that class name is returned,
   * regardless of its platform. Otherwise, implementations are chosen in this order of preference:
   * <ol>
   *   <li>The first implementation that declares an OS and/or architecture, all of them matching the provided
   *   {@code os.name} and {@code os.arch}.</li>
   *   <li>The first implementation that declares neither an OS nor an architecture (e.g. third-party
   *   implementations or platform modules prior to this change), which is assumed compatible with any platform.</li>
   * </ol>
   *
   * @return the selected implementation, or {@code null} if there are no implementations at all.
   * @throws IllegalStateException if there are implementations, but none of them can be selected.
   */
  static NativeLibrary select(
    Iterable<NativeLibrary> nativeLibraries, String forcedNativeLibrary, String osNameProperty, String osArchProperty) {
    final String forced = forcedNativeLibrary == null || forcedNativeLibrary.trim().isEmpty() ?
      null : forcedNativeLibrary.trim();
    final String os = Platform.osName(osNameProperty);
    final String arch = Platform.archName(osArchProperty);
    final List<NativeLibrary> available = new ArrayList<>();
    NativeLibrary firstUndeclared = null;
    for (NativeLibrary nativeLibrary : nativeLibraries) {
      if (forced != null) {
        if (nativeLibrary.getClass().getName().equals(forced)) {
          return nativeLibrary;
        }
      } else if (isUndeclared(nativeLibrary)) {
        if (firstUndeclared == null) {
          firstUndeclared = nativeLibrary;
        }
      } else if (matches(nativeLibrary, os, arch)) {
        return nativeLibrary;
      }
      available.add(nativeLibrary);
    }
    if (firstUndeclared != null) {
      return firstUndeclared;
    }
    if (available.isEmpty()) {
      return null;
    }
    if (forced != null) {
      throw new IllegalStateException("NativeLibrary implementation " + forced +
        " (set with the " + NATIVE_LIBRARY + " system property) not found, available implementations: " +
        describe(available));
    }
    throw new IllegalStateException("No NativeLibrary implementation found for the current platform (os.name=" +
      osNameProperty + ", os.arch=" + osArchProperty + "), available implementations: " + describe(available) +
      ". Please add the dependency for your platform to your project");
  }

  private static boolean isUndeclared(NativeLibrary nativeLibrary) {
    return nativeLibrary.getOs() == null && nativeLibrary.getArch() == null;
  }

  private static boolean matches(NativeLibrary nativeLibrary, String os, String arch) {
    return (nativeLibrary.getOs() == null || nativeLibrary.getOs().equals(os)) &&
      (nativeLibrary.getArch() == null || nativeLibrary.getArch().equals(arch));
  }

  private static String describe(List<NativeLibrary> nativeLibraries) {
    final StringBuilder sb = new StringBuilder("[");
    for (int i = 0; i < nativeLibraries.size(); i++) {
      final NativeLibrary nativeLibrary = nativeLibraries.get(i);
      if (i > 0) {
        sb.append(", ");
      }
      sb.append(nativeLibrary.getClass().getName())
        .append(" (").append(nativeLibrary.getOs()).append('/').append(nativeLibrary.getArch()).append(')');
    }
    return sb.append(']').toString();
  }
}
