package com.marcnuri.helm.jni.darwin.arm64;

import com.marcnuri.helm.jni.NativeLibrary;

public class DarwinArm64NativeLibrary implements NativeLibrary {
  @Override
  public String getBinaryName() {
    return "helm-darwin-10.12-arm64.dylib";
  }
}
