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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import com.dajudge.kindcontainer.KindContainer;
import com.dajudge.kindcontainer.KindContainerVersion;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * @author Giuseppe Cardaropoli
 */
public class HelmHistoryTest {

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
    if (kindContainer != null) {
      kindContainer.stop();
    }
  }

  @BeforeEach
  void setUp(@TempDir Path tempDir) {
    helm = Helm.create()
             .withName("test-history")
             .withDir(tempDir)
             .call();
  }

  @Nested
  class Valid {

    @Test
    void afterInstall() {
      helm.install()
        .withKubeConfig(kubeConfigFile)
        .withName("test-history-after-install")
        .call();

      List<ReleaseHistory> releaseHistories = Helm.history("test-history-after-install")
                                                .withKubeConfig(kubeConfigFile)
                                                .call();

      assertThat(releaseHistories).hasSize(1);
      assertThat(releaseHistories.get(0).getRevision()).isEqualTo(1);
      assertThat(releaseHistories.get(0).getDescription()).containsIgnoringCase("Install complete");
    }

    @Test
    void afterUpgrade() {
      helm.install()
        .withKubeConfig(kubeConfigFile)
        .withName("test-history-after-install-and-upgrade")
        .call();

      helm.upgrade()
        .withKubeConfig(kubeConfigFile)
        .withName("test-history-after-install-and-upgrade")
        .set("image.tag", "latest")
        .call();

      List<ReleaseHistory> releaseHistories = Helm.history("test-history-after-install-and-upgrade")
                                                .withKubeConfig(kubeConfigFile)
                                                .call();

      assertThat(releaseHistories).hasSize(2);
      assertThat(releaseHistories.get(0).getDescription()).containsIgnoringCase("Install complete");
      assertThat(releaseHistories.get(1).getDescription()).containsIgnoringCase("Upgrade complete");
    }

    @Test
    void withMax() {
      helm.install()
        .withKubeConfig(kubeConfigFile)
        .withName("test-history-with-max")
        .call();

      helm.upgrade()
        .withKubeConfig(kubeConfigFile)
        .withName("test-history-with-max")
        .set("image.tag", "v1")
        .call();

      helm.upgrade()
        .withKubeConfig(kubeConfigFile)
        .withName("test-history-with-max")
        .set("image.tag", "v2")
        .call();

      List<ReleaseHistory> releaseHistories = Helm.history("test-history-with-max")
                                                .withKubeConfig(kubeConfigFile)
                                                .withMax(2)
                                                .call();

      assertThat(releaseHistories).hasSize(2);
      assertThat(releaseHistories.get(0).getRevision()).isEqualTo(2);
      assertThat(releaseHistories.get(1).getRevision()).isEqualTo(3);
    }

    @Test
    void withNamespace() {
      helm.install()
        .withKubeConfig(kubeConfigFile)
        .withName("test-history-with-namespace")
        .withNamespace("history-namespace")
        .createNamespace()
        .call();

      List<ReleaseHistory> releaseHistories = Helm.history("test-history-with-namespace")
                                                .withKubeConfig(kubeConfigFile)
                                                .withNamespace("history-namespace")
                                                .call();

      assertThat(releaseHistories).hasSize(1);
      assertThat(releaseHistories.get(0).getRevision()).isEqualTo(1);
    }

    @Test
    void withKubeConfigContents() {
      helm.install()
        .withKubeConfig(kubeConfigFile)
        .withName("test-history-with-kube-config-contents")
        .call();

      List<ReleaseHistory> releaseHistories = Helm.history("test-history-with-kube-config-contents")
                                                .withKubeConfigContents(kubeConfigContents)
                                                .call();

      assertThat(releaseHistories).hasSize(1);
      assertThat(releaseHistories.get(0).getRevision()).isEqualTo(1);
    }

  }

  @Nested
  class Invalid {

    @Test
    void nonExistentRelease() {
      assertThatThrownBy(() ->
                           Helm.history("non-existent-release")
                             .withKubeConfig(kubeConfigFile)
                             .call()
      )
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("release: not found");
    }
  }

}
