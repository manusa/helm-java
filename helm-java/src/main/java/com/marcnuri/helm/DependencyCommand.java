package com.marcnuri.helm;

import com.marcnuri.helm.jni.DependencyOptions;
import com.marcnuri.helm.jni.HelmLib;
import com.marcnuri.helm.jni.Result;

import java.nio.file.Path;
import java.util.function.Function;

public class DependencyCommand {

  private final HelmLib helmLib;
  private final Path path;

  public DependencyCommand(HelmLib helmLib, Path path) {
    this.helmLib = helmLib;
    this.path = path;
  }

  /**
   * Rebuild the chart's on-disk dependencies (charts/) based on the Chart.lock file.
   *
   * @return the {@link DependencySubcommand} subcommand.
   */
  public DependencySubcommand<String> build() {
    return new DependencySubcommand<>(helmLib, path, hl -> hl::DependencyBuild, r -> r.out);
  }

  /**
   * List the dependencies for the chart.
   *
   * @return the {@link DependencySubcommand} subcommand.
   */
  public DependencySubcommand<DependencyListResult> list() {
    return new DependencySubcommand<>(helmLib, path, hl -> hl::DependencyList, DependencyListResult::parse);
  }

  /**
   * Update chart's on-disk dependencies (charts/) to mirror the contents of Chart.yaml.
   *
   * @return the {@link DependencySubcommand} subcommand.
   */
  public DependencySubcommand<String> update() {
    return new DependencySubcommand<>(helmLib, path, hl -> hl::DependencyUpdate, r -> r.out);
  }

  public static final class DependencySubcommand<T> extends HelmCommand<T> {

    private final Function<HelmLib, Function<DependencyOptions, Result>> callable;
    private final Function<Result, T> transformer;
    private final Path path;
    private Path keyring;
    private boolean skipRefresh;
    private boolean verify;
    private boolean debug;

    DependencySubcommand(
      HelmLib helmLib, Path path,
      Function<HelmLib, Function<DependencyOptions, Result>> callable, Function<Result, T> transformer
    ) {
      super(helmLib);
      this.callable = callable;
      this.transformer = transformer;
      this.path = path;
    }

    @Override
    public T call() {
      return transformer.apply(run(hl -> callable.apply(hl).apply(new DependencyOptions(
        toString(path),
        toString(keyring),
        toInt(skipRefresh),
        toInt(verify),
        toInt(debug)
      ))));
    }

    /**
     * Keyring containing public keys (default "~/.gnupg/pubring.gpg").
     *
     * @param keyring a {@link Path} with the keyring location.
     * @return this {@link DependencySubcommand} instance.
     */
    public DependencySubcommand<T> withKeyring(Path keyring) {
      this.keyring = keyring;
      return this;
    }

    /**
     * Do not refresh the local repository cache.
     *
     * @return this {@link DependencySubcommand} instance.
     */
    public DependencySubcommand<T> skipRefresh() {
      this.skipRefresh = true;
      return this;
    }

    /**
     * Verify the packages against signatures.
     *
     * @return this {@link DependencySubcommand} instance.
     */
    public DependencySubcommand<T> verify() {
      this.verify = true;
      return this;
    }

    /**
     * Enable verbose output.
     * <p>
     * The command execution output ({@link #call}) will include verbose debug messages.
     *
     * @return this {@link DependencySubcommand} instance.
     */
    public DependencySubcommand<T> debug() {
      this.debug = true;
      return this;
    }
  }
}
