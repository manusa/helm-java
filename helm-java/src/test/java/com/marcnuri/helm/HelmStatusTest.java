/*
 * Copyright 2026 Marc Nuri
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

import com.dajudge.kindcontainer.KindContainer;
import com.dajudge.kindcontainer.KindContainerVersion;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
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

/**
 * Tests for the helm status command.
 *
 * @author Marc Nuri
 */
@EnabledOnOs(OS.LINUX)
class HelmStatusTest {

  static KindContainer<?> kindContainer;
  static String kubeConfigContents;
  static Path kubeConfigFile;

  private Helm helm;

  @BeforeAll
  static void setUpKubernetes(@TempDir Path tempDir) throws IOException {
    kindContainer = new KindContainer<>(KindContainerVersion.VERSION_1_31_0);
    kindContainer.start();
    kubeConfigContents = kindContainer.getKubeconfig();
    kubeConfigFile = tempDir.resolve("config.yaml");
    Files.write(kubeConfigFile, kubeConfigContents.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
  }

  @AfterAll
  static void tearDownKubernetes() {
    kindContainer.stop();
  }

  @BeforeEach
  void setUp(@TempDir Path tempDir) {
    helm = Helm.create().withName("test").withDir(tempDir).call();
  }

  @Nested
  class Valid {

    @Test
    void withName() {
      helm.install()
        .withKubeConfig(kubeConfigFile)
        .withName("status-with-name")
        .call();
      final Release result = Helm.status("status-with-name")
        .withKubeConfig(kubeConfigFile)
        .call();
      assertThat(result)
        .returns("status-with-name", Release::getName)
        .returns("deployed", Release::getStatus)
        .returns("1", Release::getRevision)
        .satisfies(r -> assertThat(r.getLastDeployed()).matches(d -> d.toLocalDate().equals(LocalDate.now())))
        .returns("test-0.1.0", Release::getChart)
        .returns("1.16.0", Release::getAppVersion)
        .extracting(Release::getOutput).asString()
        .contains(
          "NAME: status-with-name\n",
          "LAST DEPLOYED: ",
          "STATUS: deployed",
          "REVISION: 1",
          "DESCRIPTION: Install complete"
        );
    }

    @Test
    void withNamespace() {
      helm.install()
        .withKubeConfig(kubeConfigFile)
        .withName("status-with-namespace")
        .withNamespace("status-namespace")
        .createNamespace()
        .call();
      final Release result = Helm.status("status-with-namespace")
        .withKubeConfig(kubeConfigFile)
        .withNamespace("status-namespace")
        .call();
      assertThat(result)
        .returns("status-with-namespace", Release::getName)
        .returns("status-namespace", Release::getNamespace)
        .returns("deployed", Release::getStatus);
    }

    @Test
    void withRevision() {
      helm.install()
        .withKubeConfig(kubeConfigFile)
        .withName("status-with-revision")
        .set("replicaCount", "1")
        .call();
      helm.upgrade()
        .withKubeConfig(kubeConfigFile)
        .withName("status-with-revision")
        .set("replicaCount", "2")
        .call();
      final Release result = Helm.status("status-with-revision")
        .withKubeConfig(kubeConfigFile)
        .withRevision(1)
        .call();
      assertThat(result)
        .returns("status-with-revision", Release::getName)
        .returns("1", Release::getRevision)
        .returns("superseded", Release::getStatus);
    }

    @Test
    void withKubeConfigContents() {
      helm.install()
        .withKubeConfigContents(kubeConfigContents)
        .withName("status-kube-config-contents")
        .call();
      final Release result = Helm.status("status-kube-config-contents")
        .withKubeConfigContents(kubeConfigContents)
        .call();
      assertThat(result)
        .returns("status-kube-config-contents", Release::getName)
        .returns("deployed", Release::getStatus);
    }

    @Test
    void afterUpgrade() {
      helm.install()
        .withKubeConfig(kubeConfigFile)
        .withName("status-after-upgrade")
        .call();
      helm.upgrade()
        .withKubeConfig(kubeConfigFile)
        .withName("status-after-upgrade")
        .call();
      final Release result = Helm.status("status-after-upgrade")
        .withKubeConfig(kubeConfigFile)
        .call();
      assertThat(result)
        .returns("status-after-upgrade", Release::getName)
        .returns("2", Release::getRevision)
        .returns("deployed", Release::getStatus)
        .extracting(Release::getOutput).asString()
        .contains("DESCRIPTION: Upgrade complete");
    }
  }

  @Nested
  class Invalid {

    @Test
    void nonExistentRelease() {
      final StatusCommand status = Helm.status("non-existent-release")
        .withKubeConfig(kubeConfigFile);
      assertThatThrownBy(status::call)
        .message()
        .contains("not found");
    }

    @Test
    void invalidRevision() {
      helm.install()
        .withKubeConfig(kubeConfigFile)
        .withName("status-invalid-revision")
        .call();
      final StatusCommand status = Helm.status("status-invalid-revision")
        .withKubeConfig(kubeConfigFile)
        .withRevision(999);
      assertThatThrownBy(status::call)
        .message()
        .contains("not found");
    }
  }
}
