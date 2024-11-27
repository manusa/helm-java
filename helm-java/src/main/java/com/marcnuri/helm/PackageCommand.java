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
import com.marcnuri.helm.jni.PackageOptions;

import java.nio.file.Path;

/**
 * @author Marc Nuri
 */
public class PackageCommand extends HelmCommand<Helm> {

  private final Helm helm;
  private final Path path;
  private Path destination;
  private boolean sign;
  private String key;
  private Path keyring;
  private Path passphraseFile;

  public PackageCommand(HelmLib helmLib, Helm helm, Path path) {
    super(helmLib);
    this.helm = helm;
    this.path = path;
  }

  /**
   * Execute the package command.
   *
   * @return the current {@link Helm} instance for further chaining.
   */
  @Override
  public Helm call() {
    run(hl -> hl.Package(new PackageOptions(
      path.normalize().toFile().getAbsolutePath(),
      toString(destination),
      toInt(sign),
      key,
      toString(keyring),
      toString(passphraseFile)
    )));
    return helm;
  }

  /**
   * Location to write the chart  (default ".").
   *
   * @param destination a {@link String} with the location for the new package.
   * @return this {@link PackageCommand} instance.
   */
  public PackageCommand withDestination(Path destination) {
    this.destination = destination;
    return this;
  }

  /**
   * Use a PGP private key to sign this package.
   *
   * @return this {@link PackageCommand} instance.
   */
  public PackageCommand sign() {
    this.sign = true;
    return this;
  }

  /**
   * Name of the PGP private key to use when signing.
   * <p>
   * Required if {@link #sign()} is set.
   *
   * @param key a {@link String} with the PGP private key name.
   * @return this {@link PackageCommand} instance.
   */
  public PackageCommand withKey(String key) {
    this.key = key;
    return this;
  }

  /**
   * Location of a public keyring (default "~/.gnupg/pubring.gpg").
   *
   * @param keyring a {@link Path} with the keyring location.
   * @return this {@link PackageCommand} instance.
   */
  public PackageCommand withKeyring(Path keyring) {
    this.keyring = keyring;
    return this;
  }

  /**
   * Location of a file which contains the passphrase for the signing key.
   *
   * @param passphraseFile a {@link Path} with the passphrase file location.
   * @return this {@link PackageCommand} instance.
   */
  public PackageCommand withPassphraseFile(Path passphraseFile) {
    this.passphraseFile = passphraseFile;
    return this;
  }
}
