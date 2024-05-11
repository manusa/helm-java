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
  private final String chart;

  ShowCommand(HelmLib helmLib, Path path) {
    this.helmLib = helmLib;
    this.chart = HelmCommand.toString(path);
  }

  ShowCommand(HelmLib helmLib, String chart) {
    this.helmLib = helmLib;
    this.chart = chart;
  }

  /**
   * This command shows all information about a chart.
   * @return the {@link ShowSubcommand} subcommand.
   */
  public ShowSubcommand all() {
    return new ShowSubcommand(helmLib, chart, "all");
  }

  /**
   * This command shows the chart's definition.
   * @return the {@link ShowSubcommand} subcommand.
   */
  public ShowSubcommand chart() {
    return new ShowSubcommand(helmLib, chart, "chart");
  }

  /**
   * This command shows the chart's CRDs.
   * @return the {@link ShowSubcommand} subcommand.
   */
  public ShowSubcommand crds() {
    return new ShowSubcommand(helmLib, chart, "crds");
  }

  /**
   * This command shows the chart's README.
   * @return the {@link ShowSubcommand} subcommand.
   */
  public ShowSubcommand readme() {
    return new ShowSubcommand(helmLib, chart, "readme");
  }

  /**
   * This command shows the chart's values.
   * @return the {@link ShowSubcommand} subcommand.
   */
  public ShowSubcommand values() {
    return new ShowSubcommand(helmLib, chart, "values");
  }

  public static final class ShowSubcommand extends HelmCommand<String> {

    private final String chart;
    private final String outputFormat;
    private String version;
    private Path certFile;
    private Path keyFile;
    private Path caFile;
    private boolean insecureSkipTlsVerify;
    private boolean plainHttp;
    private boolean debug;

    private ShowSubcommand(HelmLib helmLib, String chart,String outputFormat) {
      super(helmLib);
      this.chart = chart;
      this.outputFormat = outputFormat;
    }
    /**
     * Execute the show subcommand.
     * @return a {@link String} containing the output of the show subcommand.
     */
    @Override
    public String call() {
      return run(hl -> hl.Show(new ShowOptions(
        chart,
        outputFormat,
        version,
        toString(certFile),
        toString(keyFile),
        toString(caFile),
        toInt(insecureSkipTlsVerify),
        toInt(plainHttp),
        toInt(debug)
      ))).out;
    }

    /**
     * Specify a version constraint for the chart version to use.
     * <p>
     * This constraint can be a specific tag (e.g. 1.1.1) or it may reference a valid range (e.g. ^2.0.0).
     * <p>
     * If this is not specified, the latest version is used.
     *
     * @param version the version to search for.
     * @return this {@link ShowSubcommand} instance.
     */
    public ShowSubcommand withVersion(String version) {
      this.version = version;
      return this;
    }

    /**
     * Identify registry client using this SSL certificate file.
     *
     * @param certFile the path to the certificate file.
     * @return this {@link ShowSubcommand} instance.
     */
    public ShowSubcommand withCertFile(Path certFile) {
      this.certFile = certFile;
      return this;
    }

    /**
     * Identify registry client using this SSL key file.
     *
     * @param keyFile the path to the key file.
     * @return this {@link ShowSubcommand} instance.
     */
    public ShowSubcommand withKeyFile(Path keyFile) {
      this.keyFile = keyFile;
      return this;
    }

    /**
     * Verify certificates of HTTPS-enabled servers using this CA bundle.
     *
     * @param caFile the path to the CA bundle file.
     * @return this {@link ShowSubcommand} instance.
     */
    public ShowSubcommand withCaFile(Path caFile) {
      this.caFile = caFile;
      return this;
    }

    /**
     * Skip TLS certificate checks of HTTPS-enabled servers.
     *
     * @return this {@link ShowSubcommand} instance.
     */
    public ShowSubcommand insecureSkipTlsVerify() {
      this.insecureSkipTlsVerify = true;
      return this;
    }

    /**
     * Allow insecure plain HTTP connections for the chart upload.
     *
     * @return this {@link ShowSubcommand} instance.
     */
    public ShowSubcommand plainHttp() {
      this.plainHttp = true;
      return this;
    }

    /**
     * Enable verbose output.
     * <p>
     * The command execution output ({@link #call}) will include verbose debug messages.
     *
     * @return this {@link ShowSubcommand} instance.
     */
    public ShowSubcommand debug() {
      this.debug = true;
      return this;
    }
  }
}
