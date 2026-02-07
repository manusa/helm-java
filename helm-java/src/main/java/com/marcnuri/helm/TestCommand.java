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

import com.marcnuri.helm.jni.HelmLib;
import com.marcnuri.helm.jni.TestOptions;

import java.nio.file.Path;

import static com.marcnuri.helm.Release.parseSingle;

/**
 * @author Marc Nuri
 * @author Christian Gebhard
 */
public class TestCommand extends HelmCommand<Release> {

  private final String releaseName;
  private int timeout;
  private String namespace;
  private Path kubeConfig;
  private String kubeConfigContents;
  private boolean debug;

  public TestCommand(HelmLib helmLib, String releaseName) {
    super(helmLib);
    this.releaseName = releaseName;
  }

  @Override
  public Release call() {
    return parseSingle(run(hl -> hl.Test(new TestOptions(
      releaseName,
      timeout,
      namespace,
      toString(kubeConfig),
      kubeConfigContents,
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
   * Set the path to the ~/.kube/config file to use.
   *
   * @param kubeConfig the path to kube config file.
   * @return this {@link TestCommand} instance.
   */
  public TestCommand withKubeConfig(Path kubeConfig) {
    this.kubeConfig = kubeConfig;
    return this;
  }

  /**
   * Set the kube config to use
   *
   * @param kubeConfigContents the contents of the kube config file.
   * @return this {@link TestCommand} instance.
   */
  public TestCommand withKubeConfigContents(String kubeConfigContents) {
    this.kubeConfigContents = kubeConfigContents;
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
