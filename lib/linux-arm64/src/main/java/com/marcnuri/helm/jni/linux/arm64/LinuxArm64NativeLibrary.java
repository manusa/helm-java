package com.marcnuri.helm.jni.linux.arm64;

import com.marcnuri.helm.jni.NativeLibrary;

public class LinuxArm64NativeLibrary implements NativeLibrary {
  @Override
  public String getBinaryName() {
    return "helm-linux-arm64.so";
  }
}
