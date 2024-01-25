package com.marcnuri.helm;

import com.marcnuri.helm.jni.HelmLib;
import com.marcnuri.helm.jni.InstallOptions;

import java.nio.file.Path;

public class InstallCommand extends HelmCommand<String> {

  private String name;
  private boolean generateName;
  private String nameTemplate;
  private String chart;
  private String namespace;
  private boolean createNamespace;
  private String description;
  private boolean devel;
  private boolean dryRun;
  private String dryRunOption;
  private Path kubeConfig;
  private Path certFile;
  private Path keyFile;
  private Path caFile;
  private boolean insecureSkipTlsVerify;
  private boolean plainHttp;
  private boolean debug;
  private boolean clientOnly;

  public InstallCommand(HelmLib helmLib) {
    this(helmLib, null);
  }

  public InstallCommand(HelmLib helmLib, Path chart) {
    super(helmLib);
    this.chart = toString(chart);
  }

  @Override
  public String call() {
    return run(hl -> hl.Install(new InstallOptions(
      name,
      toInt(generateName),
      nameTemplate,
      chart,
      namespace,
      toInt(createNamespace),
      description,
      toInt(devel),
      toInt(dryRun),
      dryRunOption,
      toString(kubeConfig),
      toString(certFile),
      toString(keyFile),
      toString(caFile),
      toInt(insecureSkipTlsVerify),
      toInt(plainHttp),
      toInt(debug),
      toInt(clientOnly)
    ))).out;
  }

  /**
   * Name for the release
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
   * Chart reference to install.
   *
   * @param chart the reference of the chart to install.
   * @return this {@link InstallCommand} instance.
   */
  public InstallCommand withChart(String chart) {
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
   * Create the release namespace if not present.
   *
   * @return this {@link InstallCommand} instance.
   */
  public InstallCommand createNamespace() {
    this.createNamespace = true;
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
   * Use development versions, too. Equivalent to version '>0.0.0-0'. If --version is set, this is ignored.
   *
   * @return this {@link InstallCommand} instance.
   */
  public InstallCommand devel() {
    this.devel = true;
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
  public InstallCommand withDryRunOption(String dryRunOption) {
    this.dryRunOption = dryRunOption;
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
}
