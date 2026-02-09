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
import com.marcnuri.helm.jni.NativeLibrary;

import java.nio.file.Path;

/**
 * @author Marc Nuri
 * @author Andres F. Vallecilla
 * @author Antonio Fernandez Alhambra
 */
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

  public static HistoryCommand history(String releaseName) {
    return new HistoryCommand(HelmLibHolder.INSTANCE, releaseName);
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
   * This command consists of multiple subcommands which can be used to get extended information about the release.
   *
   * @param releaseName the name of the release.
   * @return the {@link GetCommand} command.
   */
  public static GetCommand get(String releaseName) {
    return new GetCommand(HelmLibHolder.INSTANCE, releaseName);
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
   * This command provides the ability to search for Helm charts in various places including the Artifact Hub
   * and the repositories you have added.
   *
   * @return the {@link SearchCommand} command.
   */
  public static SearchCommand search() {
    return new SearchCommand(HelmLibHolder.INSTANCE);
  }

  /**
   * This command shows information about a chart.
   *
   * @param chart the chart to show.
   * @return the {@link ShowCommand} command.
   */
  public static ShowCommand show(String chart) {
    return new ShowCommand(HelmLibHolder.INSTANCE, chart);
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
   * This command renders chart templates locally and displays the output.
   *
   * @param chart The chart to render the templates for.
   * @return the {@link TemplateCommand} command.
   */
  public static TemplateCommand template(String chart) {
    return new TemplateCommand(HelmLibHolder.INSTANCE).withChart(chart);
  }

  /**
   * This command renders chart templates locally and displays the output.
   *
   * @return the {@link TemplateCommand} command.
   */
  public TemplateCommand template() {
    return new TemplateCommand(HelmLibHolder.INSTANCE, path);
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
   * This commands upgrades a release to a new version of a chart.
   *
   * @param chart the chart to upgrade.
   * @return the {@link UpgradeCommand} callable command.
   */
  public static UpgradeCommand upgrade(String chart) {
    return new UpgradeCommand(HelmLibHolder.INSTANCE).withChart(chart);
  }

  /**
   * This commands upgrades a release to a new version of the current chart.
   *
   * @return the {@link UpgradeCommand} callable command.
   */
  public UpgradeCommand upgrade() {
    return new UpgradeCommand(HelmLibHolder.INSTANCE, path);
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
