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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the host JVM keeps its OS-level termination signal handlers after native helm
 * calls that used to register Go signal handlers (install path).
 *
 * <p>The Go runtime of the c-shared native library permanently replaced the JVM's SIGINT/SIGTERM
 * handlers on the first {@code signal.Notify}, leaving the JVM unable to react to SIGTERM
 * (shutdown hooks never ran, Kubernetes pods hung until SIGKILL).
 *
 * @author Wellington Mafra
 * @see <a href="https://github.com/manusa/helm-java/issues/409">manusa/helm-java#409</a>
 */
@DisabledOnOs(OS.WINDOWS) // relies on POSIX SIGTERM delivery and 128+n exit codes
class HelmSignalHandlerTest {

  private static final int SIGTERM_EXIT_CODE = 128 + 15;

  @Test
  void jvmExitsOnSigTermAfterInstall() throws Exception {
    final String javaBin = Paths.get(System.getProperty("java.home"), "bin", "java").toString();
    final ProcessBuilder processBuilder = new ProcessBuilder(
      javaBin, "-cp", System.getProperty("java.class.path"), SignalProbe.class.getName());
    processBuilder.environment().put("KUBECONFIG", "/dev/null");
    processBuilder.redirectErrorStream(true);
    final Process probe = processBuilder.start();
    try {
      final BufferedReader reader =
        new BufferedReader(new InputStreamReader(probe.getInputStream(), StandardCharsets.UTF_8));
      boolean ready = false;
      String line;
      while ((line = reader.readLine()) != null) {
        if (SignalProbe.READY_MARKER.equals(line)) {
          ready = true;
          break;
        }
      }
      assertThat(ready)
        .as("probe process should complete the native helm install")
        .isTrue();
      // Process.destroy() delivers SIGTERM on POSIX systems
      probe.destroy();
      assertThat(probe.waitFor(15, TimeUnit.SECONDS))
        .as("JVM should exit on SIGTERM after a native helm install (issue #409)")
        .isTrue();
      assertThat(probe.exitValue()).isEqualTo(SIGTERM_EXIT_CODE);
    } finally {
      probe.destroyForcibly();
    }
  }

  /**
   * Runs in a separate JVM: performs a client-only install (reaching the native code path that
   * used to call Go's {@code signal.Notify}), signals readiness, then waits to be terminated.
   */
  static final class SignalProbe {

    static final String READY_MARKER = "SIGNAL_PROBE_READY";

    public static void main(String[] args) throws Exception {
      final Path tempDir = Files.createTempDirectory("helm-signal-probe");
      final Helm helm = Helm.create().withName("test").withDir(tempDir).call();
      helm.install().clientOnly().withName("test").call();
      System.out.println(READY_MARKER);
      System.out.flush();
      Thread.sleep(TimeUnit.SECONDS.toMillis(60));
      // Only reached if SIGTERM was ignored (the bug) or never delivered (test infrastructure issue)
      System.exit(1);
    }
  }
}
