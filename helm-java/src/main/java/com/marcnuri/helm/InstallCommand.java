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
import com.marcnuri.helm.jni.InstallOptions;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import static com.marcnuri.helm.Release.parseSingle;

/**
 * @author Marc Nuri
 * @author Miriam Schmidt
 * @author Kevin J. Mckernan
 * @author Christian Gebhard
 * @author Antonio Fernandez Alhambra
 */
public class InstallCommand extends HelmCommand<Release> {

  private String name;
  private boolean generateName;
  private String nameTemplate;
  private String version;
  private String chart;
  private String namespace;
  private String kubeVersion;
  private boolean atomic;
  private boolean createNamespace;
  private String description;
  private boolean devel;
  private boolean dependencyUpdate;
  private boolean disableOpenApiValidation;
  private boolean dryRun;
  private DryRun dryRunOption;
  private boolean skipCrds;
  private boolean wait;
  private int timeout;
  private final Map<String, String> values;
  private final Map<String, Path> setFiles;
  private final List<Path> valuesFiles;
  private Path kubeConfig;
  private String kubeConfigContents;
  private Path certFile;
  private Path keyFile;
  private Path caFile;
  private boolean insecureSkipTlsVerify;
  private boolean plainHttp;
  private Path keyring;
  private boolean debug;
  private boolean clientOnly;
  private Path repositoryConfig;

  public InstallCommand(HelmLib helmLib) {
    this(helmLib, null);
  }

  public InstallCommand(HelmLib helmLib, Path chart) {
    super(helmLib);
    this.chart = toString(chart);
    this.values = new LinkedHashMap<>();
    this.setFiles = new LinkedHashMap<>();
    this.valuesFiles = new ArrayList<>();
  }

