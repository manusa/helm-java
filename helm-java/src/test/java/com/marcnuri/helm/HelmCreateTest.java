package com.marcnuri.helm;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class HelmCreateTest {

  @Test
  void valid(@TempDir Path tempDir) {
    Helm.create()
      .withName("test")
      .withDir(tempDir)
      .call();
    assertThat(tempDir).exists()
      .isDirectoryContaining(p -> p.toFile().getName().equals("test"))
      .isDirectoryRecursivelyContaining(p -> p.toFile().getName().equals("Chart.yaml"));
  }

  @Test
  void validWithDuplicateWarning(@TempDir Path tempDir) {
    Helm.create().withName("test").withDir(tempDir).call();
    Helm.create().withName("test").withDir(tempDir).call();
    assertThat(tempDir).exists()
      .isDirectoryContaining(p -> p.toFile().getName().equals("test"))
      .isDirectoryRecursivelyContaining(p -> p.toFile().getName().equals("Chart.yaml"));
  }

  @Test
  void invalid() {
    final CreateCommand create = Helm.create()
      .withName("test")
      .withDir(Paths.get("/im-an-invalid-path"));
    assertThatIllegalStateException().isThrownBy(create::call)
      .withMessage("stat /im-an-invalid-path: no such file or directory");
  }
}
