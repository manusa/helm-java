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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marc Nuri
 */
class PlatformTest {

  @Nested
  class OsName {

    @Test
    void windows() {
      assertThat(Platform.osName("Windows 10")).isEqualTo("windows");
    }

    @Test
    void macOs() {
      assertThat(Platform.osName("Mac OS X")).isEqualTo("darwin");
    }

    @Test
    void linux() {
      assertThat(Platform.osName("Linux")).isEqualTo("linux");
    }

    @Test
    void unsupported() {
      assertThat(Platform.osName("SunOS")).isNull();
    }

    @Test
    void missing() {
      assertThat(Platform.osName(null)).isNull();
    }
  }

  @Nested
  class ArchName {

    @Test
    void amd64() {
      assertThat(Platform.archName("amd64")).isEqualTo("amd64");
    }

    @Test
    void x8664() {
      assertThat(Platform.archName("x86_64")).isEqualTo("amd64");
    }

    @Test
    void aarch64() {
      assertThat(Platform.archName("aarch64")).isEqualTo("arm64");
    }

    @Test
    void arm64() {
      assertThat(Platform.archName("arm64")).isEqualTo("arm64");
    }

    @Test
    void unsupported() {
      assertThat(Platform.archName("sparcv9")).isNull();
    }

    @Test
    void missing() {
      assertThat(Platform.archName(null)).isNull();
    }
  }
}
