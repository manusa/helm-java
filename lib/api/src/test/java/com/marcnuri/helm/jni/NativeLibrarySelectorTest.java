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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Marc Nuri
 */
class NativeLibrarySelectorTest {

  private final NativeLibrary linuxArm64 = new FakeNativeLibrary("linux", "arm64");
  private final NativeLibrary linuxAmd64 = new FakeNativeLibrary("linux", "amd64");
  private final NativeLibrary darwinArm64 = new FakeNativeLibrary("darwin", "arm64");
  private final NativeLibrary undeclared = new UndeclaredNativeLibrary();

  @Nested
  class ByPlatform {

    @Test
    void selectsLinuxAmd64OnLinuxX8664() {
      assertThat(NativeLibrarySelector.select(Arrays.asList(linuxArm64, linuxAmd64), null, "Linux", "amd64"))
        .isSameAs(linuxAmd64);
    }

    @Test
    void selectsLinuxArm64OnLinuxAarch64() {
      assertThat(NativeLibrarySelector.select(Arrays.asList(linuxAmd64, linuxArm64), null, "Linux", "aarch64"))
        .isSameAs(linuxArm64);
    }

    @Test
    void selectsDarwinArm64OnMacOsAarch64() {
      assertThat(NativeLibrarySelector.select(Arrays.asList(linuxArm64, darwinArm64), null, "Mac OS X", "aarch64"))
        .isSameAs(darwinArm64);
    }

    @Test
    void prefersMatchingImplementationOverPreviousUndeclaredImplementation() {
      // e.g. an older platform JAR (no OS/arch declared) listed before the right, newer one
      assertThat(NativeLibrarySelector.select(Arrays.asList(linuxArm64, undeclared, linuxAmd64), null, "Linux", "amd64"))
        .isSameAs(linuxAmd64);
    }

    @Test
    void selectsUndeclaredImplementationWhenNoneMatches() {
      assertThat(NativeLibrarySelector.select(Arrays.asList(linuxArm64, undeclared, darwinArm64), null, "Linux", "amd64"))
        .isSameAs(undeclared);
    }

    @Test
    void selectsUndeclaredImplementationOnUnsupportedPlatform() {
      assertThat(NativeLibrarySelector.select(Arrays.asList(linuxAmd64, undeclared), null, "SunOS", "sparcv9"))
        .isSameAs(undeclared);
    }

    @Test
    void returnsNullWithNoImplementations() {
      assertThat(NativeLibrarySelector.select(Collections.<NativeLibrary>emptyList(), null, "Linux", "amd64"))
        .isNull();
    }

    @Test
    void throwsWithNoCompatibleImplementation() {
      assertThatThrownBy(() -> NativeLibrarySelector.select(Arrays.asList(linuxArm64, darwinArm64), null, "Linux", "amd64"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("No NativeLibrary implementation found for the current platform (os.name=Linux, os.arch=amd64), " +
          "available implementations: [" + FakeNativeLibrary.class.getName() + " (linux/arm64), " +
          FakeNativeLibrary.class.getName() + " (darwin/arm64)]. Please add the dependency for your platform to your project");
    }
  }

  @Nested
  class Forced {

    @Test
    void selectsForcedImplementationRegardlessOfPlatform() {
      final NativeLibrary result = NativeLibrarySelector.select(
        Arrays.asList(linuxAmd64, undeclared), UndeclaredNativeLibrary.class.getName(), "Linux", "amd64");
      assertThat(result).isSameAs(undeclared);
    }

    @Test
    void ignoresBlankForcedImplementation() {
      final NativeLibrary result = NativeLibrarySelector.select(
        Arrays.asList(linuxArm64, linuxAmd64), "  ", "Linux", "amd64");
      assertThat(result).isSameAs(linuxAmd64);
    }

    @Test
    void selectsForcedImplementationForAnotherPlatform() {
      final NativeLibrary result = NativeLibrarySelector.select(
        Arrays.asList(undeclared, linuxArm64), FakeNativeLibrary.class.getName(), "Linux", "amd64");
      assertThat(result).isSameAs(linuxArm64);
    }

    @Test
    void throwsWhenForcedImplementationNotFound() {
      assertThatThrownBy(() -> NativeLibrarySelector.select(
        Collections.singletonList(linuxAmd64), "com.example.Missing", "Linux", "amd64"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("NativeLibrary implementation com.example.Missing " +
          "(set with the com.marcnuri.jkube-helm.nativeLibrary system property) not found, " +
          "available implementations: [" + FakeNativeLibrary.class.getName() + " (linux/amd64)]");
    }
  }

  static final class FakeNativeLibrary implements NativeLibrary {
    private final String os;
    private final String arch;

    FakeNativeLibrary(String os, String arch) {
      this.os = os;
      this.arch = arch;
    }

    @Override
    public String getBinaryName() {
      return "helm-" + os + "-" + arch;
    }

    @Override
    public String getOs() {
      return os;
    }

    @Override
    public String getArch() {
      return arch;
    }
  }

  static final class UndeclaredNativeLibrary implements NativeLibrary {
    @Override
    public String getBinaryName() {
      return "helm-custom";
    }
  }
}
