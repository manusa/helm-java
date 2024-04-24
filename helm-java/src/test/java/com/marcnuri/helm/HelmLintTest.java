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

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HelmLintTest {

  @TempDir
  private Path tempDir;
  private Helm helm;

  @BeforeEach
  void setUp() {
    helm = Helm.create().withName("test").withDir(tempDir).call();
  }

  @Test
  @DisplayName("invalid path")
  void reportsErrorAndFails() {
    final LintResult result = new Helm(Paths.get("/invalid")).lint().call();
    assertThat(result)
      .hasFieldOrPropertyWithValue("failed", true)
      .extracting(LintResult::getMessages)
      .asInstanceOf(InstanceOfAssertFactories.list(String.class))
      .singleElement().asString()
      .startsWith("Error unable to check Chart.yaml file in chart:");
  }

  @Nested
  @DisplayName("valid chart")
  class ValidWithInfo {


    @Test
    @DisplayName("is not failed")
    void isNotFailed() {
      final LintResult result = helm.lint().call();
      assertThat(result.isFailed()).isFalse();
    }

    @Test
    @DisplayName("with INFO message, returns INFO message")
    void returnsInfoMessage() {
      final List<String> result = helm.lint().call().getMessages();
      assertThat(result)
        .singleElement()
        .isEqualTo("[INFO] Chart.yaml: icon is recommended");
    }

    @Test
    @DisplayName("with complete chart, returns empty")
    void completeChartReturnsEmpty() throws IOException {
      Files.write(tempDir.resolve("test").resolve("Chart.yaml"),
        "\nicon: https://valid-url".getBytes(StandardCharsets.UTF_8),
        StandardOpenOption.APPEND
      );
      final List<String> result = helm.lint().call().getMessages();
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("with INFO message and quiet, returns empty")
    void quietReturnsEmpty() {
      final List<String> result = helm.lint().quiet().call().getMessages();
      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("invalid chart")
  class invalid {

    @BeforeEach
    void setUp() throws IOException {
      Files.write(tempDir.resolve("test").resolve("Chart.yaml"),
        "\nicon: ://invalid-url".getBytes(StandardCharsets.UTF_8),
        StandardOpenOption.APPEND
      );
    }

    @Test
    @DisplayName("is not failed")
    void isFailed() {
      final LintResult result = helm.lint().call();
      assertThat(result.isFailed()).isTrue();
    }

    @Test
    @DisplayName("with ERROR message, returns ERROR message")
    void returnsErrorMessage() {
      final List<String> result = helm.lint().call().getMessages();
      assertThat(result)
        .singleElement()
        .isEqualTo("[ERROR] Chart.yaml: invalid icon URL '://invalid-url'");
    }

  }

  @Nested
  @DisplayName("warning chart")
  class TemplatesDirIsFile {

    @BeforeEach
    void setUp() throws IOException {
      Files.walk(tempDir.resolve("test").resolve("templates"))
        .sorted(Comparator.reverseOrder())
        .map(Path::toFile)
        .forEach(File::delete);
      Files.createFile(tempDir.resolve("test").resolve("templates"));
    }

    @Test
    void infoAndWarn() {
      final LintResult result = helm.lint().call();
      assertThat(result.getMessages())
        .containsExactlyInAnyOrder(
          "[INFO] Chart.yaml: icon is recommended",
          "[WARNING] templates/: not a directory"
        );
    }

    @Test
    void withQuietWarn() {
      final LintResult result = helm.lint().quiet().call();
      assertThat(result.getMessages())
        .containsExactlyInAnyOrder(
          "[WARNING] templates/: not a directory"
        );
    }

    @Test
    void isNotFailed() {
      final LintResult result = helm.lint().call();
      assertThat(result).hasFieldOrPropertyWithValue("failed", false);
    }

    @Test
    @DisplayName("with strict, is failed")
    void withStrictIsFailed() {
      final LintResult result = helm.lint().strict().call();
      assertThat(result).hasFieldOrPropertyWithValue("failed", true);
    }
  }

}
