package com.marcnuri.helm.jni;

import com.sun.jna.Structure;

@Structure.FieldOrder({"path", "strict", "quiet"})
public class LintOptions extends Structure {
  public String path;
  public int  strict;
  public int  quiet;

  public LintOptions(String path, int strict, int quiet) {
    this.path = path;
    this.strict = strict;
    this.quiet = quiet;
  }
}
