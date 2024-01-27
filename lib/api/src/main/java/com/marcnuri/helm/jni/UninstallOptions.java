package com.marcnuri.helm.jni;

import com.sun.jna.Structure;

@Structure.FieldOrder({
  "releaseName",
  "dryRun",
  "noHooks",
  "ignoreNotFound",
  "keepHistory",
  "cascade",
  "namespace",
  "kubeConfig",
  "debug"
})
public class UninstallOptions extends Structure {
  public String releaseName;
  public int dryRun;
  public int noHooks;
  public int ignoreNotFound;
  public int keepHistory;
  public String cascade;
  public String namespace;
  public String kubeConfig;
  public int debug;

  public UninstallOptions(
    String releaseName,
    int dryRun,
    int noHooks,
    int ignoreNotFound,
    int keepHistory,
    String cascade,
    String namespace,
    String kubeConfig,
    int debug
  ) {
    this.releaseName = releaseName;
    this.dryRun = dryRun;
    this.noHooks = noHooks;
    this.ignoreNotFound = ignoreNotFound;
    this.keepHistory = keepHistory;
    this.cascade = cascade;
    this.namespace = namespace;
    this.kubeConfig = kubeConfig;
    this.debug = debug;
  }
}
