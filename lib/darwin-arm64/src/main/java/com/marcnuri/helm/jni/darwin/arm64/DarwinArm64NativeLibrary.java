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

package com.marcnuri.helm.jni.darwin.arm64;

import com.marcnuri.helm.jni.NativeLibrary;

public class DarwinArm64NativeLibrary implements NativeLibrary {
  @Override
  public String getBinaryName() {
    return "helm-darwin-10.12-arm64.dylib";
  }
}
