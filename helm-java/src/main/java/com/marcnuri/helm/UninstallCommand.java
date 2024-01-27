package com.marcnuri.helm;

import com.marcnuri.helm.jni.HelmLib;
import com.marcnuri.helm.jni.UninstallOptions;

import java.nio.file.Path;
import java.util.Locale;

public class UninstallCommand extends HelmCommand<String> {

  public enum Cascade {
    BACKGROUND, ORPHAN, FOREGROUND
  }

  private final String releaseName;
  private boolean dryRun;
  private boolean noHooks;
  private boolean ignoreNotFound;
  private boolean keepHistory;
  private Cascade cascade;
  private String namespace;
  private Path kubeConfig;
  private boolean debug;

  public UninstallCommand(HelmLib helmLib, String releaseName) {
    super(helmLib);
    this.releaseName = releaseName;
  }

  @Override
  public String call() {
    return run(hl -> hl.Uninstall(new UninstallOptions(
      releaseName,
      toInt(dryRun),
      toInt(noHooks),
      toInt(ignoreNotFound),
      toInt(keepHistory),
      cascade == null ? null : cascade.name().toLowerCase(Locale.ROOT),
      namespace,
      toString(kubeConfig),
      toInt(debug)
    ))).out;
  }

  /**
   * Simulate an uninstallation.
   *
   * @return this {@link UninstallCommand} instance.
   */
  public UninstallCommand dryRun() {
    this.dryRun = true;
    return this;
  }

  /**
   * Prevent hooks from running during uninstallation.
   *
   * @return this {@link UninstallCommand} instance.
   */
  public UninstallCommand noHooks() {
    this.noHooks = true;
    return this;
  }

  /**
   * Treat "release not found" as a successful uninstall.
   *
   * @return this {@link UninstallCommand} instance.
   */
  public UninstallCommand ignoreNotFound() {
    this.ignoreNotFound = true;
    return this;
  }

  /**
   * Remove all associated resources and mark the release as deleted, but retain the release history.
   *
   * @return this {@link UninstallCommand} instance.
   */
  public UninstallCommand keepHistory() {
    this.keepHistory = true;
    return this;
  }


  /**
   * Selects the deletion cascading strategy for the dependents. Defaults to background. (default "background")
   *
   * @param cascade the deletion cascading strategy for the dependents.
   * @return this {@link UninstallCommand} instance.
   */
  public UninstallCommand withCascade(Cascade cascade) {
    this.cascade = cascade;
    return this;
  }

  /**
   * Kubernetes namespace scope for this request.
   *
   * @param namespace the Kubernetes namespace for this request.
   * @return this {@link UninstallCommand} instance.
   */
  public UninstallCommand withNamespace(String namespace) {
    this.namespace = namespace;
    return this;
  }

  /**
   * Set the path ./kube/config file to use.
   *
   * @param kubeConfig the path to kube config file.
   * @return this {@link UninstallCommand} instance.
   */
  public UninstallCommand withKubeConfig(Path kubeConfig) {
    this.kubeConfig = kubeConfig;
    return this;
  }

  /**
   * Enable verbose output.
   * <p>
   * The command execution output ({@link #call}) will include verbose debug messages.
   *
   * @return this {@link UninstallCommand} instance.
   */
  public UninstallCommand debug() {
    this.debug = true;
    return this;
  }
}
