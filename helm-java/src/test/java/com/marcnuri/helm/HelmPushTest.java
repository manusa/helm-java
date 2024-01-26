package com.marcnuri.helm;

import com.marcnuri.helm.jni.RepoServerOptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class HelmPushTest {

  @TempDir
  private Path tempDir;
  private Path packagedChart;
  private String remoteServer;

  @BeforeEach
  void setUp() {
    final Path destination = tempDir.resolve("target");
    packagedChart = destination.resolve("test-0.1.0.tgz");
    remoteServer = Helm.HelmLibHolder.INSTANCE.RepoOciServerStart(
      new RepoServerOptions(null, null, "not-known" // If default password is used, test is flaky ¯\_(ツ)_/¯
      )).out;
    Helm
      .create().withName("test").withDir(tempDir).call()
      .packageIt().withDestination(destination).call();
  }

  @AfterEach
  void tearDown() {
    Helm.HelmLibHolder.INSTANCE.RepoServerStop(remoteServer);
  }

  @Test
  void pushUnauthorizedThrowsException() {
    final PushCommand pushCommand = Helm.push()
      .withChart(packagedChart)
      .withRemote(URI.create("oci://" + remoteServer));
    assertThatIllegalStateException()
      .isThrownBy(pushCommand::call)
      .extracting(IllegalStateException::getMessage)
      .asString()
      .containsAnyOf(
        "push access denied, repository does not exist or may require authorization: authorization failed: no basic auth credentials",
        "unexpected status from HEAD request to");
  }

  @Test
  void pushUnauthorizedWithDebugThrowsExceptionWithDetail() {
    final PushCommand pushCommand = Helm.push()
      .withChart(packagedChart)
      .withRemote(URI.create("oci://" + remoteServer))
      .debug();
    assertThatIllegalStateException()
      .isThrownBy(pushCommand::call)
      .extracting(IllegalStateException::getMessage)
      .asString()
      .containsAnyOf(
        "push access denied, repository does not exist or may require authorization: authorization failed: no basic auth credentials",
        "unexpected status from HEAD request to")
      .contains(
        "time=",
        "response.status",
        "401 Unauthorized");
  }

  @Test
  void pushAuthorized() {
    Helm.registry().login().withHost(remoteServer).withUsername("username").withPassword("not-known").call();
    final String result = Helm.push()
      .withChart(packagedChart)
      .withRemote(URI.create("oci://" + remoteServer))
      .call();
    assertThat(result)
      .contains("Pushed: ", "test:0.1.0", "Digest: ");
  }

  @Test
  void pushWithDebugShowsDebugMessages() {
    Helm.registry().login().withHost(remoteServer).withUsername("username").withPassword("not-known").call();
    final String result = Helm.push()
      .withChart(packagedChart)
      .withRemote(URI.create("oci://" + remoteServer))
      .debug()
      .call();
    assertThat(result)
      .contains("time=", "checking and pushing to", "Pushed: ", "test:0.1.0", "Digest: ");
  }
}
