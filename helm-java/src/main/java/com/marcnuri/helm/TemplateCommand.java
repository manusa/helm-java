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
import com.marcnuri.helm.jni.TemplateOptions;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Marc Nuri
 * @author Andres F. Vallecilla
 * @author Antonio Fernandez Alhambra
 */
public class TemplateCommand extends HelmCommand<String> {

  private String name;
  private String version;
  private String chart;
  private String namespace;
  private String kubeVersion;
  private boolean dependencyUpdate;
  private boolean skipCrds;
  private final Map<String, String> values;
  private final List<Path> valuesFiles;
  private Path certFile;
  private Path keyFile;
  private Path caFile;
  private boolean insecureSkipTlsVerify;
  private boolean plainHttp;
  private Path keyring;
  private boolean debug;
  private Path repositoryConfig;

  public TemplateCommand(HelmLib helmLib) {
    this(helmLib, null);
  }

  public TemplateCommand(HelmLib helmLib, Path chart) {
    super(helmLib);
    this.chart = toString(chart);
    this.values = new LinkedHashMap<>();
    this.valuesFiles = new ArrayList<>();
  }

  @Override
  public String call() {
    return run(hl -> hl.Template(new TemplateOptions(
      name,
      version,
      chart,
      namespace,
      kubeVersion,
      toInt(dependencyUpdate),
      toInt(skipCrds),
      urlEncode(values),
      toString(valuesFiles),
      toString(certFile),
      toString(keyFile),
      toString(caFile),
      toInt(insecureSkipTlsVerify),
      toInt(plainHttp),
      toString(keyring),
      toInt(debug),
      toString(repositoryConfig)
    ))).out;
  }

  /**
   * Name for the release.
   *
   * @param name for the release.
   * @return this {@link TemplateCommand} instance.
   */
  public TemplateCommand withName(String name) {
    this.name = name;
    return this;
  }

  /**
   * Specify a version constraint for the chart version to use.
   * <p>
   * This constraint can be a specific tag (e.g. 1.1.1) or it may reference a valid range (e.g. ^2.0.0).
   * <p>
   * If this is not specified, the latest version is used.
   *
   * @param version constraint to install.
   * @return this {@link TemplateCommand} instance.
   */
  public TemplateCommand withVersion(String version) {
    this.version = version;
    return this;
  }

  /**
   * Chart reference to render the templates.
   *
   * @param chart the reference of the chart to render the templates.
   * @return this {@link TemplateCommand} instance.
   */
  TemplateCommand withChart(String chart) {
    this.chart = chart;
    return this;
  }

  /**
   * Kubernetes namespace scope for this request.
   *
   * @param namespace the Kubernetes namespace for this request.
   * @return this {@link TemplateCommand} instance.
   */
  public TemplateCommand withNamespace(String namespace) {
    this.namespace = namespace;
    return this;
  }

  /**
   * Kubernetes version for this request.
   *
   * @param kubeVersion the Kubernetes version for this request.
   * @return this {@link TemplateCommand} instance.
   */
  public TemplateCommand withKubeVersion(String kubeVersion) {
    this.kubeVersion = kubeVersion;
    return this;
  }

  /**
   * Update dependencies if they are missing before rendering the chart.
   *
   * @return this {@link TemplateCommand} instance.
   */
  public TemplateCommand dependencyUpdate() {
    this.dependencyUpdate = true;
    return this;
  }

  /**
   * Skip CRDs during template rendering.
   * <p>
   * If set, no CRDs will be included in the rendered templates.
   *
   * @return this {@link TemplateCommand} instance.
   */
  public TemplateCommand skipCrds() {
    this.skipCrds = true;
    return this;
  }

  /**
   * Set values for the chart.
   *
   * @param key   the key.
   * @param value the value for this key.
   * @return this {@link TemplateCommand} instance.
   */
  public TemplateCommand set(String key, Object value) {
    this.values.put(key, value == null ? "" : value.toString());
    return this;
  }

  /**
   * Adds a values (YAML) file to source values for the chart (can specify multiple).
   *
   * @param valuesFile the path to a values file.
   * @return this {@link TemplateCommand} instance.
   */
  public TemplateCommand withValuesFile(Path valuesFile) {
    this.valuesFiles.add(valuesFile);
    return this;
  }

  /**
   * Identify registry client using this SSL certificate file.
   *
   * @param certFile the path to the certificate file.
   * @return this {@link TemplateCommand} instance.
   */
  public TemplateCommand withCertFile(Path certFile) {
    this.certFile = certFile;
    return this;
  }

  /**
   * Identify registry client using this SSL key file.
   *
   * @param keyFile the path to the key file.
   * @return this {@link TemplateCommand} instance.
   */
  public TemplateCommand withKeyFile(Path keyFile) {
    this.keyFile = keyFile;
    return this;
  }

  /**
   * Verify certificates of HTTPS-enabled servers using this CA bundle.
   *
   * @param caFile the path to the CA bundle file.
   * @return this {@link TemplateCommand} instance.
   */
  public TemplateCommand withCaFile(Path caFile) {
    this.caFile = caFile;
    return this;
  }

  /**
   * Skip TLS certificate checks of HTTPS-enabled servers.
   *
   * @return this {@link TemplateCommand} instance.
   */
  public TemplateCommand insecureSkipTlsVerify() {
    this.insecureSkipTlsVerify = true;
    return this;
  }

  /**
   * Allow insecure plain HTTP connections for the chart download.
   *
   * @return this {@link TemplateCommand} instance.
   */
  public TemplateCommand plainHttp() {
    this.plainHttp = true;
    return this;
  }

  /**
   * Location of a public keyring (default "~/.gnupg/pubring.gpg").
   *
   * @param keyring a {@link Path} with the keyring location.
   * @return this {@link TemplateCommand} instance.
   */
  public TemplateCommand withKeyring(Path keyring) {
    this.keyring = keyring;
    return this;
  }

  /**
   * Enable verbose output.
   * <p>
   * The command execution output ({@link #call}) will include verbose debug messages.
   *
   * @return this {@link TemplateCommand} instance.
   */
  public TemplateCommand debug() {
    this.debug = true;
    return this;
  }

  /**
   * Path to the file containing repository names and URLs
   * (default "~/.config/helm/repositories.yaml")
   *
   * @param repositoryConfig a {@link Path} to the repository configuration file.
   * @return this {@link TemplateCommand} instance.
   */
  public TemplateCommand withRepositoryConfig(Path repositoryConfig) {
    this.repositoryConfig = repositoryConfig;
    return this;
  }

}
