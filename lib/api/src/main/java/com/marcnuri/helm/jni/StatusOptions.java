/*
 * Copyright 2026 Marc Nuri
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
  "revision",
  "namespace",
  "kubeConfig",
  "kubeConfigContents"
})
public class StatusOptions extends Structure {
  public String releaseName;
  public int revision;
  public String namespace;
  public String kubeConfig;
  public String kubeConfigContents;

  public StatusOptions(String releaseName, int revision, String namespace, String kubeConfig, String kubeConfigContents) {
    this.releaseName = releaseName;
    this.revision = revision;
    this.namespace = namespace;
    this.kubeConfig = kubeConfig;
    this.kubeConfigContents = kubeConfigContents;
  }
}
