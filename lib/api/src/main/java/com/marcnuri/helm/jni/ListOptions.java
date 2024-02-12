package com.marcnuri.helm.jni;

import com.sun.jna.Structure;

@Structure.FieldOrder({
  "all",
  "allNamespaces",
  "deployed",
  "failed",
  "pending",
  "superseded",
  "uninstalled",
  "uninstalling",
  "namespace",
  "kubeConfig"
})
public class ListOptions extends Structure {
  public int all;
  public int allNamespaces;
  public int deployed;
  public int failed;
  public int pending;
  public int superseded;
  public int uninstalled;
  public int uninstalling;
  public String namespace;
  public String kubeConfig;

  public ListOptions(int all, int allNamespaces, int deployed, int failed, int pending, int superseded, int uninstalled, int uninstalling, String namespace, String kubeConfig) {
    this.all = all;
    this.allNamespaces = allNamespaces;
    this.deployed = deployed;
    this.failed = failed;
    this.pending = pending;
    this.superseded = superseded;
    this.uninstalled = uninstalled;
    this.uninstalling = uninstalling;
    this.namespace = namespace;
    this.kubeConfig = kubeConfig;
  }
}
