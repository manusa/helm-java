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
   * Update chart's on-disk dependencies (charts/) to mirror the contents of Chart.yaml.
   *
   * @return the {@link DependencySubcommand} subcommand.
   */
  public DependencySubcommand update() {
    return new DependencySubcommand(helmLib, hl -> hl::DependencyUpdate, path);
  }

  public static final class DependencySubcommand extends HelmCommand<String> {

    private final Function<HelmLib, Function<DependencyOptions, Result>> callable;
    private final Path path;
    private Path keyring;
    private boolean skipRefresh;
    private boolean verify;
    private boolean debug;

    DependencySubcommand(HelmLib helmLib, Function<HelmLib, Function<DependencyOptions, Result>> callable, Path path) {
      super(helmLib);
      this.callable = callable;
      this.path = path;
    }

    @Override
    public String call() {
      return run(hl -> callable.apply(hl).apply(new DependencyOptions(
        toString(path),
        toString(keyring),
        toInt(skipRefresh),
        toInt(verify),
        toInt(debug)
      ))).out;
    }

    /**
     * Keyring containing public keys (default "~/.gnupg/pubring.gpg").
     *
     * @param keyring a {@link Path} with the keyring location.
     * @return this {@link DependencySubcommand} instance.
     */
    public DependencySubcommand withKeyring(Path keyring) {
      this.keyring = keyring;
      return this;
    }

    /**
     * Do not refresh the local repository cache.
     *
     * @return this {@link DependencySubcommand} instance.
     */
    public DependencySubcommand skipRefresh() {
      this.skipRefresh = true;
      return this;
    }

    /**
     * Verify the packages against signatures.
     *
     * @return this {@link DependencySubcommand} instance.
     */
    public DependencySubcommand verify() {
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
    public DependencySubcommand debug() {
      this.debug = true;
      return this;
    }
  }
}
