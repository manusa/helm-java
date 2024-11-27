/*
 * Copyright 2024 Marc Nuri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.marcnuri.helm;

import com.marcnuri.helm.jni.NativeLibrary;
import org.junit.jupiter.api.Test;

import java.net.URLClassLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Marc Nuri
 */
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
  void getReleaseInstanceFromRemoteJar() {
    final ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
    // Use a PUBLISHED release version
    System.setProperty("com.marcnuri.jkube-helm.version", "0.0.1");
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
