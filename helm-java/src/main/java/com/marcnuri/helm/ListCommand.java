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

package com.marcnuri.helm;

import com.marcnuri.helm.jni.HelmLib;
import com.marcnuri.helm.jni.ListOptions;

import java.nio.file.Path;
import java.util.List;

import static com.marcnuri.helm.Release.parseMultiple;

/**
 * @author Marc Nuri
 */
public class ListCommand extends HelmCommand<List<Release>> {

  private boolean all;
  private boolean allNamespaces;
  private boolean deployed;
  private boolean failed;
  private boolean pending;
  private boolean superseded;
  private boolean uninstalled;
  private boolean uninstalling;
  private String namespace;
  private Path kubeConfig;

  public ListCommand(HelmLib helmLib) {
    super(helmLib);
  }

  @Override
  public List<Release> call() {
    return parseMultiple(run(hl -> hl.List(new ListOptions(
      toInt(all),
      toInt(allNamespaces),
      toInt(deployed),
      toInt(failed),
      toInt(pending),
      toInt(superseded),
      toInt(uninstalled),
      toInt(uninstalling),
      namespace,
      toString(kubeConfig)
    ))));
  }

  /**
   * Show all releases without any filter applied.
   *
   * @return this {@link ListCommand} instance.
   */
  public ListCommand all() {
    this.all = true;
    return this;
  }

  /**
   * List releases across all namespaces.
   *
   * @return this {@link ListCommand} instance.
   */
  public ListCommand allNamespaces() {
    this.allNamespaces = true;
    return this;
  }

  /**
   * Show deployed releases. If no other option is specified, this will be automatically enabled.
   *
   * @return this {@link ListCommand} instance.
   */
  public ListCommand deployed() {
    this.deployed = true;
    return this;
  }

  /**
   * Show failed releases.
   *
   * @return this {@link ListCommand} instance.
   */
  public ListCommand failed() {
    this.failed = true;
    return this;
  }

  /**
   * Show pending releases.
   *
   * @return this {@link ListCommand} instance.
   */
  public ListCommand pending() {
    this.pending = true;
    return this;
  }

  /**
   * Show superseded releases.
   *
   * @return this {@link ListCommand} instance.
   */
  public ListCommand superseded() {
    this.superseded = true;
    return this;
  }

  /**
   * Show uninstalled releases (if 'helm uninstall --keep-history' was used).
   *
   * @return this {@link ListCommand} instance.
   */
  public ListCommand uninstalled() {
    this.uninstalled = true;
    return this;
  }

  /**
   * Show releases that are currently being uninstalled.
   *
   * @return this {@link ListCommand} instance.
   */
  public ListCommand uninstalling() {
    this.uninstalling = true;
    return this;
  }

  /**
   * Kubernetes namespace scope for this request.
   *
   * @param namespace the Kubernetes namespace for this request.
   * @return this {@link ListCommand} instance.
   */
  public ListCommand withNamespace(String namespace) {
    this.namespace = namespace;
    return this;
  }

  /**
   * Set the path ./kube/config file to use.
   *
   * @param kubeConfig the path to kube config file.
   * @return this {@link ListCommand} instance.
   */
  public ListCommand withKubeConfig(Path kubeConfig) {
    this.kubeConfig = kubeConfig;
    return this;
  }
}
