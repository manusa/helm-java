package com.marcnuri.helm;

import com.marcnuri.helm.jni.NativeHelm;
import com.marcnuri.helm.jni.NativeLibrary;

public class Helm {
  public static void main(String[] args) {
    NativeHelm.HelmResult success = NativeLibrary.getInstance().load().Create(new NativeHelm.CreateOptions("test", "/tmp"));
    System.out.printf("Success(?): %s%n", success.err);
    NativeHelm.HelmResult error = NativeLibrary.getInstance().load().Create(new NativeHelm.CreateOptions("test", "/im-an-invalid-path"));
    System.out.printf("Error(?): %s%n", error.err);
  }
}
