package com.marcnuri.helm;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HelmVersionTest {

  @Test
  void version() {
    final String result = Helm.version().call();
    assertThat(result)
      .isNotBlank()
      .matches("v3\\.\\d+\\.\\d+");
  }
}
