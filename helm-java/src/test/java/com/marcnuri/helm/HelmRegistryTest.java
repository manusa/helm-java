package com.marcnuri.helm;

import com.marcnuri.helm.jni.RepoServerOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HelmRegistryTest {

  private String remoteServer;

  @BeforeEach
  void setUp() {
    remoteServer = Helm.HelmLibHolder.INSTANCE.RepoOciServerStart(new RepoServerOptions()).out;
  }

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
