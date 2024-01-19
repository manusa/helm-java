package com.marcnuri.helm.jni;

import com.sun.jna.Library;

public interface HelmLib extends Library {

  Result Create(CreateOptions options);

  void Free(Result result);

}
