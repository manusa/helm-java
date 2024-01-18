package com.marcnuri.helm.jni;

import com.sun.jna.Structure;

@Structure.FieldOrder({"name", "dir"})
public class CreateOptions extends Structure {
  public String name;
  public String dir;

  public CreateOptions(String name, String dir) {
    this.name = name;
    this.dir = dir;
  }
}
