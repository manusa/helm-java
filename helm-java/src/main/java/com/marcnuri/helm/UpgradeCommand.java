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
import com.marcnuri.helm.jni.UpgradeOptions;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.marcnuri.helm.Release.parseSingle;

public class UpgradeCommand extends HelmCommand<Release> {

  private String name;
  private String version;
  private String chart;
  private String namespace;
  private boolean install;
  private boolean force;
  private boolean resetValues;
  private boolean reuseValues;
  private boolean resetThenReuseValues;
  private boolean atomic;
  private boolean cleanupOnFail;
  private boolean createNamespace;
  private String description;
  private boolean devel;
  private boolean dependencyUpdate;
  private boolean disableOpenApiValidation;
  private boolean dryRun;
  private DryRun dryRunOption;
  private boolean wait;
  private int timeout;
  private final Map<String, String> values;
  private final List<Path> valuesFiles;
  private Path kubeConfig;
  private Path certFile;
  private Path keyFile;
  private Path caFile;
  private boolean insecureSkipTlsVerify;
  private boolean plainHttp;
  private Path keyring;
  private boolean debug;
  private boolean clientOnly;
  private Path repositoryConfig;

  public UpgradeCommand(HelmLib helmLib) {
    this(helmLib, null);
  }

  public UpgradeCommand(HelmLib helmLib, Path chart) {
    super(helmLib);
    this.chart = toString(chart);
    this.values = new LinkedHashMap<>();
    this.valuesFiles = new ArrayList<>();
  }

  @Override
  public Release call() {
    return parseSingle(run(hl -> hl.Upgrade(new UpgradeOptions(
      name,
      version,
      chart,
      namespace,
      toInt(install),
      toInt(force),
      toInt(resetValues),
      toInt(reuseValues),
      toInt(resetThenReuseValues),
      toInt(atomic),
      toInt(cleanupOnFail),
      toInt(createNamespace),
      description,
      toInt(devel),
      toInt(dependencyUpdate),
      toInt(disableOpenApiValidation),
      toInt(dryRun),
      dryRunOption == null ? null : dryRunOption.name().toLowerCase(Locale.ROOT),
      toInt(wait),
      timeout,
      urlEncode(values),
      toString(valuesFiles),
      toString(kubeConfig),
      toString(certFile),
      toString(keyFile),
      toString(caFile),
      toInt(insecureSkipTlsVerify),
      toInt(plainHttp),
      toString(keyring),
      toInt(debug),
      toInt(clientOnly),
      toString(repositoryConfig)
    ))));
  }

