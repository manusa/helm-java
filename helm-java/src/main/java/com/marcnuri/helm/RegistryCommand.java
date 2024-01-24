package com.marcnuri.helm;

import com.marcnuri.helm.jni.HelmLib;
import com.marcnuri.helm.jni.RegistryLoginOptions;

import java.nio.file.Path;

public class RegistryCommand {

  private final HelmLib helmLib;

  public RegistryCommand(HelmLib helmLib) {
    this.helmLib = helmLib;
  }

  public LoginCommand login() {
    return new LoginCommand(helmLib);
  }

  public static final class LoginCommand extends RegistrySubcommand<LoginCommand> {

    private String username;
    private String password;

    LoginCommand(HelmLib helmLib) {
      super(helmLib);
    }

    @Override
    public String call() {
      return run(hl -> hl.RegistryLogin(new RegistryLoginOptions(
        host,
        username,
        password,
        certFile == null ? null : certFile.normalize().toFile().getAbsolutePath(),
        keyFile == null ? null : keyFile.normalize().toFile().getAbsolutePath(),
        caFile == null ? null : caFile.normalize().toFile().getAbsolutePath(),
        insecureSkipTlsVerify ? 1 : 0,
        plainHttp ? 1 : 0,
        debug ? 1 : 0
      ))).out;
    }

    /**
     * Registry username.
     *
     * @param username the username to use for the registry.
     * @return this {@link LoginCommand} instance.
     */
    public LoginCommand withUsername(String username) {
      this.username = username;
      return this;
    }

    /**
     * Registry password or identity token.
     *
     * @param password the password to use for the registry.
     * @return this {@link LoginCommand} instance.
     */
    public LoginCommand withPassword(String password) {
      this.password = password;
      return this;
    }
  }


  private abstract static class RegistrySubcommand<T extends RegistrySubcommand> extends HelmCommand<String> {

    String host;
    Path certFile;
    Path keyFile;
    Path caFile;
    boolean insecureSkipTlsVerify;
    boolean plainHttp;
    boolean debug;

    RegistrySubcommand(HelmLib helmLib) {
      super(helmLib);
    }

    /**
     * The host to log in to.
     *
     * @param host the URI or name of the registry to log in to.
     * @return this {@link RegistrySubcommand} instance.
     */
    public T withHost(String host) {
      this.host = host;
      return (T) this;
    }


    /**
     * Identify registry client using this SSL certificate file.
     *
     * @param certFile the path to the certificate file.
     * @return this {@link RegistrySubcommand} instance.
     */
    public T withCertFile(Path certFile) {
      this.certFile = certFile;
      return (T) this;
    }

    /**
     * Identify registry client using this SSL key file.
     *
     * @param keyFile the path to the key file.
     * @return this {@link RegistrySubcommand} instance.
     */
    public T withKeyFile(Path keyFile) {
      this.keyFile = keyFile;
      return (T) this;
    }

    /**
     * Verify certificates of HTTPS-enabled servers using this CA bundle.
     *
     * @param caFile the path to the CA bundle file.
     * @return this {@link RegistrySubcommand} instance.
     */
    public T withCaFile(Path caFile) {
      this.caFile = caFile;
      return (T) this;
    }

    /**
     * Skip TLS certificate checks of HTTPS-enabled servers.
     *
     * @return this {@link RegistrySubcommand} instance.
     */
    public T insecureSkipTlsVerify() {
      this.insecureSkipTlsVerify = true;
      return (T) this;
    }

    /**
     * Allow insecure plain HTTP connections for the registry operation.
     *
     * @return this {@link RegistrySubcommand} instance.
     */
    public T plainHttp() {
      this.plainHttp = true;
      return (T) this;
    }

    /**
     * Enable verbose output.
     * <p>
     * The command execution output ({@link #call}) will include verbose debug messages.
     *
     * @return this {@link RegistrySubcommand} instance.
     */
    public T debug() {
      this.debug = true;
      return (T) this;
    }

  }

}
