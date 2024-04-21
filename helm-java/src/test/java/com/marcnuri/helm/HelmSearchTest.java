package com.marcnuri.helm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class HelmSearchTest {

  @Nested
  class SearchRepo {

    @TempDir
    Path tempDir;

    @BeforeEach
    void setRepository() {
      Helm.repo().add().withRepositoryConfig(tempDir.resolve("repositories.yaml"))
        .withName("repo-1")
        .withUrl(URI.create("https://charts.helm.sh/stable"))
        .insecureSkipTlsVerify()
        .call();
    }

    @Test
    void withDefaults() {
      final List<SearchResult> result = Helm.search().repo()
        .withRepositoryConfig(tempDir.resolve("repositories.yaml"))
        .call();
      assertThat(result)
        .isNotEmpty()
        .first()
        .extracting(SearchResult::getName, SearchResult::getScore, SearchResult::getChartVersion, SearchResult::getAppVersion, SearchResult::getDescription)
        .allMatch(s -> s != null && !s.toString().isEmpty());
    }

    @Test
    void withKeyword() {
      final List<SearchResult> result = Helm.search().repo()
        .withRepositoryConfig(tempDir.resolve("repositories.yaml"))
        .withKeyword("nginx")
        .call();
      assertThat(result)
        .isNotEmpty()
        .allMatch(r -> r.getName().contains("nginx") || r.getDescription().contains("nginx") || r.getKeywords().contains("nginx"));
    }

    @Test
    void hasNoDevelVersions() {
      final List<SearchResult> result = Helm.search().repo()
        .withRepositoryConfig(tempDir.resolve("repositories.yaml"))
        .withKeyword("nginx")
        .call();
      assertThat(result)
        .extracting(SearchResult::getChartVersion)
        .noneMatch(chartVersion -> chartVersion.contains("-"));
    }

    @Test
    void withDevelHasDevelVersions() {
      final List<SearchResult> result = Helm.search().repo()
        .withRepositoryConfig(tempDir.resolve("repositories.yaml"))
        .devel()
        .call();
      assertThat(result)
        .extracting(SearchResult::getChartVersion)
        .anyMatch(chartVersion -> chartVersion.contains("-"));
    }

  }
}
