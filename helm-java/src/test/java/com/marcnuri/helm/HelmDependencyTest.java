package com.marcnuri.helm;

import org.junit.jupiter.api.BeforeEach;
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
    final DependencyCommand.DependencySubcommand dependencyUpdate = helm.dependency().update();
    assertThatThrownBy(dependencyUpdate::call)
      .message()
      .contains(
        "directory",
        "the-dependency not found"
      );
  }
}
