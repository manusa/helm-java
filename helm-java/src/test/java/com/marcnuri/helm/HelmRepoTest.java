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
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/**
 * @author Marc Nuri
 */
class HelmRepoTest {

  @Nested
  class RepoAdd {
    @TempDir
    Path tempDir;

    @Test
    void withValidRepo() {
      final Path config = tempDir.resolve("repositories.yaml");
      Helm.repo().add().withRepositoryConfig(config)
        .withName("repo-1")
        .withUrl(URI.create("http://charts.gitlab.io"))
        .withUsername("not-needed")
        .withPassword("not-needed-pass")
        .insecureSkipTlsVerify()
        .call();
      final List<Repository> result = Helm.repo().list().withRepositoryConfig(config).call();
      assertThat(result).singleElement()
        .extracting(Repository::getName, r -> r.getUrl().toString(), Repository::getUsername, Repository::getPassword, Repository::isInsecureSkipTlsVerify)
        .containsExactly("repo-1", "http://charts.gitlab.io", "not-needed", "not-needed-pass", true);
    }

    @Test
    void withInvalidRepo() {
      final RepoCommand.RepoSubcommand<Void> callable = Helm.repo()
        .add()
        .withRepositoryConfig(tempDir.resolve("repositories.yaml"))
        .withName("invalid-repo")
        .withUrl(URI.create("https://localhost/stable"));
      assertThatThrownBy(callable::call)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("looks like \"https://localhost/stable\" is not a valid chart repository or cannot be reached:");
    }
  }

  @Nested
  class RepoList {
    @Test
    void withMissingPath() {
      final RepoCommand.WithRepositoryConfig<List<Repository>> callable = Helm.repo()
        .list().withRepositoryConfig(Paths.get("i-dont-exist"));
      assertThatThrownBy(callable::call)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("couldn't load repositories file")
        .hasMessageContaining("i-dont-exist");
    }

    @Test
    void withValidConfig(@TempDir Path tempDir) throws Exception {
      final Path repositoryConfig = Files.write(tempDir.resolve("repositories.yaml"),
        ("repositories:\n" +
          "  - name: repo-1\n" +
          "    url: https://charts.example.com/repo-1?i=31&test\n" +
          "    username: user-name\n" +
          "  - name: valid-repo\n" +
          "    url: http://charts.gitlab.io\n" +
          "  - name: other\n" +
          "    url: https://charts.example.sh/other\n" +
          "    ignored: field"
        ).getBytes(StandardCharsets.UTF_8),
        StandardOpenOption.CREATE);
      final List<Repository> result = Helm.repo().list().withRepositoryConfig(repositoryConfig).call();
      assertThat(result)
        .extracting(Repository::getName, r -> r.getUrl().toString(), Repository::getUsername)
        .containsExactly(
          tuple("repo-1", "https://charts.example.com/repo-1?i=31&test", "user-name"),
          tuple("valid-repo", "http://charts.gitlab.io", null),
          tuple("other", "https://charts.example.sh/other", null)
        );
    }

    @Test
    void withEmptyConfig(@TempDir Path tempDir) throws Exception {
      final Path repositoryConfig = Files.write(tempDir.resolve("repositories.yaml"),
        new byte[0], StandardOpenOption.CREATE);
      final List<Repository> result = Helm.repo().list().withRepositoryConfig(repositoryConfig).call();
      assertThat(result).isEmpty();
    }
  }

  @Nested
  class RepoRemove {

    Path repositoryConfig;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws IOException {
      repositoryConfig = tempDir.resolve("repositories.yaml");
      Files.write(repositoryConfig,
        ("repositories:\n" +
          "  - name: repo-1\n" +
          "    url: https://charts.example.com/repo-1?i=31&test\n" +
          "    username: user-name\n" +
          "  - name: valid-repo\n" +
          "    url: http://charts.gitlab.io\n" +
          "  - name: other\n" +
          "    url: https://charts.example.sh/other\n" +
          "    ignored: field"
        ).getBytes(StandardCharsets.UTF_8),
        StandardOpenOption.CREATE);
    }