  @Override
  public Release call() {
    return parseSingle(run(hl -> hl.Install(new InstallOptions(
      name,
      toInt(generateName),
      nameTemplate,
      version,
      chart,
      namespace,
      kubeVersion,
      toInt(atomic),
      toInt(createNamespace),
      description,
      toInt(devel),
      toInt(dependencyUpdate),
      toInt(disableOpenApiValidation),
      toInt(dryRun),
      dryRunOption == null ? null : dryRunOption.name().toLowerCase(Locale.ROOT),
      toInt(skipCrds),
      toInt(wait),
      timeout,
      urlEncode(values, Function.identity()),
      urlEncode(setFiles, HelmCommand::toString),
      toString(valuesFiles),
      toString(kubeConfig),
      kubeConfigContents,
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
   * @return this {@link InstallCommand} instance.
   */
  public InstallCommand withName(String name) {
    this.name = name;
    return this;
  }

  /**
   * Generate the name (and omit the NAME parameter).
   *
   * @return this {@link InstallCommand} instance.
   */
  public InstallCommand generateName() {
    this.generateName = true;
    return this;
  }

  /**
   * Specify template used to name the release.
   *
   * @param nameTemplate for the release.
   * @return this {@link InstallCommand} instance.
   */
  public InstallCommand withNameTemplate(String nameTemplate) {
    this.nameTemplate = nameTemplate;
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
   * @return this {@link InstallCommand} instance.
   */
  public InstallCommand withVersion(String version) {
    this.version = version;
    return this;
  }

  /**
   * Chart reference to install.
   *
   * @param chart the reference of the chart to install.
   * @return this {@link InstallCommand} instance.
   */
  InstallCommand withChart(String chart) {
    this.chart = chart;
    return this;
  }

  /**
   * Kubernetes namespace scope for this request.
   *
   * @param namespace the Kubernetes namespace for this request.
   * @return this {@link InstallCommand} instance.
   */
  public InstallCommand withNamespace(String namespace) {
    this.namespace = namespace;
    return this;
  }

  /**
   * Kubernetes version used for capabilities and deprecation checks.
   * <p>
   * This is useful when rendering charts without connecting to a cluster, or when you want to
   * validate chart compatibility with a specific Kubernetes version.
   * <p>
   * Accepts versions with or without the "v" prefix (e.g., "v1.21.0" or "1.21.0").
   *
   * @param kubeVersion the Kubernetes version to use (e.g., "v1.21.0", "1.21.0").
   * @return this {@link InstallCommand} instance.
   */
  public InstallCommand withKubeVersion(String kubeVersion) {
    this.kubeVersion = kubeVersion;
    return this;
  }

  /**
   * Create the release namespace if not present.
   *
   * @return this {@link InstallCommand} instance.
   */
  public InstallCommand createNamespace() {
    this.createNamespace = true;
    return this;
  }

  /**
   * If set, the installation process deletes the installation on failure.
   * <p>
   * The {@link #waitReady()} flag will be set automatically if used.
   *
   * @return this {@link InstallCommand} instance.
   */
  public InstallCommand atomic() {
    this.atomic = true;
    return this;
  }

  /**
   * Add a custom description.
   *
   * @param description the custom description.
   * @return this {@link InstallCommand} instance.
   */
  public InstallCommand withDescription(String description) {
    this.description = description;
    return this;
  }

  /**
   * Use development versions, too. Equivalent to version '&gt;0.0.0-0'. If --version is set, this is ignored.
   *
   * @return this {@link InstallCommand} instance.
   */
  public InstallCommand devel() {
    this.devel = true;
    return this;
  }

  /**
   * Update dependencies if they are missing before installing the chart.
   *
   * @return this {@link InstallCommand} instance.
   */
  public InstallCommand dependencyUpdate() {
    this.dependencyUpdate = true;
    return this;
  }

  /**
   * The installation process will not validate rendered templates against the Kubernetes OpenAPI Schema.
   *
   * @return this {@link InstallCommand} instance.
   */
  public InstallCommand disableOpenApiValidation() {
    this.disableOpenApiValidation = true;
    return this;
  }

  /**
   * Simulate an installation.
   * <p>
   * If set with no option in dryRunOption or dryRunOption is set to 'client', it will not attempt cluster connections.
   *
   * @return this {@link InstallCommand} instance.
   */
  public InstallCommand dryRun() {
    this.dryRun = true;
    return this;
  }

  /**
   * Set the dry run option/mode.
   *
   * @param dryRunOption the dry run option/mode.
   * @return this {@link InstallCommand} instance.
   */
  public InstallCommand withDryRunOption(DryRun dryRunOption) {
    this.dryRunOption = dryRunOption;
    return this;
  }

  /**
   * Skip CRDs during installation.
   * <p>
   * If set, no CRDs will be installed. By default, CRDs are installed if not already present.
   *
   * @return this {@link InstallCommand} instance.
   */
  public InstallCommand skipCrds() {
    this.skipCrds = true;
    return this;
  }

  /**
   * Waits until all Pods are in a ready state, PVCs are bound, Deployments have minimum
   * (Desired minus maxUnavailable) Pods in ready state and Services have an IP address
   * (and Ingress if a LoadBalancer) before marking the release as successful.
   *
   * @return this {@link InstallCommand} instance.
   */
  public InstallCommand waitReady() {
    this.wait = true;
    return this;
  }

  /**
   * Time (in seconds) to wait for any individual Kubernetes operation (like Jobs for hooks) (default 300).
   *
   * @param timeout the timeout in seconds.
   * @return this {@link InstallCommand} instance.
   */
  public InstallCommand withTimeout(int timeout) {
    this.timeout = timeout;
    return this;
  }

  /**
   * Set values for the chart.
   *
   * @param key   the key.
   * @param value the value for this key.
   * @return this {@link InstallCommand} instance.
   */
  public InstallCommand set(String key, Object value) {
    this.values.put(key, value == null ? "" : value.toString());
    return this;
  }

  /**
   * Set a value for the chart by reading it from a file.
   * <p>
   * The file contents will be used as the value for the specified key.
   *
   * @param key  the key.
   * @param file the path to the file containing the value.
   * @return this {@link InstallCommand} instance.
   */
  public InstallCommand setFile(String key, Path file) {
    this.setFiles.put(key, file);
    return this;
  }

  /**
   * Adds a values (YAML) file to source values for the chart (can specify multiple).
   *
   * @param valuesFile the path to a values file.
   * @return this {@link InstallCommand} instance.
   */
  public InstallCommand withValuesFile(Path valuesFile) {
    this.valuesFiles.add(valuesFile);
    return this;
  }

  /**
   * Set the path ./kube/config file to use.
   *
   * @param kubeConfig the path to kube config file.
   * @return this {@link InstallCommand} instance.
   */
  public InstallCommand withKubeConfig(Path kubeConfig) {
    this.kubeConfig = kubeConfig;
    return this;
  }

  /**
   * Set the kube config to use
   *
   * @param kubeConfigContents the contents of the kube config file.
   * @return this {@link TestCommand} instance.
   */
  public InstallCommand withKubeConfigContents(String kubeConfigContents) {
    this.kubeConfigContents = kubeConfigContents;
    return this;
  }

  /**
   * Identify registry client using this SSL certificate file.
   *
   * @param certFile the path to the certificate file.
   * @return this {@link InstallCommand} instance.
   */
  public InstallCommand withCertFile(Path certFile) {
    this.certFile = certFile;
    return this;
  }

  /**
   * Identify registry client using this SSL key file.
   *
   * @param keyFile the path to the key file.
   * @return this {@link InstallCommand} instance.
   */
  public InstallCommand withKeyFile(Path keyFile) {
    this.keyFile = keyFile;
    return this;
  }

  /**
   * Verify certificates of HTTPS-enabled servers using this CA bundle.
   *
   * @param caFile the path to the CA bundle file.
   * @return this {@link InstallCommand} instance.
   */
  public InstallCommand withCaFile(Path caFile) {
    this.caFile = caFile;
    return this;
  }

  /**
   * Skip TLS certificate checks of HTTPS-enabled servers.
   *
   * @return this {@link InstallCommand} instance.
   */
  public InstallCommand insecureSkipTlsVerify() {
    this.insecureSkipTlsVerify = true;
    return this;
  }

  /**
   * Allow insecure plain HTTP connections for the chart download.
   *
   * @return this {@link InstallCommand} instance.
   */
  public InstallCommand plainHttp() {
    this.plainHttp = true;
    return this;
  }

  /**
   * Location of a public keyring (default "~/.gnupg/pubring.gpg").
   *
   * @param keyring a {@link Path} with the keyring location.
   * @return this {@link InstallCommand} instance.
   */
  public InstallCommand withKeyring(Path keyring) {
    this.keyring = keyring;
    return this;
  }

  /**
   * Enable verbose output.
   * <p>
   * The command execution output ({@link #call}) will include verbose debug messages.
   *
   * @return this {@link InstallCommand} instance.
   */
  public InstallCommand debug() {
    this.debug = true;
    return this;
  }

  /**
   * Enable client only mode.
   * <p>
   * No connections will be made to the Kubernetes cluster. Especially intended for testing purposes.
   *
   * @return this {@link InstallCommand} instance.
   */
  public InstallCommand clientOnly() {
    this.clientOnly = true;
    return this;
  }

  /**
   * Path to the file containing repository names and URLs
   * (default "~/.config/helm/repositories.yaml")
   *
   * @param repositoryConfig a {@link Path} to the repository configuration file.
   * @return this {@link InstallCommand} instance.
   */
  public InstallCommand withRepositoryConfig(Path repositoryConfig) {
    this.repositoryConfig = repositoryConfig;
    return this;
  }
}
