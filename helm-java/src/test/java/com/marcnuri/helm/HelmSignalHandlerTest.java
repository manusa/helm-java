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
import com.marcnuri.helm.jni.Result;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the host JVM keeps its OS-level termination signal handlers after native helm
 * calls that used to register Go signal handlers.
 *
 * <p>The Go runtime of the c-shared native library permanently replaced the JVM's SIGINT/SIGTERM
 * handlers on the first {@code signal.Notify}, leaving the JVM unable to react to SIGTERM
 * (shutdown hooks never ran, Kubernetes pods hung until SIGKILL).
 *
 * @author Wellington Mafra
 * @author Marc Nuri
 * @see <a href="https://github.com/manusa/helm-java/issues/409">manusa/helm-java#409</a>
 */
@DisabledOnOs(OS.WINDOWS) // relies on POSIX SIGTERM delivery and 128+n exit codes
// SEPARATE_THREAD is defensive: no blocking read is left, but it still bounds a call that ignores interrupts
@Timeout(value = 2, unit = TimeUnit.MINUTES, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class HelmSignalHandlerTest {

  private static final int SIGTERM_EXIT_CODE = 128 + 15;

  private volatile Process probe;

  @AfterEach
  void destroyProbe() throws InterruptedException {
    // Runs even if the test failed or timed out, so no probe process is left behind
    if (probe != null) {
      probe.destroyForcibly().waitFor(30, TimeUnit.SECONDS);
    }
  }

  @Test
  void jvmRunsShutdownHooksOnSigTermAfterInstall(@TempDir Path tempDir) throws Exception {
    assertProbeExitsGracefullyOnSigTerm(SignalProbe.INSTALL, tempDir);
  }

  @Test
  void jvmRunsShutdownHooksOnSigTermAfterTemplate(@TempDir Path tempDir) throws Exception {
    assertProbeExitsGracefullyOnSigTerm(SignalProbe.TEMPLATE, tempDir);
  }

  @Test
  void jvmRunsShutdownHooksOnSigTermAfterTestOciServerStart(@TempDir Path tempDir) throws Exception {
    assertProbeExitsGracefullyOnSigTerm(SignalProbe.TEST_OCI_SERVER, tempDir);
  }

  private void assertProbeExitsGracefullyOnSigTerm(String scenario, Path tempDir) throws Exception {
    final String javaBin = Paths.get(System.getProperty("java.home"), "bin", "java").toString();
    final ProcessBuilder processBuilder = new ProcessBuilder(
      javaBin,
      // Extract the native library into the test's temporary directory
      "-Dcom.marcnuri.jkube-helm.tempDir=" + tempDir,
      "-cp", System.getProperty("java.class.path"),
      SignalProbe.class.getName(), scenario, tempDir.toString());
    processBuilder.environment().put("KUBECONFIG", "/dev/null");
    // Go's os.TempDir() (e.g. the test OCI server's storage), so nothing is left behind
    processBuilder.environment().put("TMPDIR", tempDir.toString());
    // A file, not a pipe: Process.destroy() closes the pipes, which would lose the output after SIGTERM
    final Path outputFile = tempDir.resolve("probe-output.log");
    processBuilder.redirectErrorStream(true).redirectOutput(outputFile.toFile());
    probe = processBuilder.start();
    final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(90);
    while (!read(outputFile).contains(SignalProbe.READY_MARKER) && probe.isAlive() && System.nanoTime() < deadline) {
      Thread.sleep(100);
    }
    assertThat(read(outputFile))
      .as("probe process should complete the native helm %s call", scenario)
      .contains(SignalProbe.READY_MARKER);
    // Process.destroy() delivers SIGTERM on POSIX systems
    probe.destroy();
    assertThat(probe.waitFor(15, TimeUnit.SECONDS))
      .as("JVM should exit on SIGTERM after a native helm %s call (issue #409), output:%n%s",
        scenario, read(outputFile))
      .isTrue();
    assertThat(read(outputFile))
      .as("JVM should run its shutdown hooks on SIGTERM after a native helm %s call (issue #409)", scenario)
      .contains(SignalProbe.SHUTDOWN_HOOK_MARKER);
    assertThat(probe.exitValue())
      .as("JVM exit code on SIGTERM after a native helm %s call, output:%n%s", scenario, read(outputFile))
      .isEqualTo(SIGTERM_EXIT_CODE);
  }

  private static String read(Path file) throws IOException {
    return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
  }

  /**
   * Runs in a separate JVM: performs a native helm call that used to register Go signal handlers,
   * registers a shutdown hook, signals readiness, then waits to be terminated.
   *
   * <p>Arguments: the scenario and a temporary directory to work in.
   */
  static final class SignalProbe {

    static final String READY_MARKER = "SIGNAL_PROBE_READY";
    static final String SHUTDOWN_HOOK_MARKER = "SIGNAL_PROBE_SHUTDOWN_HOOK_RAN";
    static final String INSTALL = "install";
    static final String TEMPLATE = "template";
    static final String TEST_OCI_SERVER = "test-oci-server";

    public static void main(String[] args) throws Exception {
      final String scenario = args[0];
      final Path tempDir = Paths.get(args[1]);
      switch (scenario) {
        case INSTALL:
          Helm.create().withName("test").withDir(tempDir).call().install().clientOnly().withName("test").call();
          break;
        case TEMPLATE:
          Helm.create().withName("test").withDir(tempDir).call().template().call();
          break;
        case TEST_OCI_SERVER:
          startTestOciServer();
          break;
        default:
          throw new IllegalArgumentException("Unknown scenario: " + scenario);
      }
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        System.out.println(SHUTDOWN_HOOK_MARKER);
        System.out.flush();
      }));
      System.out.println(READY_MARKER);
      System.out.flush();
      Thread.sleep(TimeUnit.SECONDS.toMillis(60));
      // Only reached if SIGTERM was ignored (the bug) or never delivered (test infrastructure issue)
      System.exit(1);
    }

    /**
     * Starts the test OCI registry and waits until it serves requests. The registry starts serving
     * in a goroutine, after registering any signal handlers, so a response proves they are in place.
     */
    private static void startTestOciServer() throws Exception {
      final Result result = Helm.HelmLibHolder.INSTANCE.RepoOciServerStart(new RepoServerOptions());
      if (result.err != null && !result.err.isEmpty()) {
        throw new IllegalStateException("Test OCI server failed to start: " + result.err);
      }
      final String registry = result.out;
      final URL url = new URL("http://" + registry + "/v2/");
      final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
      while (true) {
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(5_000);
        connection.setReadTimeout(5_000);
        try {
          System.out.println("Test OCI server responded with " + connection.getResponseCode());
          return;
        } catch (IOException exception) {
          if (System.nanoTime() > deadline) {
            throw exception;
          }
          Thread.sleep(50);
        } finally {
          connection.disconnect();
        }
      }
    }
  }
}
