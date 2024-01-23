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
   * This command examines a chart for possible issues.
   *
   * @return the {@link LintCommand} callable command.
   */
  public LintCommand lint() {
    return new LintCommand(HelmLibHolder.INSTANCE, path);
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
   * This command shows information about a chart.
   *
   * @return the {@link ShowCommand} command.
   */
  public ShowCommand show() {
    return new ShowCommand(HelmLibHolder.INSTANCE, path);
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
