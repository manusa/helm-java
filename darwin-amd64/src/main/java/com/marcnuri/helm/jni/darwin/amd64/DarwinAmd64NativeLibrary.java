package com.marcnuri.helm.jni.darwin.amd64;

import com.marcnuri.helm.jni.NativeLibrary;

public class DarwinAmd64NativeLibrary implements NativeLibrary {
  @Override
  public String getBinaryName() {
    return "helm-darwin-10.12-amd64.dylib";
  }
}
