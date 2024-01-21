package com.marcnuri.helm.jni;

import com.sun.jna.Structure;

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
