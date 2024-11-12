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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.k3s.K3sContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@EnabledOnOs(OS.LINUX)
class HelmKubernetesTest {

  static K3sContainer k3sContainer;
  static Path kubeConfig;

  private Helm helm;

  @BeforeAll
  static void setUpKubernetes(@TempDir Path tempDir) throws IOException {
    k3sContainer = new K3sContainer(DockerImageName.parse("rancher/k3s:v1.31.2-k3s1"));
    k3sContainer.start();
    kubeConfig = tempDir.resolve("config.yaml");
    Files.write(kubeConfig, k3sContainer.getKubeConfigYaml().getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
  }

  @AfterAll
  static void tearDownKubernetes() {
    k3sContainer.stop();
  }

  @BeforeEach
  void setUp(@TempDir Path tempDir) {
    helm = Helm.create().withName("test").withDir(tempDir).call();
  }

  @Nested
  class Install {
    @Nested
    class Valid {

      @Test
      void withName() {
        final Release result = helm.install()
          .withKubeConfig(kubeConfig)
          .withName("test")
          .call();
        assertThat(result)
          .extracting(Release::getOutput).asString()
          .contains(
            "NAME: test\n",
            "LAST DEPLOYED: ",
            "STATUS: deployed",
            "REVISION: 1"
          );
      }

      @Test
      void withDebug() {
        final Release result = helm.install()
          .withKubeConfig(kubeConfig)
          .withName("with-debug")
          .debug()
          .call();
        assertThat(result)
          .extracting(Release::getOutput).asString()
          .contains(
            "NAME: with-debug\n",
            "---\n",
            "creating 3 resource(s)"
          );
      }

      @Test
      void withWait() {
        final Release result = helm.install()
          .withKubeConfig(kubeConfig)
          .withName("with-wait")
          .waitReady()
          .debug()
          .call();
        assertThat(result)
          .extracting(Release::getOutput).asString()
          .contains(
            "beginning wait for 3 resources with timeout of 5m0s"
          );
      }

      @Test
      void withNamespaceAndCreateNamespace() {
        final Release result = helm.install()
          .withKubeConfig(kubeConfig)
          .withName("created-namespace")
          .withNamespace("to-be-created")
          .createNamespace()
          .debug().call();
        assertThat(result)
          .extracting(Release::getOutput).asString()
          .contains(
            "NAME: created-namespace\n",
            "---\n",
            "creating 1 resource(s)",
            "creating 3 resource(s)"
          );
      }

      // TODO: Add withDescription test when we can check the status (status command implementation)
    }

    @Nested
    class Invalid {

      @Test
      void missingNamespace() {
        final InstallCommand install = helm.install()
          .withKubeConfig(kubeConfig)
          .withName("missing-namespace")
          .withNamespace("non-existent");
        assertThatThrownBy(install::call)
          .message()
          .isEqualTo("create: failed to create: namespaces \"non-existent\" not found");
      }
    }

    @Nested
    class Failing {
      private Helm failingHelm;

      @BeforeEach
      void setUp(@TempDir Path tempDir) throws IOException {
        failingHelm = Helm.create().withName("test-failing").withDir(tempDir).call();
        Path podWithoutSpecPath = tempDir.resolve("test-failing").resolve("templates").resolve("pod-without-spec.yaml");
        Files.createFile(podWithoutSpecPath);
        Files.write(podWithoutSpecPath, (
          "apiVersion: v1\n" +
          "kind: Pod\n" +
          "metadata:\n" +
          "  name: {{ include \"test-failing.fullname\" . }}\n" +
          "  labels:\n" +
          "    {{- include \"test-failing.labels\" . | nindent 4 }}\n" +
          "spec:\n").getBytes(
                StandardCharsets.UTF_8), StandardOpenOption.CREATE);
      }

      @Test
      void withoutAtomic() {
        InstallCommand installCommand = failingHelm.install().withKubeConfig(kubeConfig).withName("test-fail");
        assertThatThrownBy(installCommand::call).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Pod \"test-fail-test-failing\" is invalid: spec.containers: Required value");

        assertThat(Helm.list().withKubeConfig(kubeConfig).call()).filteredOn(r -> r.getName().equals("test-fail"))
                .singleElement()
                .returns("failed", Release::getStatus);
      }

      @Test
      void withAtomic() {
        InstallCommand atomicInstallCommand =
                failingHelm.install().withKubeConfig(kubeConfig).withName("test-rollback").atomic();
        assertThatThrownBy(atomicInstallCommand::call).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Pod \"test-rollback-test-failing\" is invalid: spec.containers: Required value");

        assertThat(Helm.list().withKubeConfig(kubeConfig).call()).filteredOn(r -> r.getName().equals("test-rollback"))
                .isEmpty();
      }
    }
  }

  @Nested
  class HelmList {

    @BeforeEach
    void setUp() {
      helm.install().withKubeConfig(kubeConfig).withName("list-default").call();
      helm.install().withKubeConfig(kubeConfig)
        .withName("list-namespace").withNamespace("list-namespace").createNamespace().call();
    }

    @AfterEach()
    void tearDown() {
      Helm.uninstall("list-default").withKubeConfig(kubeConfig).call();
      Helm.uninstall("list-namespace").withKubeConfig(kubeConfig).withNamespace("list-namespace").call();
    }

    @Test
    void listsCurrentNamespace() {
      final List<Release> result = Helm.list().withKubeConfig(kubeConfig).call();
      assertThat(result)
        .filteredOn(r -> r.getName().equals("list-default"))
        .singleElement()
        .returns("list-default", Release::getName)
        .returns(null, Release::getNamespace)
        .returns("deployed", Release::getStatus)
        .returns("1", Release::getRevision)
        .returns("test-0.1.0", Release::getChart)
        .returns("1.16.0", Release::getAppVersion)
        .returns("", Release::getOutput)
        .extracting(Release::getLastDeployed)
        .matches(d -> d.toLocalDate().equals(LocalDate.now()));
    }

    @Test
    void listsSpecificNamespace() {
      final List<Release> result = Helm.list().withKubeConfig(kubeConfig)
        .withNamespace("list-namespace")
        .call();
      assertThat(result)
        .singleElement()
        .returns("list-namespace", Release::getName)
        .returns("list-namespace", Release::getNamespace);
    }

    @Test
    void listsAllNamespaces() {
      final List<Release> result = Helm.list().withKubeConfig(kubeConfig)
        .allNamespaces()
        .call();
      assertThat(result)
        .filteredOn(r -> r.getName().startsWith("list-"))
        .extracting(Release::getName)
        .containsExactlyInAnyOrder("list-default", "list-namespace");
    }
  }

  @Nested
  class HelmTest {

    @Nested
    class Valid {

      @Test
      void withValidRelease() {
        helm.install()
          .withKubeConfig(kubeConfig)
          .withName("helm-test")
          .waitReady()
          .call();
        final Release result = Helm.test("helm-test")
          .withKubeConfig(kubeConfig)
          .call();
        assertThat(result)
          .hasFieldOrPropertyWithValue("name", "helm-test")
          .extracting(Release::getOutput).asString()
          .contains(
            "NAME: helm-test\n",
            "LAST DEPLOYED: ",
            "STATUS: deployed",
            "REVISION: 1"
          );
      }
    }

    @Nested
    class Invalid {

      @Test
      void missingRelease() {
        final TestCommand testCommand = Helm.test("i-was-never-created")
          .withKubeConfig(kubeConfig);
        assertThatThrownBy(testCommand::call)
          .message()
          .isEqualTo("release: not found");
      }

      @Test
      void lowTimeout() {
        helm.install().withKubeConfig(kubeConfig).withName("test-low-timeout").call();
        final TestCommand testCommand = Helm.test("test-low-timeout")
          .withKubeConfig(kubeConfig)
          .withTimeout(1);
        assertThatThrownBy(testCommand::call)
          .message()
          .contains("* timed out waiting for the condition");
      }
    }
  }

  @Nested
  class Uninstall {

    @Nested
    class Valid {

      @Test
      void withValidRelease() {
        helm.install().withKubeConfig(kubeConfig).withName("uninstall").call();
        final String out = Helm.uninstall("uninstall").withKubeConfig(kubeConfig).call();
        assertThat(out).contains(
          "release \"uninstall\" uninstalled\n"
        );
      }

      @Test
      void withNamespace() {
        helm.install().withKubeConfig(kubeConfig).withName("uninstall-with-namespace")
          .withNamespace("uninstall-with-namespace").createNamespace().call();
        final String out = Helm.uninstall("uninstall-with-namespace")
          .withKubeConfig(kubeConfig)
          .withNamespace("uninstall-with-namespace")
          .call();
        assertThat(out).contains(
          "release \"uninstall-with-namespace\" uninstalled\n"
        );
      }

      @Test
      void withValidReleaseAndDebug() {
        helm.install().withKubeConfig(kubeConfig).withName("uninstall-with-debug").call();
        final String out = Helm.uninstall("uninstall-with-debug")
          .withKubeConfig(kubeConfig)
          .debug()
          .call();
        assertThat(out).contains(
          "release \"uninstall-with-debug\" uninstalled\n",
          "---\n",
          "uninstall: Deleting uninstall-with-debug",
          "Starting delete for \"uninstall-with-debug-test\" Service\n",
          "Starting delete for \"uninstall-with-debug-test\" Deployment\n",
          "Starting delete for \"uninstall-with-debug-test\" ServiceAccount\n",
          "purge requested for uninstall-with-debug"
        );
      }

      @Test
      void withValidReleaseAndDryRunAndDebug() {
        helm.install().withKubeConfig(kubeConfig).withName("uninstall-with-dry-run").call();
        final String out = Helm.uninstall("uninstall-with-dry-run")
          .withKubeConfig(kubeConfig)
          .dryRun()
          .debug()
          .call();
        assertThat(out)
          .contains(
            "release \"uninstall-with-dry-run\" uninstalled\n"
          )
          .doesNotContain(
            "---\n",
            "uninstall: Deleting uninstall-with-dry-run",
            "Starting delete for",
            "purge requested for"
          );
      }

      @Test
      void withValidReleaseAndNoHooksAndDebug() {
        helm.install().withKubeConfig(kubeConfig).withName("uninstall-with-no-hooks-debug").call();
        final String out = Helm.uninstall("uninstall-with-no-hooks-debug")
          .withKubeConfig(kubeConfig)
          .debug()
          .withCascade(UninstallCommand.Cascade.FOREGROUND)
          .keepHistory()
          .noHooks()
          .call();
        assertThat(out)
          .contains(
            "release \"uninstall-with-no-hooks-debug\" uninstalled\n",
            "---\n",
            "uninstall: Deleting uninstall-with-no-hooks-debug",
            "delete hooks disabled for uninstall-with-no-hooks-debug\n",
            "Starting delete for \"uninstall-with-no-hooks-debug-test\" Service\n",
            "Starting delete for \"uninstall-with-no-hooks-debug-test\" Deployment\n",
            "Starting delete for \"uninstall-with-no-hooks-debug-test\" ServiceAccount\n"
          )
          .doesNotContain(
            "purge requested for"
          );
      }

      @Test
      void missingReleaseWithIgnoreNotFound() {
        final String out = Helm.uninstall("i-was-never-created")
          .withKubeConfig(kubeConfig)
          .ignoreNotFound()
          .call();
        assertThat(out).contains(
          "release \"i-was-never-created\" uninstalled\n"
        );
      }
    }

    @Nested
    class Invalid {

      @Test
      void missingRelease() {
        final UninstallCommand uninstall = Helm.uninstall("i-was-never-created")
          .withKubeConfig(kubeConfig);
        assertThatThrownBy(uninstall::call)
          .message()
          .isEqualTo("uninstall: Release not loaded: i-was-never-created: release: not found");
      }
    }
  }

  @Nested
  class Upgrade {

    @Nested
    class Valid {

      @Test
      void withInstall() {
        final Release result = helm.upgrade()
          .withKubeConfig(kubeConfig)
          .install()
          .withName("upgrade-with-install")
          .call();
        assertThat(result)
          .returns("1", Release::getRevision)
          .returns("deployed", Release::getStatus);
      }

      @Test
      void withPriorInstall() {
        helm.install().withName("upgrade-prior-install").withKubeConfig(kubeConfig).call();
        final Release result = helm.upgrade()
          .withKubeConfig(kubeConfig)
          .withName("upgrade-prior-install")
          .call();
        assertThat(result)
          .returns("2", Release::getRevision)
          .returns("deployed", Release::getStatus);
      }
    }

    @Nested
    class Invalid {
      @Test
      void missingRelease() {
        final UpgradeCommand upgrade = helm.upgrade()
          .withName("upgrade-missing-release")
          .withKubeConfig(kubeConfig);
        assertThatThrownBy(upgrade::call)
          .message()
          .isEqualTo("\"upgrade-missing-release\" has no deployed releases");
      }
    }
  }
}
