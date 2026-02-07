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

import com.marcnuri.helm.jni.GetValuesOptions;
import com.marcnuri.helm.jni.HelmLib;

import java.nio.file.Path;

/**
 * This command consists of multiple subcommands which can be used to get extended information about the release.
 *
 * @author Antonio Fernandez Alhambra
 */
public class GetCommand {

  private final HelmLib helmLib;
  private final String releaseName;

  GetCommand(HelmLib helmLib, String releaseName) {
    this.helmLib = helmLib;
    this.releaseName = releaseName;
  }

  /**
   * This command downloads the values file for a given release.
   *
   * @return the {@link GetValuesSubcommand} subcommand.
   */
  public GetValuesSubcommand values() {
    return new GetValuesSubcommand(helmLib, releaseName);
  }

  public static final class GetValuesSubcommand extends HelmCommand<String> {

    private final String releaseName;
    private boolean allValues;
    private int revision;
    private String namespace;
    private Path kubeConfig;
    private String kubeConfigContents;

    private GetValuesSubcommand(HelmLib helmLib, String releaseName) {
      super(helmLib);
      this.releaseName = releaseName;
    }

    /**
     * Execute the get values subcommand.
     *
     * @return a {@link String} containing the values in YAML format.
     */
    @Override
    public String call() {
      return run(hl -> hl.GetValues(new GetValuesOptions(
        releaseName,
        toInt(allValues),
        revision,
        namespace,
        toString(kubeConfig),
        kubeConfigContents
      ))).out;
    }

    /**
     * Dump all (computed) values.
     * <p>
     * When set, all computed values are returned, including the default values from the chart.
     *
     * @return this {@link GetValuesSubcommand} instance.
     */
    public GetValuesSubcommand allValues() {
      this.allValues = true;
      return this;
    }

    /**
     * Get the named release with revision.
     * <p>
     * If not specified, the latest release is returned.
     *
     * @param revision the revision number.
     * @return this {@link GetValuesSubcommand} instance.
     */
    public GetValuesSubcommand withRevision(int revision) {
      this.revision = revision;
      return this;
    }

    /**
     * Kubernetes namespace scope for this request.
     *
     * @param namespace the Kubernetes namespace for this request.
     * @return this {@link GetValuesSubcommand} instance.
     */
    public GetValuesSubcommand withNamespace(String namespace) {
      this.namespace = namespace;
      return this;
    }

    /**
     * Set the path to the ~/.kube/config file to use.
     *
     * @param kubeConfig the path to kube config file.
     * @return this {@link GetValuesSubcommand} instance.
     */
    public GetValuesSubcommand withKubeConfig(Path kubeConfig) {
      this.kubeConfig = kubeConfig;
      return this;
    }

    /**
     * Set the kube config to use.
     *
     * @param kubeConfigContents the contents of the kube config file.
     * @return this {@link GetValuesSubcommand} instance.
     */
    public GetValuesSubcommand withKubeConfigContents(String kubeConfigContents) {
      this.kubeConfigContents = kubeConfigContents;
      return this;
    }
  }
}
