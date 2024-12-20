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

/**
 * @author Marc Nuri
 */
public class VersionCommand extends HelmCommand<String> {

  public VersionCommand(HelmLib helmLib) {
    super(helmLib);
  }

  /**
   * Execute the version command.
   * @return a {@link String} containing the underlying Helm library version.
   */
  @Override
  public String call() {
    return run(HelmLib::Version).out;
  }

}
