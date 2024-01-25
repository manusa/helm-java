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
class HelmInstallKubernetesTest {

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
