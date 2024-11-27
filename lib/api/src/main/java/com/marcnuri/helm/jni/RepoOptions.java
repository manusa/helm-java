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
@Structure.FieldOrder({"repositoryConfig", "name", "names", "url", "username", "password", "certFile", "keyFile", "caFile", "insecureSkipTlsVerify"})
public class RepoOptions extends Structure {
  public String repositoryConfig;
  public String name;
  public String names;
  public String url;
  public String username;
  public String password;
  public String certFile;
  public String keyFile;
  public String caFile;
  public int insecureSkipTlsVerify;

  public RepoOptions(String repositoryConfig, String name, String names, String url, String username, String password, String certFile, String keyFile, String caFile, int insecureSkipTlsVerify) {
    this.repositoryConfig = repositoryConfig;
    this.name = name;
    this.names = names;
    this.url = url;
    this.username = username;
    this.password = password;
    this.certFile = certFile;
    this.keyFile = keyFile;
    this.caFile = caFile;
    this.insecureSkipTlsVerify = insecureSkipTlsVerify;
  }
}
