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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static org.assertj.core.api.Assertions.assertThat;

class HelmShowTest {

  @TempDir
  private Path tempDir;
  private Helm helm;

  @BeforeEach
  void setUp() throws IOException {
    helm = Helm.create().withName("test").withDir(tempDir).call();
    Files.write(Files.createDirectories(tempDir.resolve("test").resolve("crds")).resolve("crd.yaml"),
      ("apiVersion: apiextensions.k8s.io/v1\n" +
        "kind: CustomResourceDefinition\n" +
        "metadata:\n" +
        "  name: tests.marcnuri.com").getBytes(StandardCharsets.UTF_8),
      StandardOpenOption.CREATE);
    Files.write(tempDir.resolve("test").resolve("README.md"),
      ("# Readme").getBytes(StandardCharsets.UTF_8),
      StandardOpenOption.CREATE);
  }

  @Test
  void all() {
    final String result = helm.show().all().call();
    assertThat(result).contains(
      "---",
      "name: test\n",
      "# Default values for test.",
      "kind: CustomResourceDefinition\n",
      "# Readme"
    );
  }

  @Test
  void chart() {
    final String result = helm.show().chart().call();
    assertThat(result)
      .contains("name: test\n")
      .doesNotContain(
        "---",
        "# Default values for test.",
        "kind: CustomResourceDefinition\n",
        "# Readme"
      );
  }

  @Test
  void crds() {
    final String result = helm.show().crds().call();
    assertThat(result)
      .contains(
        "kind: CustomResourceDefinition\n",
        "name: tests.marcnuri.com"
      )
      .doesNotContain(
        "---",
        "name: tests\n",
        "# Default values for test.",
        "# Readme"
      );
  }

  @Test
  void readme() {
    final String result = helm.show().readme().call();
    assertThat(result)
      .contains(
        "# Readme"
      )
      .doesNotContain(
        "---",
        "name: tests\n",
        "# Default values for test.",
        "kind: CustomResourceDefinition\n"
      );
  }

  @Test
  void values() {
    final String result = helm.show().values().call();
    assertThat(result)
      .contains(
        "# Default values for test."
      )
      .doesNotContain(
        "---",
        "name: tests\n",
        "kind: CustomResourceDefinition\n",
        "# Readme"
      );
  }
}
