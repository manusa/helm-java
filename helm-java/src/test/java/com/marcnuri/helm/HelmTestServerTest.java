package com.marcnuri.helm;

import com.marcnuri.helm.jni.RepoServerOptions;
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
    Helm.HelmLibHolder.INSTANCE.RepoServerStopAll();
  }

  @Test
  void testRepoServerStart() throws Exception {
    final Result result = Helm.HelmLibHolder.INSTANCE.RepoServerStart(new RepoServerOptions());
    final HttpURLConnection uc = (HttpURLConnection) new URI(result.out).toURL().openConnection();
    uc.setRequestMethod("GET");
    uc.connect();
    assertThat(uc.getResponseCode()).isEqualTo(200);
  }

  @Test
  void testRepoServerStartMultipleTimesReturnsMultipleFunctionalUrls() throws Exception {
    final Result result1 = Helm.HelmLibHolder.INSTANCE.RepoServerStart(new RepoServerOptions());
    final Result result2 = Helm.HelmLibHolder.INSTANCE.RepoServerStart(new RepoServerOptions());
    for (Result result : new Result[]{result1, result2}) {
      final HttpURLConnection uc = (HttpURLConnection) new URI(result.out).toURL().openConnection();
      uc.setRequestMethod("GET");
      uc.connect();
      assertThat(uc.getResponseCode()).isEqualTo(200);
    }
  }

  @Test
  void testRepoServerStopAllStopsAllInstances() throws Exception {
    final Result result1 = Helm.HelmLibHolder.INSTANCE.RepoServerStart(new RepoServerOptions());
    final Result result2 = Helm.HelmLibHolder.INSTANCE.RepoServerStart(new RepoServerOptions());
    Helm.HelmLibHolder.INSTANCE.RepoServerStopAll();
    for (Result result : new Result[]{result1, result2}) {
      final HttpURLConnection uc = (HttpURLConnection) new URI(result.out).toURL().openConnection();
      uc.setRequestMethod("GET");
      assertThatThrownBy(uc::connect).isInstanceOf(ConnectException.class);
    }
  }

}
