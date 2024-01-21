package com.marcnuri.helm.jni;

import com.sun.jna.Library;

public interface HelmLib extends Library {

  Result Create(CreateOptions options);

  Result Lint(LintOptions options);

  Result Package(PackageOptions options);

  Result Show(ShowOptions options);

  Result RepoTempServerStart(RepoServerOptions options);

  Result RepoServerStop(String url);

  Result RepoServerStopAll();

  Result Version();

  void Free(Result result);

}
