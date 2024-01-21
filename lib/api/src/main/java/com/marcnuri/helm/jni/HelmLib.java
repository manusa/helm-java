package com.marcnuri.helm.jni;

import com.sun.jna.Library;

public interface HelmLib extends Library {

  Result Create(CreateOptions options);

  Result Lint(LintOptions options);

  Result Package(PackageOptions options);

  Result Show(ShowOptions options);

  Result TestRepoServerStart();

  Result TestRepoServerStop(String url);

  Result TestRepoServerStopAll();

  Result Version();

  void Free(Result result);

}
