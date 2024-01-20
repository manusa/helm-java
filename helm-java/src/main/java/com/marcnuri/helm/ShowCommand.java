package com.marcnuri.helm;

import com.marcnuri.helm.jni.HelmLib;
import com.marcnuri.helm.jni.ShowOptions;

import java.nio.file.Path;

public class ShowCommand {

  private final HelmLib helmLib;
  private final Path path;

  public ShowCommand(HelmLib helmLib, Path path) {
    this.helmLib = helmLib;
    this.path = path;
  }

  /**
   * This command shows all information about a chart.
   * @return the {@link ShowSubcommand} subcommand.
   */
  public ShowSubcommand all() {
    return new ShowSubcommand(helmLib, path, "all");
  }

  /**
   * This command shows the chart's definition.
   * @return the {@link ShowSubcommand} subcommand.
   */
  public ShowSubcommand chart() {
    return new ShowSubcommand(helmLib, path, "chart");
  }

  /**
   * This command shows the chart's CRDs.
   * @return the {@link ShowSubcommand} subcommand.
   */
  public ShowSubcommand crds() {
    return new ShowSubcommand(helmLib, path, "crds");
  }

  /**
   * This command shows the chart's README.
   * @return the {@link ShowSubcommand} subcommand.
   */
  public ShowSubcommand readme() {
    return new ShowSubcommand(helmLib, path, "readme");
  }

  /**
   * This command shows the chart's values.
   * @return the {@link ShowSubcommand} subcommand.
   */
  public ShowSubcommand values() {
    return new ShowSubcommand(helmLib, path, "values");
  }

  public static final class ShowSubcommand extends HelmCommand<String> {

    private final Path path;
    private final String outputFormat;

    private ShowSubcommand(HelmLib helmLib, Path path, String outputFormat) {
      super(helmLib);
      this.path = path;
      this.outputFormat = outputFormat;
    }
    /**
     * Execute the show subcommand.
     * @return a {@link String} containing the output of the show subcommand.
     */
    @Override
    public String call() {
      return run(hl -> hl.Show(new ShowOptions(path.normalize().toFile().getAbsolutePath(), outputFormat))).out;
    }
  }
}
