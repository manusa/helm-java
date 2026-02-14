/*
 * Copyright 2026 Marc Nuri
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
import com.marcnuri.helm.jni.StatusOptions;

import java.nio.file.Path;

import static com.marcnuri.helm.Release.parseSingle;

/**
 * Displays the status of the named release.
 *
 * <p>This command shows the current state of a release including:
 * <ul>
 *   <li>The last deployment time</li>
 *   <li>The Kubernetes namespace</li>
 *   <li>The current state (deployed, failed, etc.)</li>
 *   <li>The revision number</li>
 *   <li>The chart name and version</li>
 *   <li>Any notes provided by the chart</li>
 * </ul>
 *
 * @author Marc Nuri
 */
public class StatusCommand extends HelmCommand<Release> {

  private final String releaseName;
  private int revision;
  private String namespace;
  private Path kubeConfig;
  private String kubeConfigContents;

  public StatusCommand(HelmLib helmLib, String releaseName) {
    super(helmLib);
    this.releaseName = releaseName;
  }

  /**
   * Execute the status command.
   *
   * @return a {@link Release} containing the status information.
   */
  @Override
  public Release call() {
    return parseSingle(run(hl -> hl.Status(new StatusOptions(
      releaseName,
      revision,
      namespace,
      toString(kubeConfig),
      kubeConfigContents
    ))));
  }

  /**
   * Get the status of the named release with the specified revision.
   *
   * <p>If not specified, the latest release is returned.
   *
   * @param revision the revision number.
   * @return this {@link StatusCommand} instance.
   */
  public StatusCommand withRevision(int revision) {
    this.revision = revision;
    return this;
  }

  /**
   * Kubernetes namespace scope for this request.
   *
   * @param namespace the Kubernetes namespace for this request.
   * @return this {@link StatusCommand} instance.
   */
  public StatusCommand withNamespace(String namespace) {
    this.namespace = namespace;
    return this;
  }

  /**
   * Set the path to the ~/.kube/config file to use.
   *
   * @param kubeConfig the path to kube config file.
   * @return this {@link StatusCommand} instance.
   */
  public StatusCommand withKubeConfig(Path kubeConfig) {
    this.kubeConfig = kubeConfig;
    return this;
  }

  /**
   * Set the kube config to use.
   *
   * @param kubeConfigContents the contents of the kube config file.
   * @return this {@link StatusCommand} instance.
   */
  public StatusCommand withKubeConfigContents(String kubeConfigContents) {
    this.kubeConfigContents = kubeConfigContents;
    return this;
  }
}
