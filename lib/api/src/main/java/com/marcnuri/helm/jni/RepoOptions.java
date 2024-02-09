package com.marcnuri.helm.jni;

import com.sun.jna.Structure;

@Structure.FieldOrder({"repositoryConfig"})
public class RepoOptions extends Structure {
  public String repositoryConfig;

  public RepoOptions(String repositoryConfig) {
    this.repositoryConfig = repositoryConfig;
  }
}
