package com.marcnuri.helm;

import com.marcnuri.helm.jni.HelmLib;
import com.marcnuri.helm.jni.TestOptions;

import java.nio.file.Path;

import static com.marcnuri.helm.ReleaseResult.parse;

public class TestCommand extends HelmCommand<ReleaseResult> {

  private final String releaseName;
  private int timeout;
  private String namespace;
  private Path kubeConfig;
  private boolean debug;

  public TestCommand(HelmLib helmLib, String releaseName) {
    super(helmLib);
    this.releaseName = releaseName;
  }

  @Override
  public ReleaseResult call() {
    return parse(run(hl -> hl.Test(new TestOptions(
      releaseName,
      timeout,
      namespace,
      toString(kubeConfig),
      toInt(debug)
    ))));
  }

  /**
   * Time (in seconds) to wait for any individual Kubernetes operation (like Jobs for hooks) (default 300).
   *
   * @param timeout the timeout in seconds.
   * @return this {@link TestCommand} instance.
   */
  public TestCommand withTimeout(int timeout) {
    this.timeout = timeout;
    return this;
  }

  /**
   * Kubernetes namespace scope for this request.
   *
   * @param namespace the Kubernetes namespace for this request.
   * @return this {@link TestCommand} instance.
   */
  public TestCommand withNamespace(String namespace) {
    this.namespace = namespace;
    return this;
  }

  /**
   * Set the path ./kube/config file to use.
   *
   * @param kubeConfig the path to kube config file.
   * @return this {@link TestCommand} instance.
   */
  public TestCommand withKubeConfig(Path kubeConfig) {
    this.kubeConfig = kubeConfig;
    return this;
  }

  /**
   * Enable verbose output.
   * <p>
   * The command execution output ({@link #call}) will include verbose debug messages.
   *
   * @return this {@link TestCommand} instance.
   */
  public TestCommand debug() {
    this.debug = true;
    return this;
  }
}
