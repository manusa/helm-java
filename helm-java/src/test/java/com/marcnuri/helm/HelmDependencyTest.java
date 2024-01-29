package com.marcnuri.helm;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HelmDependencyTest {

  @TempDir
  private Path tempDir;
  private Helm helm;

  @BeforeEach
  void setUp() throws IOException {
    helm = Helm.create().withName("test").withDir(tempDir).call();
    Helm.create().withName("the-dependency").withDir(tempDir).call();
    Files.write(tempDir.resolve("test").resolve("Chart.yaml"),
      ("\ndependencies:\n" +
        "  - name: the-dependency\n" +
        "    version: 0.1.0\n" +
        "    repository: file://../the-dependency\n").getBytes(StandardCharsets.UTF_8),
      StandardOpenOption.APPEND);
  }

  @Nested
  class List {

    @Test
    void withValidDependency() {
      final DependencyListResult result = helm.dependency().list().call();
      assertThat(result)
        .satisfies(r -> assertThat(r.getOutput()).containsPattern("NAME\\s*\tVERSION\\s*\tREPOSITORY\\s*\tSTATUS\\s*\n"))
        .extracting(DependencyListResult::getDependencies)
        .asInstanceOf(InstanceOfAssertFactories.list(DependencyListResult.Dependency.class))
        .singleElement()
        .hasFieldOrPropertyWithValue("name", "the-dependency")
        .hasFieldOrPropertyWithValue("version", "0.1.0")
        .hasFieldOrPropertyWithValue("repository", "file://../the-dependency")
        .hasFieldOrPropertyWithValue("status", "missing");
    }

    @Test
    void withWarning() {
      Helm.create().withName("unlisted").withDir(tempDir.resolve("test").resolve("charts")).call();
      final DependencyListResult result = helm.dependency().list().call();
      assertThat(result)
        .satisfies(r -> assertThat(r.getOutput())
          .containsPattern("NAME\\s*\tVERSION\\s*\tREPOSITORY\\s*\tSTATUS\\s*\n")
          .contains("WARNING:")
          .contains("unlisted\" is not in Chart.yaml.")
        )
        .extracting(DependencyListResult::getWarnings)
        .asInstanceOf(InstanceOfAssertFactories.list(String.class))
        .singleElement(InstanceOfAssertFactories.STRING)
        .endsWith("unlisted\" is not in Chart.yaml.")
        .doesNotStartWith("WARNING");

    }
  }

  @Nested
  class Update {
    @Test
    void withValidDependency() {
      final String result = helm.dependency().update().call();
      assertThat(result)
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
    void withDebug() {
      final String result = helm.dependency().update().debug().call();
      assertThat(result)
        .contains(
          "Repository from local path:",
          "Saving 1 charts",
          "Archiving the-dependency from repo",
          "Deleting outdated charts"
        );
    }

    @Test
    void withMissingDependency() throws IOException {
      Files.walk(tempDir.resolve("the-dependency"))
        .sorted(Comparator.reverseOrder())
        .map(Path::toFile)
        .forEach(File::delete);
      final DependencyCommand.DependencySubcommand<String> dependencyUpdate = helm.dependency().update();
      assertThatThrownBy(dependencyUpdate::call)
        .message()
        .contains(
          "directory",
          "the-dependency not found"
        );
    }
  }
}
