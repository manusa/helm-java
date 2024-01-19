package com.marcnuri.helm.jni;

import com.sun.jna.Structure;

@Structure.FieldOrder({"out", "err", "stdOut", "stdErr"})
public class Result extends Structure implements Structure.ByValue {
  public String out;
  public String err;
  public String stdOut;
  public String stdErr;
}
