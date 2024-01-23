package com.marcnuri.helm.jni;

import com.sun.jna.Structure;

@Structure.FieldOrder({"chart", "remote", "certFile", "keyFile", "caFile", "insecureSkipTlsVerify", "plainHttp", "debug"})
public class PushOptions extends Structure {
  public String chart;
  public String remote;
  public String certFile;
  public String keyFile;
  public String caFile;
  public int insecureSkipTlsVerify;
  public int plainHttp;
  public int debug;

  public PushOptions(String chart, String remote, String certFile, String keyFile, String caFile, int insecureSkipTlsVerify, int plainHttp, int debug) {
    this.chart = chart;
    this.remote = remote;
    this.certFile = certFile;
    this.keyFile = keyFile;
    this.caFile = caFile;
    this.insecureSkipTlsVerify = insecureSkipTlsVerify;
    this.plainHttp = plainHttp;
    this.debug = debug;
  }
}