    @Test
    void withValidRepos() {
      Helm.repo().remove()
        .withRepositoryConfig(repositoryConfig)
        .withRepo("repo-1")
        .withRepo("valid-repo")
        .call();
      final List<Repository> result = Helm.repo().list().withRepositoryConfig(repositoryConfig).call();
      assertThat(result)
        .extracting(Repository::getName)
        .containsExactly("other");
    }

    @Test
    void withNoRepos() {
      Helm.repo().remove()
        .withRepositoryConfig(repositoryConfig)
        .call();
      final List<Repository> result = Helm.repo().list().withRepositoryConfig(repositoryConfig).call();
      assertThat(result)
        .extracting(Repository::getName)
        .containsExactly("repo-1", "valid-repo", "other");
    }

    @Test
    void withMissingRepo() {
      final RepoCommand.WithRepo<Void> callable = Helm.repo().remove()
        .withRepositoryConfig(repositoryConfig)
        .withRepo("missing");
      assertThatThrownBy(callable::call)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("no repo named \"missing\" found");
    }

  }

  @Nested
  class RepoUpdate {

    Path repositoryConfig;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
      repositoryConfig = tempDir.resolve("repositories.yaml");
    }

    @Test
    void updateAllWithValidRepos() throws IOException {
      Files.write(repositoryConfig,
        ("repositories:\n" +
          "  - name: valid-repo\n" +
          "    url: http://charts.gitlab.io\n"
        ).getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
      final List<Repository> updated = Helm.repo().update().withRepositoryConfig(repositoryConfig).call();
      assertThat(updated)
        .singleElement().hasFieldOrPropertyWithValue("name", "valid-repo");
    }

    @Test
    void updateAllWithValidAndInvalidRepos() throws IOException {
      Files.write(repositoryConfig,
        ("repositories:\n" +
          "  - name: repo-1\n" +
          "    url: https://charts.example.com/repo-1?i=31&test\n" +
          "    username: user-name\n" +
          "  - name: valid-repo\n" +
          "    url: http://charts.gitlab.io\n"
        ).getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
      final RepoCommand.WithRepositoryConfig<List<Repository>> callable = Helm.repo().update()
        .withRepositoryConfig(repositoryConfig);
      assertThatThrownBy(callable::call)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("ailed to update the following repositories:")
        .hasMessageContaining("[https://charts.example.com/repo-1?i=31&test]");
    }

    @Test
    void updateByNameWithValidAndInvalidRepos() throws IOException {
      Files.write(repositoryConfig,
        ("repositories:\n" +
          "  - name: repo-1\n" +
          "    url: https://charts.example.com/repo-1?i=31&test\n" +
          "    username: user-name\n" +
          "  - name: valid-repo\n" +
          "    url: http://charts.gitlab.io\n"
        ).getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
      final RepoCommand.WithRepositoryConfig<List<Repository>> callable = Helm.repo().update()
        .withRepositoryConfig(repositoryConfig)
        .withRepo("valid-repo")
        .withRepo("repo-1");
      assertThatThrownBy(callable::call)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("ailed to update the following repositories:")
        .hasMessageContaining("[https://charts.example.com/repo-1?i=31&test]");
    }

    @Test
    void updateByNameWithValidAndInvalidReposOnlyValid() throws IOException {
      Files.write(repositoryConfig,
        ("repositories:\n" +
          "  - name: repo-1\n" +
          "    url: https://charts.example.com/repo-1?i=31&test\n" +
          "  - name: valid-repo\n" +
          "    url: http://charts.gitlab.io\n"
        ).getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
      final List<Repository> updated = Helm.repo().update()
        .withRepositoryConfig(repositoryConfig)
        .withRepo("valid-repo")
        .call();
      assertThat(updated)
        .singleElement().hasFieldOrPropertyWithValue("name", "valid-repo");
    }

    @Test
    void updateByNameWithMissingRepo() throws IOException {
      Files.write(repositoryConfig,
        ("repositories:\n" +
          "  - name: repo-1\n" +
          "    url: https://charts.example.com/repo-1?i=31&test\n" +
          "  - name: valid-repo\n" +
          "    url: http://charts.gitlab.io\n"
        ).getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
      final List<Repository> updated = Helm.repo().update()
        .withRepositoryConfig(repositoryConfig)
        .withRepo("not-there")
        .call();
      assertThat(updated).isEmpty();
    }

  }
}
