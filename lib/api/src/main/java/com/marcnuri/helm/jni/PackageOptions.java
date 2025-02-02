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

package com.marcnuri.helm.jni;

import com.sun.jna.Structure;

/**
 * @author Marc Nuri
 */
@Structure.FieldOrder({"path", "destination", "sign", "key", "keyring", "passphraseFile"})
public class PackageOptions extends Structure {
  public String path;
  public String destination;
  public int sign;
  public String key;
  public String keyring;
  public String passphraseFile;

  public PackageOptions(String path, String destination, int sign, String key, String keyring, String passphraseFile) {
    this.path = path;
    this.destination = destination;
    this.sign = sign;
    this.key = key;
    this.keyring = keyring;
    this.passphraseFile = passphraseFile;
  }
}
