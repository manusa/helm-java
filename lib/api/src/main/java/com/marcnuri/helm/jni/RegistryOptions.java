package com.marcnuri.helm.jni;

import com.sun.jna.Structure;

@Structure.FieldOrder({"hostname", "username", "password", "certFile", "keyFile", "caFile", "insecureSkipTlsVerify", "plainHttp", "debug"})
public class RegistryOptions extends Structure {
  public String hostname;
  public String username;
  public String password;
  public String certFile;
  public String keyFile;
  public String caFile;
  public int insecureSkipTlsVerify;
  public int plainHttp;
  public int debug;

  public RegistryOptions(String hostname, String username, String password, String certFile, String keyFile, String caFile, int insecureSkipTlsVerify, int plainHttp, int debug) {
    this.hostname = hostname;
    this.username = username;
    this.password = password;
    this.certFile = certFile;
    this.keyFile = keyFile;
    this.caFile = caFile;
    this.insecureSkipTlsVerify = insecureSkipTlsVerify;
    this.plainHttp = plainHttp;
    this.debug = debug;
  }
}
