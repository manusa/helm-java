package com.marcnuri.helm.jni;

import com.sun.jna.Structure;

@Structure.FieldOrder({
  "name",
  "generateName",
  "nameTemplate",
  "chart",
  "namespace",
  "createNamespace",
  "description",
  "devel",
  "dryRun",
  "dryRunOption",
  "kubeConfig",
  "certFile",
  "keyFile",
  "caFile",
  "insecureSkipTLSverify",
  "plainHttp",
  "debug",
  "clientOnly"
})
public class InstallOptions extends Structure {

  public String name;
  public int generateName;
  public String nameTemplate;
  public String chart;
  public String namespace;
  public int createNamespace;
  public String description;
  public int devel;
  public int dryRun;
  public String dryRunOption;
  public String kubeConfig;
  public String certFile;
  public String keyFile;
  public String caFile;
  public int insecureSkipTLSverify;
  public int plainHttp;
  public int debug;
  public int clientOnly;

  public InstallOptions(
    String name,
    int  generateName,
    String nameTemplate,
    String chart,
    String namespace,
    int createNamespace,
    String description,
    int devel,
    int dryRun,
    String dryRunOption,
    String kubeConfig,
    String certFile,
    String keyFile,
    String caFile,
    int insecureSkipTLSverify,
    int plainHttp,
    int debug,
    int clientOnly
  ) {
    this.name = name;
    this.generateName = generateName;
    this.nameTemplate = nameTemplate;
    this.chart = chart;
    this.namespace = namespace;
    this.createNamespace = createNamespace;
    this.description = description;
    this.devel = devel;
    this.dryRun = dryRun;
    this.dryRunOption = dryRunOption;
    this.kubeConfig = kubeConfig;
    this.certFile = certFile;
    this.keyFile = keyFile;
    this.caFile = caFile;
    this.insecureSkipTLSverify = insecureSkipTLSverify;
    this.plainHttp = plainHttp;
    this.debug = debug;
    this.clientOnly = clientOnly;
  }
}
