package com.marcnuri.helm;

import com.marcnuri.helm.jni.HelmLib;
import com.marcnuri.helm.jni.RepoOptions;
import com.marcnuri.helm.jni.Result;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Function;

public class RepoCommand {

  private final HelmLib helmLib;

  public RepoCommand(HelmLib helmLib) {
    this.helmLib = helmLib;
  }

  /**
   * Add a chart repository.
   *
   * @return the {@link RepoCommand.RepoSubcommand} subcommand.
   */
  public RepoCommand.RepoSubcommand<Void> add() {
    return new RepoCommand.RepoSubcommand<>(helmLib, hl -> hl::RepoAdd, r -> null);
  }

  /**
   * List chart repositories
   *
   * @return the {@link WithRepositoryConfig} subcommand.
   */
  public WithRepositoryConfig<List<Repository>> list() {
    return new RepoCommand.RepoSubcommand<>(helmLib, hl -> hl::RepoList, Repository::parse);
  }

  public static final class RepoSubcommand<T> extends HelmCommand<T> implements WithRepositoryConfig<T> {

    private final Function<HelmLib, Function<RepoOptions, Result>> callable;
    private final Function<Result, T> transformer;
    private Path repositoryConfig;
    private String name;
    private URI url;
    private String username;
    private String password;
    private Path certFile;
    private Path keyFile;
    private Path caFile;
    private boolean insecureSkipTlsVerify;


    RepoSubcommand(HelmLib helmLib, Function<HelmLib, Function<RepoOptions, Result>> callable, Function<Result, T> transformer) {
      super(helmLib);
      this.callable = callable;
      this.transformer = transformer;
    }

    @Override
    public T call() {
      return transformer.apply(run(hl -> callable.apply(hl).apply(new RepoOptions(
        toString(repositoryConfig),
        name,
        null,
        toString(url),
        username,
        password,
        toString(certFile),
        toString(keyFile),
        toString(caFile),
        toInt(insecureSkipTlsVerify)
      ))));
    }

    /**
     * {@inheritDoc}
     */
    public RepoCommand.RepoSubcommand<T> withRepositoryConfig(Path repositoryConfig) {
      this.repositoryConfig = repositoryConfig;
      return this;
    }

    /**
     * The name of the repository.
     *
     * @param name a {@link String} with the name of the repository.
     * @return this {@link RepoCommand.RepoSubcommand} instance.
     */
    public RepoCommand.RepoSubcommand<T> withName(String name) {
      this.name = name;
      return this;
    }

    /**
     * The url of the repository.
     *
     * @param url a {@link URI} with the url of the repository.
     * @return this {@link RepoCommand.RepoSubcommand} instance.
     */
    public RepoCommand.RepoSubcommand<T> withUrl(URI url) {
      this.url = url;
      return this;
    }

    /**
     * The username of the repository.
     *
     * @param username a {@link String} with the username of the repository.
     * @return this {@link RepoCommand.RepoSubcommand} instance.
     */
    public RepoCommand.RepoSubcommand<T> withUsername(String username) {
      this.username = username;
      return this;
    }

    /**
     * The password of the repository.
     *
     * @param password a {@link String} with the password of the repository.
     * @return this {@link RepoCommand.RepoSubcommand} instance.
     */
    public RepoCommand.RepoSubcommand<T> withPassword(String password) {
      this.password = password;
      return this;
    }

    /**
     * Identify HTTPS client using this SSL certificate file.
     *
     * @param certFile the path to the certificate file.
     * @return this {@link RepoCommand.RepoSubcommand} instance.
     */
    public RepoCommand.RepoSubcommand<T> withCertFile(Path certFile) {
      this.certFile = certFile;
      return this;
    }

    /**
     * Identify HTTPS client using this SSL key file.
     *
     * @param keyFile the path to the key file.
     * @return this {@link RepoCommand.RepoSubcommand} instance.
     */
    public RepoCommand.RepoSubcommand<T> withKeyFile(Path keyFile) {
      this.keyFile = keyFile;
      return this;
    }

    /**
     * Verify certificates of HTTPS-enabled servers using this CA bundle.
     *
     * @param caFile the path to the CA bundle file.
     * @return this {@link RepoCommand.RepoSubcommand} instance.
     */
    public RepoCommand.RepoSubcommand<T> withCaFile(Path caFile) {
      this.caFile = caFile;
      return this;
    }

    /**
     * Skip TLS certificate checks of HTTPS-enabled servers.
     *
     * @return this {@link RepoCommand.RepoSubcommand} instance.
     */
    public RepoCommand.RepoSubcommand<T> insecureSkipTlsVerify() {
      this.insecureSkipTlsVerify = true;
      return this;
    }
  }

  public interface WithRepositoryConfig<T> extends RepoCallable<T> {
    /**
     * Path to the file containing repository names and URLs
     * (default "~/.config/helm/repositories.yaml")
     *
     * @param repositoryConfig a {@link Path} to the repository configuration file.
     * @return this {@link RepoCommand.RepoSubcommand} instance.
     */
    WithRepositoryConfig<T> withRepositoryConfig(Path repositoryConfig);
  }
  public interface RepoCallable<T> extends Callable<T> {
    @Override
    T call();
  }
}
