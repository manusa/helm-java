package com.marcnuri.helm.jni;

import com.sun.jna.Structure;

@Structure.FieldOrder({
  "releaseName",
  "timeout",
  "namespace",
  "kubeConfig",
  "debug"
})
public class TestOptions extends Structure {
  public String releaseName;
  public int timeout;
  public String namespace;
  public String kubeConfig;
  public int debug;

  public TestOptions(
    String releaseName,
    int timeout,
    String namespace,
    String kubeConfig,
    int debug
  ) {
    this.releaseName = releaseName;
    this.timeout = timeout;
    this.namespace = namespace;
    this.kubeConfig = kubeConfig;
    this.debug = debug;
  }

}
