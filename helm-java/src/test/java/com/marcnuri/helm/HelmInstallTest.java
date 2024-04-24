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
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HelmInstallTest {

  @TempDir
  private Path tempDir;
  private Helm helm;

  @BeforeEach
  void setUp() {
    helm = Helm.create().withName("test").withDir(tempDir).call();
  }

  @Nested
  class Valid {

    @Test
    void withName() {
      final Release result = helm.install()
        .clientOnly()
        .withName("test")
        .call();
      assertThat(result)
        .returns("test", Release::getName)
        .returns("deployed", Release::getStatus)
        .returns("1", Release::getRevision)
        .satisfies(r -> assertThat(r.getLastDeployed()).matches(d -> d.toLocalDate().equals(LocalDate.now())))
        .returns("test-0.1.0", Release::getChart)
        .returns("1.16.0", Release::getAppVersion)
        .extracting(Release::getOutput).asString()
        .contains(
          "NAME: test\n",
          "LAST DEPLOYED: ",
          "STATUS: deployed",
          "REVISION: 1",
          "NOTES:"
        );
    }

    @Test
    void withPackagedChart(@TempDir Path destination) {
      helm.packageIt().withDestination(destination).call();
      final Release result = Helm.install(destination.resolve("test-0.1.0.tgz").toFile().getAbsolutePath())
        .clientOnly()
        .withName("test")
        .call();
      assertThat(result)
        .hasFieldOrPropertyWithValue("name", "test");
    }

    @Test
    void withGenerateName() {
      final Release result = helm.install()
        .clientOnly()
        .withName("test") // Should be ignored (omitted/not failure)
        .generateName()
        .call();
      assertThat(result)
        .hasFieldOrPropertyWithValue("status", "deployed")
        .extracting(Release::getName).asString()
        .startsWith("test-");
    }

    @Test
    void withGenerateNameAndNameTemplate() {
      final Release result = helm.install()
        .clientOnly()
        .generateName()
        .withNameTemplate("a-chart-{{randAlpha 6 | lower}}")
        .call();
      assertThat(result)
        .hasFieldOrPropertyWithValue("status", "deployed")
        .extracting(Release::getName).asString()
        .startsWith("a-chart-");
    }

    @Test
    void withNamespace() {
      final Release result = helm.install()
        .clientOnly()
        .withName("test")
        .withNamespace("test-namespace")
        .call();
      assertThat(result)
        .hasFieldOrPropertyWithValue("name", "test")
        .returns("test-namespace", Release::getNamespace);
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void withDependencyUpdate() throws IOException {
      Helm.create().withName("the-dependency").withDir(tempDir).call();
      Files.write(tempDir.resolve("test").resolve("Chart.yaml"),
        ("\ndependencies:\n" +
          "  - name: the-dependency\n" +
          "    version: 0.1.0\n" +
          "    repository: " + tempDir.resolve("the-dependency").toUri() + "\n").getBytes(StandardCharsets.UTF_8),
        StandardOpenOption.APPEND);
      final Release result = helm.install()
        .clientOnly()
        .withName("dependency")
        .dependencyUpdate()
        .call();
      assertThat(result)
        .hasFieldOrPropertyWithValue("name", "dependency")
        .extracting(Release::getOutput).asString()
        .contains(
          "Saving 1 charts",
          "Deleting outdated charts"
        );
      assertThat(tempDir.resolve("test").resolve("Chart.lock"))
        .exists()
        .isNotEmptyFile()
        .content().contains("name: the-dependency");
    }

    @Test
    void withDryRun() {
      final Release result = helm.install()
        .clientOnly()
        .withName("test")
        .dryRun()
        .withDryRunOption(DryRun.CLIENT)
        .call();
      assertThat(result)
        .hasFieldOrPropertyWithValue("name", "test")
        .hasFieldOrPropertyWithValue("status", "pending-install");
    }

    @Test
    void withValues() {
      final Release result = helm.install()
        .clientOnly()
        .debug()
        .withName("test")
        .set("corner", "\"'\\={[,.]}!?-_test=1,other=2")
        .set("bool", "true")
        .set("int", "1")
        .set("float", "1.1")
        .call();
      assertThat(result)
        .extracting(Release::getOutput).asString()
        .contains(
          "NAME: test\n",
          "USER-SUPPLIED VALUES:\n",
          "corner: '\"''\\={[,.]}!?-_test=1,other=2'\n",
          "bool: true\n",
          "int: 1\n",
          "float: \"1.1\"" // helm.sh/helm/v3/pkg/strvals does not support floats
        );
    }
  }

  @Nested
  class Invalid {

    @Test
    void withMissingChart() {
      final InstallCommand install = Helm.install("/tmp/nothing").clientOnly().withName("test");
      assertThatThrownBy(install::call)
        .message()
        .containsAnyOf(
          "path \"/tmp/nothing\" not found",
          "repo  not found"
        );
    }

    @Test
    void withMissingName() {
      final InstallCommand install = helm.install().clientOnly();
      assertThatThrownBy(install::call)
        .hasMessage("release name \"\": no name provided");
    }

    @Test
    void withDependencyUpdateAndMissing() throws IOException {
      Files.write(tempDir.resolve("test").resolve("Chart.yaml"),
        ("\ndependencies:\n" +
          "  - name: dependency\n" +
          "    version: 0.1.0\n" +
          "    repository: file://i-dont-exist\n").getBytes(StandardCharsets.UTF_8),
        StandardOpenOption.APPEND);
      final InstallCommand install = helm.install().clientOnly().withName("dependency-missing").dependencyUpdate();
      assertThatThrownBy(install::call)
        .hasMessageContaining(
          "An error occurred while updating chart dependencies:",
          "i-dont-exist not found"
        );
    }

//    @Test
//    void withDevelopmentVersionInChart() throws IOException {
//      final Path chartYaml = tempDir.resolve("test").resolve("Chart.yaml");
//      final String chart = new String(Files.readAllBytes(chartYaml), StandardCharsets.UTF_8);
//      Files.write(chartYaml, chart.replace("version: 0.1.0", "version: 0.1.0-SNAPSHOT").getBytes(StandardCharsets.UTF_8));
//      final InstallCommand install = helm.install()
//        .clientOnly()
//        .withName("development-version");
//      assertThatThrownBy(install::call)
//        .hasMessage("release name \"\": no name provided");
//    }
  }


}
