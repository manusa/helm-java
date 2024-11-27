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
@Structure.FieldOrder({"hostname", "username", "password", "certFile", "keyFile", "caFile", "insecureSkipTlsVerify", "plainHttp", "debug"})
public class RegistryOptions extends Structure {
  public String hostname;
  public String username;
  public String password;
  public String certFile;
  public String keyFile;
  public String caFile;
  public int insecureSkipTlsVerify;
  public int plainHttp;
  public int debug;

  public RegistryOptions(String hostname, String username, String password, String certFile, String keyFile, String caFile, int insecureSkipTlsVerify, int plainHttp, int debug) {
    this.hostname = hostname;
    this.username = username;
    this.password = password;
    this.certFile = certFile;
    this.keyFile = keyFile;
    this.caFile = caFile;
    this.insecureSkipTlsVerify = insecureSkipTlsVerify;
    this.plainHttp = plainHttp;
    this.debug = debug;
  }
}
