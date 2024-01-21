package com.marcnuri.helm;

import com.marcnuri.helm.jni.Result;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.HttpURLConnection;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class HelmTestServerTest {

  @AfterEach
  void tearDown() {
    Helm.HelmLibHolder.INSTANCE.TestServerStop();
  }

  @Test
  void testServerStart() throws Exception {
    final Result result = Helm.HelmLibHolder.INSTANCE.TestServerStart();
    final HttpURLConnection uc = (HttpURLConnection) new URI(result.out).toURL().openConnection();
    uc.setRequestMethod("GET");
    uc.connect();
    assertThat(uc.getResponseCode()).isEqualTo(200);
  }

  @Test
  void testServerStartMultipleTimesReturnsError() {
    Helm.HelmLibHolder.INSTANCE.TestServerStart();
    final Result result = Helm.HelmLibHolder.INSTANCE.TestServerStart();
    assertThat(result.err).isEqualTo("server already started, only one instance allowed");
  }

  @Test
  void testServerStartMultipleTimesReturnsStartedServerInfo() throws Exception {
    Helm.HelmLibHolder.INSTANCE.TestServerStart();
    final Result result = Helm.HelmLibHolder.INSTANCE.TestServerStart();
    final HttpURLConnection uc = (HttpURLConnection) new URI(result.out).toURL().openConnection();
    uc.setRequestMethod("GET");
    uc.connect();
    assertThat(uc.getResponseCode()).isEqualTo(200);
  }
}
