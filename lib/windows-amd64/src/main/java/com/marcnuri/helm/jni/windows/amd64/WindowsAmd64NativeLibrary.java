package com.marcnuri.helm.jni.windows.amd64;

import com.marcnuri.helm.jni.NativeLibrary;

public class WindowsAmd64NativeLibrary implements NativeLibrary {
  @Override
  public String getBinaryName() {
    return "helm-windows-4.0-amd64.dll";
  }
}
