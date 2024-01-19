package com.marcnuri.helm.jni;

import com.sun.jna.Structure;

@Structure.FieldOrder({"err", "stdOut", "stdErr"})
public class Result extends Structure implements Structure.ByValue {
  public String err;
  public String stdOut;
  public String stdErr;
}
