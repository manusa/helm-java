package com.marcnuri.helm;

import org.junit.jupiter.api.AfterAll;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@EnabledOnOs(OS.LINUX)
class HelmKubernetesTest {

  static K3sContainer k3sContainer;
  static Path kubeConfig;

  private Helm helm;

  @BeforeAll
  static void setUpKubernetes(@TempDir Path tempDir) throws IOException {
    k3sContainer = new K3sContainer(DockerImageName.parse("rancher/k3s:v1.29.0-k3s1"));
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
        final String out = helm.install()
          .withKubeConfig(kubeConfig)
          .withName("test")
          .call();
        assertThat(out).contains(
          "NAME: test\n",
          "LAST DEPLOYED: ",
          "STATUS: deployed",
          "REVISION: 1"
        );
      }

      @Test
      void withDebug() {
        final String out = helm.install()
          .withKubeConfig(kubeConfig)
          .withName("with-debug")
          .debug()
          .call();
        assertThat(out).contains(
          "NAME: with-debug\n",
          "---\n",
          "creating 3 resource(s)"
        );
      }

      @Test
      void withNamespaceAndCreateNamespace() {
        final String out = helm.install()
          .withKubeConfig(kubeConfig)
          .withName("created-namespace")
          .withNamespace("to-be-created")
          .createNamespace()
          .debug().call();
        assertThat(out).contains(
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
}
