package com.marcnuri.helm.jni;

import com.sun.jna.Structure;

@Structure.FieldOrder({"path", "outputFormat"})
public class ShowOptions extends Structure {
  public String path;
  public String outputFormat;

  public ShowOptions(String path, String outputFormat) {
    this.path = path;
    this.outputFormat = outputFormat;
  }
}
