package com.marcnuri.helm.jni;

import com.sun.jna.Library;
import com.sun.jna.Structure;

public interface NativeHelm extends Library {

  @Structure.FieldOrder({"err"})
  class HelmResult extends Structure implements Structure.ByValue {
    public String err;
  }

  @Structure.FieldOrder({"name", "dir"})
  class CreateOptions extends Structure {
    public String name;
    public String dir;

    public CreateOptions(String name, String dir) {
      this.name = name;
      this.dir = dir;
    }
  }

  HelmResult Create(CreateOptions options);

}
