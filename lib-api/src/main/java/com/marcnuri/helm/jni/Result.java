package com.marcnuri.helm.jni;

import com.sun.jna.Structure;

@Structure.FieldOrder({"err"})
public class Result extends Structure implements Structure.ByValue {
  public String err;
}
