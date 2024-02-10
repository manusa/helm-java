package com.marcnuri.helm.jni;

import com.sun.jna.Structure;

@Structure.FieldOrder({"repositoryConfig", "name", "url", "username", "password", "certFile", "keyFile", "caFile", "insecureSkipTlsVerify"})
public class RepoOptions extends Structure {
  public String repositoryConfig;
  public String name;
  public String url;
  public String username;
  public String password;
  public String certFile;
  public String keyFile;
  public String caFile;
  public int insecureSkipTlsVerify;

  public RepoOptions(String repositoryConfig, String name, String url, String username, String password, String certFile, String keyFile, String caFile, int insecureSkipTlsVerify) {
    this.repositoryConfig = repositoryConfig;
    this.name = name;
    this.url = url;
    this.username = username;
    this.password = password;
    this.certFile = certFile;
    this.keyFile = keyFile;
    this.caFile = caFile;
    this.insecureSkipTlsVerify = insecureSkipTlsVerify;
  }
}
