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

@Structure.FieldOrder({"path", "outputFormat", "certFile", "keyFile", "caFile", "insecureSkipTlsVerify", "plainHttp", "debug"})
public class ShowOptions extends Structure {
  public String path;
  public String outputFormat;
  public String certFile;
  public String keyFile;
  public String caFile;
  public int insecureSkipTlsVerify;
  public int plainHttp;
  public int debug;

  public ShowOptions(String path, String outputFormat, String certFile, String keyFile, String caFile, int insecureSkipTlsVerify, int plainHttp, int debug) {
    this.path = path;
    this.outputFormat = outputFormat;
    this.certFile = certFile;
    this.keyFile = keyFile;
    this.caFile = caFile;
    this.insecureSkipTlsVerify = insecureSkipTlsVerify;
    this.plainHttp = plainHttp;
    this.debug = debug;
  }
}
