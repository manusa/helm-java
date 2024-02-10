package com.marcnuri.helm;

import com.marcnuri.helm.jni.HelmLib;
import com.marcnuri.helm.jni.RepoOptions;
import com.marcnuri.helm.jni.Result;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

public class RepoCommand {

  private final HelmLib helmLib;

  public RepoCommand(HelmLib helmLib) {
    this.helmLib = helmLib;
  }

  /**
   * List chart repositories
   *
   * @return the {@link RepoCommand.RepoSubcommand} subcommand.
   */
  public RepoCommand.RepoSubcommand<List<Repository>> list() {
    return new RepoCommand.RepoSubcommand<>(helmLib, hl -> hl::RepoList, Repository::parse);
  }

  public static final class RepoSubcommand<T> extends HelmCommand<T> {

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
     * Path to the file containing repository names and URLs
     * (default "~/.config/helm/repositories.yaml")
     *
     * @param repositoryConfig a {@link Path} to the repository configuration file.
     * @return this {@link RepoCommand.RepoSubcommand} instance.
     */
    public RepoCommand.RepoSubcommand<T> withRepositoryConfig(Path repositoryConfig) {
      this.repositoryConfig = repositoryConfig;
      return this;
    }
  }

}
