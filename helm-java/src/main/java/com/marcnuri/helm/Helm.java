package com.marcnuri.helm;

import com.marcnuri.helm.jni.HelmLib;
import com.marcnuri.helm.jni.NativeLibrary;

import java.nio.file.Path;

public class Helm {

  // Initialization on demand
  static final class HelmLibHolder {
    static final HelmLib INSTANCE = NativeLibrary.getInstance().load();

    private HelmLibHolder() {
    }
  }

  private final Path path;

  public Helm(Path path) {
    this.path = path;
  }

  /**
   * This command creates a chart directory along with the common files and directories used in a chart.
   *
   * @return the {@link CreateCommand} callable command.
   */
  public static CreateCommand create() {
    return new CreateCommand(HelmLibHolder.INSTANCE);
  }

  /**
   * This command allows you to manage a chart's dependencies.
   *
   * @return the {@link DependencyCommand} command.
   */
  public DependencyCommand dependency() {
    return new DependencyCommand(HelmLibHolder.INSTANCE, path);
  }

  /**
   * This commands installs the referenced chart archive.
   *
   * @param chart the chart to install.
   * @return the {@link InstallCommand} callable command.
   */
  public static InstallCommand install(String chart) {
    return new InstallCommand(HelmLibHolder.INSTANCE).withChart(chart);
  }

  /**
   * This commands installs the current chart.
   *
   * @return the {@link InstallCommand} callable command.
   */
  public InstallCommand install() {
    return new InstallCommand(HelmLibHolder.INSTANCE, path);
  }

  /**
   * This command examines a chart for possible issues.
   *
   * @return the {@link LintCommand} callable command.
   */
  public LintCommand lint() {
    return new LintCommand(HelmLibHolder.INSTANCE, path);
  }

  /**
   * Lists all the releases for a specified namespace (uses current namespace context if namespace not specified).
   *
   * @return the {@link ListCommand} callable command.
   */
  public static ListCommand list() {
    return new ListCommand(HelmLibHolder.INSTANCE);
  }

  /**
   * This command packages a chart into a versioned chart archive file.
   * If a path is given, this will look at that path for a chart (which must contain a Chart.yaml file) and then package that directory.
   *
   * @return the {@link PackageCommand} callable command.
   */
  public PackageCommand packageIt() {
    return new PackageCommand(HelmLibHolder.INSTANCE, this, path);
  }

  /**
   * This command uploads a chart to a registry.
   *
   * @return the {@link PushCommand} callable command.
   */
  public static PushCommand push() {
    return new PushCommand(HelmLibHolder.INSTANCE);
  }

  /**
   * This command allows you to log in to or out from a Helm registry.
   *
   * @return the {@link RegistryCommand} command.
   */
  public static RegistryCommand registry() {
    return new RegistryCommand(HelmLibHolder.INSTANCE);
  }

  /**
   * This command allows you to add, list, remove, update, and index chart repositories.
   *
   * @return the {@link RepoCommand} command.
   */
  public static RepoCommand repo() {
    return new RepoCommand(HelmLibHolder.INSTANCE);
  }

  /**
   * This command shows information about a chart.
   *
   * @return the {@link ShowCommand} command.
   */
  public ShowCommand show() {
    return new ShowCommand(HelmLibHolder.INSTANCE, path);
  }

  /**
   * This command runs the tests for a release.
   *
   * @param releaseName the name of the release to test.
   * @return the {@link TestCommand} callable command.
   */
  public static TestCommand test(String releaseName) {
    return new TestCommand(HelmLibHolder.INSTANCE, releaseName);
  }

  /**
   * This command takes a release name and uninstalls the release.
   *
   * @param releaseName the name of the release to uninstall.
   * @return the {@link UninstallCommand} callable command.
   */
  public static UninstallCommand uninstall(String releaseName) {
    return new UninstallCommand(HelmLibHolder.INSTANCE, releaseName);
  }

  /**
   * This command returns the underlying Helm library version
   *
   * @return the {@link VersionCommand} callable command.
   */
  public static VersionCommand version() {
    return new VersionCommand(HelmLibHolder.INSTANCE);
  }
}
