package com.marcnuri.helm.jni.linux.amd64;

import com.marcnuri.helm.jni.NativeLibrary;

public class LinuxAmd64NativeLibrary implements NativeLibrary {
  @Override
  public String getBinaryName() {
    return "helm-linux-amd64.so";
  }
}
