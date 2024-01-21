package com.marcnuri.helm.jni;

import com.sun.jna.Library;

public interface HelmLib extends Library {

  Result Create(CreateOptions options);

  Result Lint(LintOptions options);

  Result Show(ShowOptions options);

  Result Package(PackageOptions options);

  Result Version();

  void Free(Result result);

}
