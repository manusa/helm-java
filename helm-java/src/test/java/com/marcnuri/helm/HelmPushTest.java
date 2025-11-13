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

import com.marcnuri.helm.jni.RepoServerOptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author Marc Nuri
 */
class HelmPushTest {

  @TempDir
  private Path tempDir;
  private Path packagedChart;
  private String remoteServer;
  private String password;

  @BeforeEach
  void setUp() {
    final Path destination = tempDir.resolve("target");
    packagedChart = destination.resolve("test-0.1.0.tgz");
    password = UUID.randomUUID().toString(); // If default password is used, test is flaky ¯\_(ツ)_/¯
    remoteServer = Helm.HelmLibHolder.INSTANCE.RepoOciServerStart(
      new RepoServerOptions(null, null, password)).out;
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
        "basic credential not found");
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
        "basic credential not found");
  }

  @Test
  void pushAuthorized() {
    Helm.registry().login().withHost(remoteServer).withUsername("username").withPassword(password).call();
    final String result = Helm.push()
      .withChart(packagedChart)
      .withRemote(URI.create("oci://" + remoteServer))
      .call();
    assertThat(result)
      .contains("Pushed: ", "test:0.1.0", "Digest: ");
  }

  @Test
  void pushWithDebugShowsDebugMessages() {
    Helm.registry().login().withHost(remoteServer).withUsername("username").withPassword(password).call();
    final String result = Helm.push()
      .withChart(packagedChart)
      .withRemote(URI.create("oci://" + remoteServer))
      .debug()
      .call();
    assertThat(result)
      .contains("Pushed: ", "test:0.1.0", "Digest: ");
  }
}
