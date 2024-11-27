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

/**
 * @author Marc Nuri
 */
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
