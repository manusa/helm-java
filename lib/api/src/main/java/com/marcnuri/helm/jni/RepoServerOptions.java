package com.marcnuri.helm.jni;

import com.sun.jna.Structure;

@Structure.FieldOrder({"glob", "username", "password"})
public class RepoServerOptions extends Structure {
  public String glob;
  public String username;
  public String password;

  public RepoServerOptions() {
    this(null, null, null);
  }

  public RepoServerOptions(String glob, String username, String password) {
    this.glob = glob;
    this.username = username;
    this.password = password;
  }
}
