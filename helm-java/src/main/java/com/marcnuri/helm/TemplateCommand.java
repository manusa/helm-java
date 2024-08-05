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
import com.marcnuri.helm.jni.TemplateOptions;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TemplateCommand extends HelmCommand<String> {

  private String name;
  private String chart;
  private String namespace;
  private boolean dependencyUpdate;
  private final Map<String, String> values;
  private Path kubeConfig;
  private boolean debug;


  public TemplateCommand(HelmLib helmLib, String chart) {
    this(helmLib, "release-name", chart);
  }

  public TemplateCommand(HelmLib helmLib, String name, String chart) {
    super(helmLib);
    this.name = name;
    this.chart = chart;
    this.values = new LinkedHashMap<>();
  }

  @Override
  public String call() {
    return run(hl -> hl.Template(new TemplateOptions(
      name,
      chart,
      namespace,
      toInt(dependencyUpdate),
      urlEncode(values),
      toString(kubeConfig),
      toInt(debug)
    ))).out;
  }

  /**
   * Set values for the chart.
   *
   * @param key   the key.
   * @param value the value for this key.
   * @return this {@link TemplateCommand} instance.
   */
  public TemplateCommand set(String key, Object value) {
    this.values.put(key, value == null ? "" : value.toString());
    return this;
  }

  /**
   * Name for the release.
   *
   * @param name for the release.
   * @return this {@link TemplateCommand} instance.
   */
  public TemplateCommand withName(String name) {
    this.name = name;
    return this;
  }

  /**
   * Enable verbose output.
   * <p>
   * The command execution output ({@link #call}) will include verbose debug messages.
   *
   * @return this {@link TemplateCommand} instance.
   */
  public TemplateCommand debug() {
    this.debug = true;
    return this;
  }

}