  /**
   * Name for the release.
   *
   * @param name for the release.
   * @return this {@link UpgradeCommand} instance.
   */
  public UpgradeCommand withName(String name) {
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
   * @param version constraint to upgrade.
   * @return this {@link UpgradeCommand} instance.
   */
  public UpgradeCommand withVersion(String version) {
    this.version = version;
    return this;
  }

  /**
   * Chart reference to upgrade.
   *
   * @param chart the reference of the chart to upgrade.
   * @return this {@link UpgradeCommand} instance.
   */
  UpgradeCommand withChart(String chart) {
    this.chart = chart;
    return this;
  }

  /**
   * Kubernetes namespace scope for this request.
   *
   * @param namespace the Kubernetes namespace for this request.
   * @return this {@link UpgradeCommand} instance.
   */
  public UpgradeCommand withNamespace(String namespace) {
    this.namespace = namespace;
    return this;
  }

  /**
   * If a release by this name doesn't already exist, run an installation.
   *
   * @return this {@link UpgradeCommand} instance.
   */
  public UpgradeCommand install() {
    this.install = true;
    return this;
  }

  /**
   * Force resource updates through a replacement strategy.
   *
   * @return this {@link UpgradeCommand} instance.
   */
  public UpgradeCommand force() {
    this.force = true;
    return this;
  }

  /**
   * When upgrading, reset the values to the ones built into the chart.
   *
   * @return this {@link UpgradeCommand} instance.
   */
  public UpgradeCommand resetValues() {
    this.resetValues = true;
    return this;
  }

  /**
   * When upgrading, reuse the last release's values and merge in any overrides from the current values.
   * <p>
   * If {@link #resetValues()} is used, this is ignored.
   *
   * @return this {@link UpgradeCommand} instance.
   */
  public UpgradeCommand reuseValues() {
    this.reuseValues = true;
    return this;
  }

  /**
   * When upgrading, reset the values to the ones built into the chart, apply the last release's values and merge in any overrides from the current values.
   * <p>
   * If {@link #resetValues()} or {@link #reuseValues()} is used, this is ignored.
   *
   * @return this {@link UpgradeCommand} instance.
   */
  public UpgradeCommand resetThenReuseValues() {
    this.resetThenReuseValues = true;
    return this;
  }

  /**
   * If set, upgrade process rolls back changes made in case of failed upgrade.
   * <p>
   * The {@link #waitReady()} flag will be set automatically if used.
   *
   * @return this {@link UpgradeCommand} instance.
   */
  public UpgradeCommand atomic() {
    this.atomic = true;
    return this;
  }

  /**
   * Allow deletion of new resources created in this upgrade when upgrade fails.
   *
   * @return this {@link UpgradeCommand} instance.
   */
  public UpgradeCommand cleanupOnFail() {
    this.cleanupOnFail = true;
    return this;
  }

  /**
   * Create the release namespace if not present (if {@link #install()} is set).
   *
   * @return this {@link UpgradeCommand} instance.
   */
  public UpgradeCommand createNamespace() {
    this.createNamespace = true;
    return this;
  }

  /**
   * Add a custom description.
   *
   * @param description the custom description.
   * @return this {@link UpgradeCommand} instance.
   */
  public UpgradeCommand withDescription(String description) {
    this.description = description;
    return this;
  }

  /**
   * Use development versions, too. Equivalent to version '&gt;0.0.0-0'. If --version is set, this is ignored.
   *
   * @return this {@link UpgradeCommand} instance.
   */
  public UpgradeCommand devel() {
    this.devel = true;
    return this;
  }

  /**
   * Update dependencies if they are missing before installing the chart.
   *
   * @return this {@link UpgradeCommand} instance.
   */
  public UpgradeCommand dependencyUpdate() {
    this.dependencyUpdate = true;
    return this;
  }

  /**
   * The upgrade process will not validate rendered templates against the Kubernetes OpenAPI Schema.
   *
   * @return this {@link UpgradeCommand} instance.
   */
  public UpgradeCommand disableOpenApiValidation() {
    this.disableOpenApiValidation = true;
    return this;
  }


  /**
   * Simulate an installation.
   * <p>
   * If set with no option in dryRunOption or dryRunOption is set to 'client', it will not attempt cluster connections.
   *
   * @return this {@link UpgradeCommand} instance.
   */
  public UpgradeCommand dryRun() {
    this.dryRun = true;
    return this;
  }

  /**
   * Set the dry run option/mode.
   *
   * @param dryRunOption the dry run option/mode.
   * @return this {@link UpgradeCommand} instance.
   */
  public UpgradeCommand withDryRunOption(DryRun dryRunOption) {
    this.dryRunOption = dryRunOption;
    return this;
  }

  /**
   * Waits until all Pods are in a ready state, PVCs are bound, Deployments have minimum
   * (Desired minus maxUnavailable) Pods in ready state and Services have an IP address
   * (and Ingress if a LoadBalancer) before marking the release as successful.
   *
   * @return this {@link UpgradeCommand} instance.
   */
  public UpgradeCommand waitReady() {
    this.wait = true;
    return this;
  }

  /**
   * Time (in seconds) to wait for any individual Kubernetes operation (like Jobs for hooks) (default 300).
   *
   * @param timeout the timeout in seconds.
   * @return this {@link UpgradeCommand} instance.
   */
  public UpgradeCommand withTimeout(int timeout) {
    this.timeout = timeout;
    return this;
  }

  /**
   * Set values for the chart.
   *
   * @param key   the key.
   * @param value the value for this key.
   * @return this {@link UpgradeCommand} instance.
   */
  public UpgradeCommand set(String key, Object value) {
    this.values.put(key, value == null ? "" : value.toString());
    return this;
  }

  /**
   * Adds a values (YAML) file to source values for the chart (can specify multiple).
   *
   * @param valuesFile the path to a values file.
   * @return this {@link UpgradeCommand} instance.
   */
  public UpgradeCommand withValuesFile(Path valuesFile) {
    this.valuesFiles.add(valuesFile);
    return this;
  }

  /**
   * Set the path ./kube/config file to use.
   *
   * @param kubeConfig the path to kube config file.
   * @return this {@link UpgradeCommand} instance.
   */
  public UpgradeCommand withKubeConfig(Path kubeConfig) {
    this.kubeConfig = kubeConfig;
    return this;
  }

  /**
   * Identify registry client using this SSL certificate file.
   *
   * @param certFile the path to the certificate file.
   * @return this {@link UpgradeCommand} instance.
   */
  public UpgradeCommand withCertFile(Path certFile) {
    this.certFile = certFile;
    return this;
  }

  /**
   * Identify registry client using this SSL key file.
   *
   * @param keyFile the path to the key file.
   * @return this {@link UpgradeCommand} instance.
   */
  public UpgradeCommand withKeyFile(Path keyFile) {
    this.keyFile = keyFile;
    return this;
  }

  /**
   * Verify certificates of HTTPS-enabled servers using this CA bundle.
   *
   * @param caFile the path to the CA bundle file.
   * @return this {@link UpgradeCommand} instance.
   */
  public UpgradeCommand withCaFile(Path caFile) {
    this.caFile = caFile;
    return this;
  }

  /**
   * Skip TLS certificate checks of HTTPS-enabled servers.
   *
   * @return this {@link UpgradeCommand} instance.
   */
  public UpgradeCommand insecureSkipTlsVerify() {
    this.insecureSkipTlsVerify = true;
    return this;
  }

  /**
   * Allow insecure plain HTTP connections for the chart download.
   *
   * @return this {@link UpgradeCommand} instance.
   */
  public UpgradeCommand plainHttp() {
    this.plainHttp = true;
    return this;
  }

  /**
   * Location of a public keyring (default "~/.gnupg/pubring.gpg").
   *
   * @param keyring a {@link Path} with the keyring location.
   * @return this {@link UpgradeCommand} instance.
   */
  public UpgradeCommand withKeyring(Path keyring) {
    this.keyring = keyring;
    return this;
  }

  /**
   * Enable verbose output.
   * <p>
   * The command execution output ({@link #call}) will include verbose debug messages.
   *
   * @return this {@link UpgradeCommand} instance.
   */
  public UpgradeCommand debug() {
    this.debug = true;
    return this;
  }

  /**
   * Enable client only mode.
   * <p>
   * No connections will be made to the Kubernetes cluster. Especially intended for testing purposes.
   *
   * @return this {@link UpgradeCommand} instance.
   */
  public UpgradeCommand clientOnly() {
    this.clientOnly = true;
    return this;
  }

  /**
   * Path to the file containing repository names and URLs
   * (default "~/.config/helm/repositories.yaml")
   *
   * @param repositoryConfig a {@link Path} to the repository configuration file.
   * @return this {@link UpgradeCommand} instance.
   */
  public UpgradeCommand withRepositoryConfig(Path repositoryConfig) {
    this.repositoryConfig = repositoryConfig;
    return this;
  }
}
