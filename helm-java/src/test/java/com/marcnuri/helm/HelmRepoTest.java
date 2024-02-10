package com.marcnuri.helm;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
        .withUrl(URI.create("https://charts.helm.sh/stable"))
        .withUsername("not-needed")
        .withPassword("not-needed-pass")
        .insecureSkipTlsVerify()
        .call();
      final List<Repository> result = Helm.repo().list().withRepositoryConfig(config).call();
      assertThat(result).singleElement()
        .extracting(Repository::getName, r -> r.getUrl().toString(), Repository::getUsername, Repository::getPassword, Repository::isInsecureSkipTlsVerify)
        .containsExactly("repo-1", "https://charts.helm.sh/stable", "not-needed", "not-needed-pass", true);
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
          "  - name: stable\n" +
          "    url: https://charts.helm.sh/stable\n" +
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
          tuple("stable", "https://charts.helm.sh/stable", null),
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
}
