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

import java.util.Locale;

/**
 * Detects the operating system and architecture of the running JVM using the names of the
 * platform-specific native library modules (e.g. {@code linux}, {@code arm64}).
 *
 * @author Marc Nuri
 */
final class Platform {

  private Platform() {
  }

  /**
   * @return the OS name of the running JVM ({@code darwin}, {@code linux}, {@code windows}), or {@code null} if unsupported.
   */
  static String osName() {
    return osName(System.getProperty("os.name"));
  }

  /**
   * @return the architecture of the running JVM ({@code amd64}, {@code arm64}), or {@code null} if unsupported.
   */
  static String archName() {
    return archName(System.getProperty("os.arch"));
  }

  static String osName(String osNameProperty) {
    if (osNameProperty == null) {
      return null;
    }
    final String osName = osNameProperty.toLowerCase(Locale.ROOT);
    if (osName.contains("win")) {
      return "windows";
    } else if (osName.contains("mac")) {
      return "darwin";
    } else if (osName.contains("nux") || osName.contains("nix") || osName.contains("aix")) {
      return "linux";
    }
    return null;
  }

  static String archName(String osArchProperty) {
    if (osArchProperty == null) {
      return null;
    }
    if (osArchProperty.equals("amd64") || osArchProperty.equals("x86_64")) {
      return "amd64";
    } else if (osArchProperty.equals("arm64") || osArchProperty.equals("aarch64")) {
      return "arm64";
    }
    return null;
  }
}
