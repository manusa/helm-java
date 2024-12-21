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
 * @author Christian Gebhard
 */
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
  "kubeConfig",
  "kubeConfigContents"
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
  public String kubeConfigContents;

  public ListOptions(int all, int allNamespaces, int deployed, int failed, int pending, int superseded, int uninstalled, int uninstalling, String namespace, String kubeConfig, String kubeConfigContents) {
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
    this.kubeConfigContents = kubeConfigContents;
  }
}
