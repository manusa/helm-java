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
  "version",
  "chart",
  "dependencyUpdate",
  "values",
  "certFile",
  "keyFile",
  "caFile",
  "insecureSkipTlsVerify",
  "plainHttp",
  "keyring",
  "debug",
  "repositoryConfig"
})
public class TemplateOptions extends Structure {
  public String name;
  public String version;
  public String chart;
  public int dependencyUpdate;
  public String values;
  public String certFile;
  public String keyFile;
  public String caFile;
  public int insecureSkipTlsVerify;
  public int plainHttp;
  public String keyring;
  public int debug;
  public String repositoryConfig;

  public TemplateOptions(
    String name,
    String version,
    String chart,
    int dependencyUpdate,
    String values,
    String certFile,
    String keyFile,
    String caFile,
    int insecureSkipTlsVerify,
    int plainHttp,
    String keyring,
    int debug,
    String repositoryConfig
  ) {
    this.name = name;
    this.version = version;
    this.chart = chart;
    this.dependencyUpdate = dependencyUpdate;
    this.values = values;
    this.certFile = certFile;
    this.keyFile = keyFile;
    this.caFile = caFile;
    this.insecureSkipTlsVerify = insecureSkipTlsVerify;
    this.plainHttp = plainHttp;
    this.keyring = keyring;
    this.debug = debug;
    this.repositoryConfig = repositoryConfig;
  }
}
