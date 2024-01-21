package com.marcnuri.helm;

import com.marcnuri.helm.jni.Result;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HelmTestServerTest {

  @AfterEach
  void tearDown() {
    Helm.HelmLibHolder.INSTANCE.TestRepoServerStopAll();
  }

  @Test
  void testRepoServerStart() throws Exception {
    final Result result = Helm.HelmLibHolder.INSTANCE.TestRepoServerStart();
    final HttpURLConnection uc = (HttpURLConnection) new URI(result.out).toURL().openConnection();
    uc.setRequestMethod("GET");
    uc.connect();
    assertThat(uc.getResponseCode()).isEqualTo(200);
  }

  @Test
  void testRepoServerStartMultipleTimesReturnsMultipleFunctionalUrls() throws Exception {
    final Result result1 = Helm.HelmLibHolder.INSTANCE.TestRepoServerStart();
    final Result result2 = Helm.HelmLibHolder.INSTANCE.TestRepoServerStart();
    for (Result result : new Result[]{result1, result2}) {
      final HttpURLConnection uc = (HttpURLConnection) new URI(result.out).toURL().openConnection();
      uc.setRequestMethod("GET");
      uc.connect();
      assertThat(uc.getResponseCode()).isEqualTo(200);
    }
  }

  @Test
  void testRepoServerStopAllStopsAllInstances() throws Exception {
    final Result result1 = Helm.HelmLibHolder.INSTANCE.TestRepoServerStart();
    final Result result2 = Helm.HelmLibHolder.INSTANCE.TestRepoServerStart();
    Helm.HelmLibHolder.INSTANCE.TestRepoServerStopAll();
    for (Result result : new Result[]{result1, result2}) {
      final HttpURLConnection uc = (HttpURLConnection) new URI(result.out).toURL().openConnection();
      uc.setRequestMethod("GET");
      assertThatThrownBy(uc::connect).isInstanceOf(ConnectException.class);
    }
  }

}
