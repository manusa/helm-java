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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Marc Nuri
 * @author Andres F. Vallecilla
 * @author Antonio Fernandez Alhambra
 */
class HelmTemplateTest {

  @Nested
  class FromLocalChart {

    @TempDir
    private Path tempDir;
    private Helm helm;

    @BeforeEach
    void setUp() {
      helm = Helm.create().withName("local-chart-test").withDir(tempDir).call();
    }

    @Test
    void withDefaults() {
      final String result = helm.template().call();
      assertThat(result)
        .contains("name: release-name-local-chart-test");
    }

    @Test
    void withName() {
      final String result = helm.template().withName("the-name").call();
      assertThat(result)
        .contains("name: the-name-local-chart-test");
    }

    @Test
    void withValues() {
      final String result = helm.template()
        .set("replicaCount", 1337)
        .call();
      assertThat(result)
        .contains("replicas: 1337");
    }

    @Test
    void withValuesFile() throws IOException {
      final Path valuesFile = Files.write(tempDir.resolve("test-values.yaml"),
      "replicaCount: 313373\n".getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
      final String result = helm.template()
        .withValuesFile(valuesFile)
        .call();
      assertThat(result)
        .contains("replicas: 313373");
    }

    @Test
    void withInvalidValues() {
      final TemplateCommand templateCommand = helm.template()
        .set("ingress.enabled", true)
        .set("ingress.annotations", "- invalid value - -- - -");
      assertThatThrownBy(templateCommand::call)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("YAML parse error on local-chart-test/templates/ingress.yaml: error unmarshaling JSON")
        .hasMessageContaining("Use --debug flag to render out invalid YAML")
        .hasMessageNotContaining("# Source:");
    }

    @Test
    void withInvalidValuesAndDebug() {
      final TemplateCommand templateCommand = helm.template()
        .set("ingress.enabled", true)
        .set("ingress.annotations", "- invalid value - -- - -")
        .debug();
      assertThatThrownBy(templateCommand::call)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("YAML parse error on local-chart-test/templates/ingress.yaml: error unmarshaling JSON")
        .hasMessageContaining("# Source: local-chart-test")
        .hasMessageContaining("name: release-name-local-chart-test");
    }

    @Test
    void skipCrdsWithoutCrdsInChart() {
      final String result = helm.template()
        .skipCrds()
        .call();
      assertThat(result)
        .contains("name: release-name-local-chart-test");
    }
  }

  @Nested
  class FromLocalChartWithCrds {

    @TempDir
    private Path tempDir;
    private Helm helm;

    @BeforeEach
    void setUp() throws IOException {
      helm = Helm.create().withName("chart-with-crds").withDir(tempDir).call();
      Files.write(Files.createDirectories(tempDir.resolve("chart-with-crds").resolve("crds")).resolve("crd.yaml"),
        ("apiVersion: apiextensions.k8s.io/v1\n" +
          "kind: CustomResourceDefinition\n" +
          "metadata:\n" +
          "  name: widgets.example.com\n" +
          "spec:\n" +
          "  group: example.com\n" +
          "  names:\n" +
          "    kind: Widget\n" +
          "    plural: widgets\n" +
          "  scope: Namespaced\n" +
          "  versions:\n" +
          "    - name: v1\n" +
          "      served: true\n" +
          "      storage: true\n" +
          "      schema:\n" +
          "        openAPIV3Schema:\n" +
          "          type: object\n").getBytes(StandardCharsets.UTF_8),
        StandardOpenOption.CREATE);
    }

    @Test
    void withDefaultsDoesNotIncludeCrds() {
      // Helm template by default does not include CRDs from the crds/ directory
      final String result = helm.template().call();
      assertThat(result)
        .doesNotContain("kind: CustomResourceDefinition")
        .contains("name: release-name-chart-with-crds");
    }

    @Test
    void skipCrdsWithCrdsInChartWorks() {
      // skipCrds ensures CRDs are not included even when the chart contains them
      final String result = helm.template()
        .skipCrds()
        .call();
      assertThat(result)
        .doesNotContain("kind: CustomResourceDefinition")
        .doesNotContain("name: widgets.example.com")
        .contains("name: release-name-chart-with-crds");
    }
  }

  @Nested
  class FromRepo {

    @TempDir
    private Path tempDir;
    private Path repositoryConfig;

    @BeforeEach
    void setUp() {
      repositoryConfig = tempDir.resolve("repositories.yaml");
      Helm.repo().add().withRepositoryConfig(repositoryConfig)
        .withName("stable")
        .withUrl(URI.create("https://charts.helm.sh/stable"))
        .insecureSkipTlsVerify()
        .call();
    }

    @Test
    void withDefaults() {
      final String result = Helm.template("stable/nginx-ingress")
        .withRepositoryConfig(repositoryConfig)
        .call();
      assertThat(result)
        .contains("name: release-name-nginx-ingress")
        .contains("chart: nginx-ingress-");
    }

    @Test
    void withNamespace() {
      final String result = Helm.template("stable/nginx-ingress")
        .withRepositoryConfig(repositoryConfig)
        .withNamespace("the-namespace")
        .call();
      assertThat(result)
        .contains("namespace: the-namespace");
    }
  }
}
