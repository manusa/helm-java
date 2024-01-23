package com.marcnuri.helm.jni;

import com.sun.jna.Library;

public interface HelmLib extends Library {

  Result Create(CreateOptions options);

  Result Lint(LintOptions options);

  Result Package(PackageOptions options);

  Result Push(PushOptions options);

  Result RegistryLogin(RegistryLoginOptions options);

  Result RepoServerStart(RepoServerOptions options);

  Result RepoOciServerStart(RepoServerOptions options);

  Result RepoServerStop(String url);

  Result RepoServerStopAll();

  Result Show(ShowOptions options);

  Result Version();

  void Free(Result result);

}
