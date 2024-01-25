package com.marcnuri.helm;

import com.marcnuri.helm.jni.HelmLib;
import com.marcnuri.helm.jni.PushOptions;

import java.net.URI;
import java.nio.file.Path;

public class PushCommand extends HelmCommand<String> {

  private Path chart;
  private URI remote;
  private Path certFile;
  private Path keyFile;
  private Path caFile;
  private boolean insecureSkipTlsVerify;
  private boolean plainHttp;
  private boolean debug;

  public PushCommand(HelmLib helmLib) {
    super(helmLib);
  }

  @Override
  public String call() {
    return run(hl -> hl.Push(new PushOptions(
      chart.normalize().toFile().getAbsolutePath(),
      remote.toString(),
      toString(certFile),
      toString(keyFile),
      toString(caFile),
      toInt(insecureSkipTlsVerify),
      toInt(plainHttp),
      toInt(debug)
    ))).out;
  }

  /**
   * Location of the packaged chart (.tgz) to push.
   *
   * @param chart the path to the packaged chart.
   * @return this {@link PushCommand} instance.
   */
  public PushCommand withChart(Path chart) {
    this.chart = chart;
    return this;
  }

  /**
   * The URI of the remote chart repository.
   *
   * @param remote the URI of the remote chart repository.
   * @return this {@link PushCommand} instance.
   */
  public PushCommand withRemote(URI remote) {
    this.remote = remote;
    return this;
  }

  /**
   * Identify registry client using this SSL certificate file.
   *
   * @param certFile the path to the certificate file.
   * @return this {@link PushCommand} instance.
   */
  public PushCommand withCertFile(Path certFile) {
    this.certFile = certFile;
    return this;
  }

  /**
   * Identify registry client using this SSL key file.
   *
   * @param keyFile the path to the key file.
   * @return this {@link PushCommand} instance.
   */
  public PushCommand withKeyFile(Path keyFile) {
    this.keyFile = keyFile;
    return this;
  }

  /**
   * Verify certificates of HTTPS-enabled servers using this CA bundle.
   *
   * @param caFile the path to the CA bundle file.
   * @return this {@link PushCommand} instance.
   */
  public PushCommand withCaFile(Path caFile) {
    this.caFile = caFile;
    return this;
  }

  /**
   * Skip TLS certificate checks of HTTPS-enabled servers.
   *
   * @return this {@link PushCommand} instance.
   */
  public PushCommand insecureSkipTlsVerify() {
    this.insecureSkipTlsVerify = true;
    return this;
  }

  /**
   * Allow insecure plain HTTP connections for the chart upload.
   *
   * @return this {@link PushCommand} instance.
   */
  public PushCommand plainHttp() {
    this.plainHttp = true;
    return this;
  }

  /**
   * Enable verbose output.
   * <p>
   * The command execution output ({@link #call}) will include verbose debug messages.
   *
   * @return this {@link PushCommand} instance.
   */
  public PushCommand debug() {
    this.debug = true;
    return this;
  }

}
