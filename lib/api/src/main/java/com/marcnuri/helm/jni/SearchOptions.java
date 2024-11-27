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
@Structure.FieldOrder({"repositoryConfig", "keyword", "regexp", "devel", "version"})
public class SearchOptions extends Structure {

  public String repositoryConfig;
  public String keyword;
  public int regexp;
  public int devel;
  public String version;

  public SearchOptions(String repositoryConfig, String keyword, int regexp, int devel, String version) {
    this.repositoryConfig = repositoryConfig;
    this.keyword = keyword;
    this.regexp = regexp;
    this.devel = devel;
    this.version = version;
  }
}
