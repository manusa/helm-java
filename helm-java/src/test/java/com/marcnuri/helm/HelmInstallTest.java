package com.marcnuri.helm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HelmInstallTest {

  @TempDir
  private Path tempDir;
  private Helm helm;

  @BeforeEach
  void setUp() {
    helm = Helm.create().withName("test").withDir(tempDir).call();
  }

  @Nested
  class Valid {

    @Test
    void withName() {
      final String out = helm.install()
        .clientOnly()
        .withName("test")
        .call();
      assertThat(out).contains(
        "NAME: test\n",
        "LAST DEPLOYED: ",
        "STATUS: deployed",
        "REVISION: 1",
        "NOTES:"
      );
    }

    @Test
    void withPackagedChart(@TempDir Path destination) {
      helm.packageIt().withDestination(destination).call();
      final String out = Helm.install(destination.resolve("test-0.1.0.tgz").toFile().getAbsolutePath())
        .clientOnly()
        .withName("test")
        .call();
      assertThat(out).contains("NAME: test");
    }

    @Test
    void withGenerateName() {
      final String out = helm.install()
        .clientOnly()
        .withName("test") // Should be ignored (omitted/not failure)
        .generateName()
        .call();
      assertThat(out).contains(
        "NAME: test-",
        "STATUS: deployed"
      );
    }

    @Test
    void withGenerateNameAndNameTemplate() {
      final String out = helm.install()
        .clientOnly()
        .generateName()
        .withNameTemplate("a-chart-{{randAlpha 6 | lower}}")
        .call();
      assertThat(out).contains(
        "NAME: a-chart-",
        "STATUS: deployed"
      );
    }

    @Test
    void withNamespace() {
      final String out = helm.install()
        .clientOnly()
        .withName("test")
        .withNamespace("test-namespace")
        .call();
      assertThat(out).contains(
        "NAME: test\n",
        "NAMESPACE: test-namespace"
      );
    }

    @Test
    void withDryRun() {
      final String out = helm.install()
        .clientOnly()
        .withName("test")
        .dryRun()
        .withDryRunOption("client")
        .call();
      assertThat(out).contains(
        "NAME: test",
        "STATUS: pending-install"
      );
    }

    @Test
    void withValues() {
      final String out = helm.install()
        .clientOnly()
        .debug()
        .withName("test")
        .set("corner", "\"'\\={[,.]}!?-_test=1,other=2")
        .set("bool", "true")
        .set("int", "1")
        .set("float", "1.1")
        .call();
      assertThat(out).contains(
        "NAME: test\n",
        "USER-SUPPLIED VALUES:\n",
        "corner: '\"''\\={[,.]}!?-_test=1,other=2'\n",
        "bool: true\n",
        "int: 1\n",
        "float: \"1.1\"" // helm.sh/helm/v3/pkg/strvals does not support floats
      );
    }
  }

  @Nested
  class Invalid {

    @Test
    void withMissingChart() {
      final InstallCommand install = helm.install().clientOnly().withName("test").withChart(null);
      assertThatThrownBy(install::call)
        .message()
        .containsAnyOf(
          "no such file or directory",
          "The system cannot find the path specified"
        );
    }

    @Test
    void withMissingName() {
      final InstallCommand install = helm.install().clientOnly();
      assertThatThrownBy(install::call)
        .hasMessage("release name \"\": no name provided");
    }

//    @Test
//    void withDevelopmentVersionInChart() throws IOException {
//      final Path chartYaml = tempDir.resolve("test").resolve("Chart.yaml");
//      final String chart = new String(Files.readAllBytes(chartYaml), StandardCharsets.UTF_8);
//      Files.write(chartYaml, chart.replace("version: 0.1.0", "version: 0.1.0-SNAPSHOT").getBytes(StandardCharsets.UTF_8));
//      final InstallCommand install = helm.install()
//        .clientOnly()
//        .withName("development-version");
//      assertThatThrownBy(install::call)
//        .hasMessage("release name \"\": no name provided");
//    }
  }


}
