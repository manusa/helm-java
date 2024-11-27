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

import com.marcnuri.helm.jni.CreateOptions;
import com.marcnuri.helm.jni.HelmLib;

import java.nio.file.Path;

/**
 * @author Marc Nuri
 */
public class CreateCommand extends HelmCommand<Helm> {

  private String name;
  private Path dir;

  public CreateCommand(HelmLib helmLib) {
    super(helmLib);
  }

  /**
   * Execute the create command.
   * @return a {@link Helm} instance pointing to the directory containing the chart files.
   */
  @Override
  public Helm call() {
    run(hl -> hl.Create(new CreateOptions(name, dir.normalize().toFile().getAbsolutePath())));
    return new Helm(dir.normalize().resolve(name));
  }

  /**
   * Name of the chart to create.
   * <p>
   * This will also be the name of the directory containing the chart.
   * @param name a {@link String} with the name of the chart.
   * @return this {@link CreateCommand} instance.
   */
  public CreateCommand withName(String name) {
    this.name = name;
    return this;
  }

  /**
   * Path to the directory where the directory containing the chart files will be created.
   * @param dir a {@link Path} to the directory.
   * @return this {@link CreateCommand} instance.
   */
  public CreateCommand withDir(Path dir) {
    this.dir = dir;
    return this;
  }
}
