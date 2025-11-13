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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Marc Nuri
 */
class HelmRegistryTest {

  private String remoteServer;

  @BeforeEach
  void setUp() {
    remoteServer = Helm.HelmLibHolder.INSTANCE.RepoOciServerStart(new RepoServerOptions()).out;
  }

  @Nested
  class Login {
    @Test
    void withValidCredentialsSucceeds() {
      final String result = Helm.registry().login()
        .withHost(remoteServer).withUsername("username").withPassword("password").call();
      assertThat(result).startsWith("Login Succeeded");
    }

    @Test
    void withDebugAndValidCredentialsSucceeds() {
      final String result = Helm.registry().login()
        .debug()
        .withHost(remoteServer).withUsername("username").withPassword("password").call();
      assertThat(result)
        .containsPattern(Pattern.compile("^Login Succeeded$", Pattern.MULTILINE))
        .contains("level=info msg=\"authorized request\"");
    }

    @Test
    void withInvalidCredentialsFails() {
      final RegistryCommand.LoginCommand loginCommand = Helm.registry().login()
        .withHost(remoteServer).withUsername("username").withPassword("invalid");
      assertThatThrownBy(loginCommand::call)
        .isInstanceOf(IllegalStateException.class)
        .extracting(Throwable::getMessage)
        .asString()
        .containsAnyOf(
          "login attempt to",
          "authenticating to");
    }

    @Test
    void withDebugAndInvalidCredentialsFails() {
      final RegistryCommand.LoginCommand loginCommand = Helm.registry().login()
        .debug()
        .withHost(remoteServer).withUsername("username").withPassword("invalid");
      assertThatThrownBy(loginCommand::call)
        .isInstanceOf(IllegalStateException.class)
        .extracting(Throwable::getMessage)
        .asString()
        .containsAnyOf(
          "login attempt to",
          "authenticating to");
    }
  }

  @Nested
  class Logout {

    @Test
    void withPreviousLoginSucceeds() {
      Helm.registry().login().withHost(remoteServer).withUsername("username").withPassword("password").call();
      final String result = Helm.registry().logout()
        .withHost(remoteServer).call();
      assertThat(result).startsWith("Removing login credentials for " + remoteServer);
    }

    @Test
    void withDebugAndPreviousLoginSucceeds() {
      Helm.registry().login().withHost(remoteServer).withUsername("username").withPassword("password").call();
      final String result = Helm.registry().logout()
        .debug()
        .withHost(remoteServer).call();
      assertThat(result).startsWith("Removing login credentials for " + remoteServer);
    }

    @Test
    void withNoPreviousLoginThrowsException() {
      final RegistryCommand.LogoutCommand logoutCommand = Helm.registry().logout()
        .withHost(remoteServer);
      // In Helm 3.19.2, logout no longer throws an exception when not logged in
      final String result = logoutCommand.call();
      assertThat(result).contains("Removing login credentials for " + remoteServer);
    }

    @Test
    void withDebugAndNoPreviousLoginThrowsException() {
      final RegistryCommand.LogoutCommand logoutCommand = Helm.registry().logout()
        .debug()
        .withHost(remoteServer);
      // In Helm 3.19.2, logout no longer throws an exception when not logged in
      final String result = logoutCommand.call();
      assertThat(result).contains("Removing login credentials for " + remoteServer);
    }
  }
}
