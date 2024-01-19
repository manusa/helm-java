package com.marcnuri.helm;

import com.marcnuri.helm.jni.HelmLib;
import com.marcnuri.helm.jni.NativeLibrary;

import java.nio.file.Path;

public class Helm {

  // Initialization on demand
  static final class HelmLibHolder {
    static final HelmLib INSTANCE = NativeLibrary.getInstance().load();

    private HelmLibHolder() {}
  }

  private final Path path;

  public Helm(Path path) {
    this.path = path;
  }

  /**
   * This command creates a chart directory along with the common files and directories used in a chart.
   * @return the {@link CreateCommand} callable command.
   */
  public static CreateCommand create() {
    return new CreateCommand(HelmLibHolder.INSTANCE);
  }

  public LintCommand lint() {
    return new LintCommand(HelmLibHolder.INSTANCE, path);
  }
}
