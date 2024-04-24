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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class HelmPackageTest {

  @TempDir
  private Path tempDir;
  private Path keyring;
  private Path protectedKeyring;
  private Path protectedKeyringPassphrase;
  private Helm helm;

  @BeforeEach
  void setUp() throws IOException {
    // In test resources directory
    // gpg --no-default-keyring --keyring ./gpg-keyring.secret --full-generate-key
    // Use RSA (helm doesn't support ECDSA)
    // Don't set passphrase
    // gpg --no-default-keyring --keyring ./gpg-keyring.secret --export-secret-keys > ./keyring.secret.gpg
    keyring = new File(Objects.requireNonNull(HelmPackageTest.class.getResource("/keyring.secret.gpg")).getFile())
      .toPath();
    // In test resources directory
    // gpg --no-default-keyring --keyring ./gpg-keyring-passphrase.secret --full-generate-key
    // Use RSA (helm doesn't support ECDSA)
    // gpg --no-default-keyring --keyring ./gpg-keyring-passphrase.secret --export-secret-keys > ./keyring.secret.passphrase.gpg
    protectedKeyring = new File(Objects.requireNonNull(HelmPackageTest.class.getResource("/keyring.secret.passphrase.gpg")).getFile())
      .toPath();
    protectedKeyringPassphrase = Files.write(tempDir.resolve("passphrase"),
      "passphrase".getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);

    helm = Helm.create().withName("test").withDir(tempDir).call();

  }
  @Test
  void withoutChart() {
    final PackageCommand packageCommand = new Helm(Paths.get("")).packageIt();
    assertThatIllegalStateException()
      .isThrownBy(packageCommand::call)
      .withMessage("Chart.yaml file is missing");
  }

  @Test
  void withSignAndNoKey() {
    final PackageCommand packageCommand = helm.packageIt().sign();
    assertThatIllegalStateException()
      .isThrownBy(packageCommand::call)
      .withMessage("--key is required for signing a package");
  }

  @Test
  void withSignAndKeyAndNoKeyring() {
    final PackageCommand packageCommand = helm.packageIt()
      .sign().withKey("key");
    assertThatIllegalStateException()
      .isThrownBy(packageCommand::call)
      .withMessage("--keyring is required for signing a package");
  }

  @Test
  void withSignAndKeyAndKeyringAndMissingKey() {
    final PackageCommand packageCommand = helm.packageIt().withDestination(tempDir)
      .sign().withKey("NOT-THERE").withKeyring(keyring);
    assertThatIllegalStateException()
      .isThrownBy(packageCommand::call)
      .withMessage("private key not found");
  }

  @Test
  void withSignAndKeyAndKeyringAndNoPassphraseFile() {
    final PackageCommand packageCommand = helm.packageIt().withDestination(tempDir)
      .sign().withKey("KEY <KEY@example.com>").withKeyring(protectedKeyring);
    assertThatIllegalStateException()
      .isThrownBy(packageCommand::call);
  }

  @Test
  void valid() {
    helm.packageIt().withDestination(tempDir).call();
    assertThat(tempDir)
      .isDirectoryContaining(p -> p.toFile().getName().equals("test-0.1.0.tgz"));
  }
  @Test
  void validSign() {
    helm.packageIt().withDestination(tempDir)
      .sign().withKey("KEY <KEY@example.com>").withKeyring(keyring).call();
    assertThat(tempDir)
      .isDirectoryContaining(p -> p.toFile().getName().equals("test-0.1.0.tgz"));
  }

  @Test
  void validSignPassphrase() {
    helm.packageIt().withDestination(tempDir)
      .sign().withKey("KEY <KEY@example.com>").withKeyring(protectedKeyring)
      .withPassphraseFile(protectedKeyringPassphrase)
      .call();
    assertThat(tempDir)
      .isDirectoryContaining(p -> p.toFile().getName().equals("test-0.1.0.tgz"));
  }
}
