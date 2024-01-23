package com.marcnuri.helm;

import com.marcnuri.helm.jni.NativeLibrary;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.net.URLClassLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NativeLibraryTest {

  @Test
  void getInstanceFromClassPath() {
    System.setProperty("com.marcnuri.jkube-helm.skipRemoteJar", "true");
    try {
      NativeLibrary nativeLibrary = NativeLibrary.getInstance();
      assertThat(nativeLibrary).isNotNull();
    } finally {
      System.clearProperty("com.marcnuri.jkube-helm.skipRemoteJar");
    }
  }

  @Test
  void getSnapshotInstanceFromRemoteJar() {
    final ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
    // Use a PUBLISHED snapshot version
    System.setProperty("com.marcnuri.jkube-helm.version", "0.0-SNAPSHOT");
    System.setProperty("com.marcnuri.jkube-helm.forceUpdate", "true");
    try {
      Thread.currentThread().setContextClassLoader(new URLClassLoader(new java.net.URL[0], null));
      assertThat(NativeLibrary.serviceProviderLibrary(null)).isNull();
      NativeLibrary nativeLibrary = NativeLibrary.getInstance();
      assertThat(nativeLibrary).isNotNull();
    } finally {
      Thread.currentThread().setContextClassLoader(currentClassLoader);
      System.clearProperty("com.marcnuri.jkube-helm.version");
    }
  }

  @Test
  @Disabled("Can only be enabled once we do an initial stable release") // TODO
  void getReleaseInstanceFromRemoteJar() {
    final ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
    // Use a PUBLISHED release version
    System.setProperty("com.marcnuri.jkube-helm.version", "0.0.0");
    System.setProperty("com.marcnuri.jkube-helm.forceUpdate", "true");
    try {
      Thread.currentThread().setContextClassLoader(new URLClassLoader(new java.net.URL[0], null));
      assertThat(NativeLibrary.serviceProviderLibrary(null)).isNull();
      NativeLibrary nativeLibrary = NativeLibrary.getInstance();
      assertThat(nativeLibrary).isNotNull();
    } finally {
      Thread.currentThread().setContextClassLoader(currentClassLoader);
      System.clearProperty("com.marcnuri.jkube-helm.version");
    }
  }

  @Test
  void getInstanceNotAvailable() {
    final ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
    System.setProperty("com.marcnuri.jkube-helm.skipRemoteJar", "true");
    try {
      Thread.currentThread().setContextClassLoader(new URLClassLoader(new java.net.URL[0], null));
      assertThatThrownBy(NativeLibrary::getInstance)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("No NativeLibrary implementation found, please add one of the supported dependencies to your project");
    } finally {
      System.clearProperty("com.marcnuri.jkube-helm.skipRemoteJar");
      Thread.currentThread().setContextClassLoader(currentClassLoader);
    }
  }
}
