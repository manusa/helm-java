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
