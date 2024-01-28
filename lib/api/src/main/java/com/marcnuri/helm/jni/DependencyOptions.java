package com.marcnuri.helm.jni;

import com.sun.jna.Structure;

@Structure.FieldOrder({
  "path",
  "keyring",
  "skipRefresh",
  "verify",
  "debug"
})
public class DependencyOptions extends Structure {
  public String path;
  public String keyring;
  public int skipRefresh;
  public int verify;
  public int debug;

  public DependencyOptions(String path, String keyring, int skipRefresh, int verify, int debug) {
    this.path = path;
    this.keyring = keyring;
    this.skipRefresh = skipRefresh;
    this.verify = verify;
    this.debug = debug;
  }
}
