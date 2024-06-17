/*
 * Copyright 2024 Marc Nuri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.marcnuri.helm.jni;

import com.sun.jna.Structure;

@Structure.FieldOrder({
  "name",
  "chart",
  "namespace",
  "install",
  "force",
  "resetValues",
  "reuseValues",
  "resetThenReuseValues",
  "atomic",
  "cleanupOnFail",
  "createNamespace",
  "description",
  "devel",
  "dependencyUpdate",
  "disableOpenApiValidation",
  "dryRun",
  "dryRunOption",
  "wait",
  "values",
  "kubeConfig",
  "certFile",
  "keyFile",
  "caFile",
  "insecureSkipTlsVerify",
  "plainHttp",
  "keyring",
  "debug",
  "clientOnly"
})
public class UpgradeOptions extends Structure {
  public String name;
  public String chart;
  public String namespace;
  public int install;
  public int force;
  public int resetValues;
  public int reuseValues;
  public int resetThenReuseValues;
  public int atomic;
  public int cleanupOnFail;
  public int createNamespace;
  public String description;
  public int devel;
  public int dependencyUpdate;
  public int disableOpenApiValidation;
  public int dryRun;
  public String dryRunOption;
  public int wait;
  public String values;
  public String kubeConfig;
  public String certFile;
  public String keyFile;
  public String caFile;
  public int insecureSkipTlsVerify;
  public int plainHttp;
  public String keyring;
  public int debug;
  public int clientOnly;

  public UpgradeOptions(
    String name,
    String chart,
    String namespace,
    int install,
    int force,
    int resetValues,
    int reuseValues,
    int resetThenReuseValues,
    int atomic,
    int cleanupOnFail,
    int createNamespace,
    String description,
    int devel,
    int dependencyUpdate,
    int disableOpenApiValidation,
    int dryRun,
    String dryRunOption,
    int wait,
    String values,
    String kubeConfig,
    String certFile,
    String keyFile,
    String caFile,
    int insecureSkipTlsVerify,
    int plainHttp,
    String keyring,
    int debug,
    int clientOnly
  ) {
    this.name = name;
    this.chart = chart;
    this.namespace = namespace;
    this.install = install;
    this.force = force;
    this.resetValues = resetValues;
    this.reuseValues = reuseValues;
    this.resetThenReuseValues = resetThenReuseValues;
    this.atomic = atomic;
    this.cleanupOnFail = cleanupOnFail;
    this.createNamespace = createNamespace;
    this.description = description;
    this.devel = devel;
    this.dependencyUpdate = dependencyUpdate;
    this.disableOpenApiValidation = disableOpenApiValidation;
    this.dryRun = dryRun;
    this.dryRunOption = dryRunOption;
    this.wait = wait;
    this.values = values;
    this.kubeConfig = kubeConfig;
    this.certFile = certFile;
    this.keyFile = keyFile;
    this.caFile = caFile;
    this.insecureSkipTlsVerify = insecureSkipTlsVerify;
    this.plainHttp = plainHttp;
    this.keyring = keyring;
    this.debug = debug;
    this.clientOnly = clientOnly;
  }
}
