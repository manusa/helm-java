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
import com.marcnuri.helm.jni.HistoryOptions;
import java.nio.file.Path;
import java.util.List;

/**
 * @author Giuseppe Cardaropoli
 */
public class HistoryCommand extends HelmCommand<List<ReleaseHistory>>{

  private final String releaseName;
  private int max;
  private String namespace;
  private Path kubeConfig;
  private String kubeConfigContents;

  public HistoryCommand(HelmLib helmLib, String releaseName) {
    super(helmLib);
    this.releaseName = releaseName;
  }

  @Override
  public List<ReleaseHistory> call() {
    return ReleaseHistory.parseMultiple(run(hl -> hl.History(new HistoryOptions(
      releaseName,
      max,
      namespace,
      toString(kubeConfig),
      kubeConfigContents
    ))));
  }

  /**
   * Maximum number of revisions to include in history.
   * Default is 256.
   *
   * @param max maximum number of revisions.
   * @return this {@link HistoryCommand} instance.
   */
  public HistoryCommand withMax(int max) {
    this.max = max;
    return this;
  }

  /**
   * Kubernetes namespace scope for this request.
   *
   * @param namespace the Kubernetes namespace for this request.
   * @return this {@link HistoryCommand} instance.
   */
  public HistoryCommand withNamespace(String namespace) {
    this.namespace = namespace;
    return this;
  }

  /**
   * Set the path to the ~/.kube/config file to use.
   *
   * @param kubeConfig the path to Kube config file.
   * @return this {@link HistoryCommand} instance.
   */
  public HistoryCommand withKubeConfig(Path kubeConfig) {
    this.kubeConfig = kubeConfig;
    return this;
  }

  /**
   * Set the Kube config to use.
   *
   * @param kubeConfigContents the contents of the Kube config file.
   * @return this {@link HistoryCommand} instance.
   */
  public HistoryCommand withKubeConfigContents(String kubeConfigContents) {
    this.kubeConfigContents = kubeConfigContents;
    return this;
  }
}
