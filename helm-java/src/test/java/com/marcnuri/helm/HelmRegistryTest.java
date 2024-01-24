package com.marcnuri.helm;

import com.marcnuri.helm.jni.RepoServerOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        .startsWith("Login Succeeded")
        .contains("level=info msg=\"authorized request\"");
    }

    @Test
    void withInvalidCredentialsFails() {
      final RegistryCommand.LoginCommand loginCommand = Helm.registry().login()
        .withHost(remoteServer).withUsername("username").withPassword("invalid");
      assertThatThrownBy(loginCommand::call)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContainingAll(
          "login attempt to",
          "failed with status: 401 Unauthorized"
        );
    }

    @Test
    void withDebugAndInvalidCredentialsFails() {
      final RegistryCommand.LoginCommand loginCommand = Helm.registry().login()
        .debug()
        .withHost(remoteServer).withUsername("username").withPassword("invalid");
      assertThatThrownBy(loginCommand::call)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContainingAll(
          "login attempt to",
          "failed with status: 401 Unauthorized",
          "level=info msg=\"Error logging in to endpoint, trying next endpoint\""
        );
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
      assertThatThrownBy(logoutCommand::call)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("not logged in");
    }

    @Test
    void withDebugAndNoPreviousLoginThrowsException() {
      final RegistryCommand.LogoutCommand logoutCommand = Helm.registry().logout()
        .debug()
        .withHost(remoteServer);
      assertThatThrownBy(logoutCommand::call)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("not logged in");
    }
  }
}
