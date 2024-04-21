package com.marcnuri.helm.jni;

import com.sun.jna.Library;

public interface HelmLib extends Library {

  Result Create(CreateOptions options);

  Result DependencyBuild(DependencyOptions options);

  Result DependencyList(DependencyOptions options);

  Result DependencyUpdate(DependencyOptions options);

  Result Install(InstallOptions options);

  Result Lint(LintOptions options);

  Result List(ListOptions options);

  Result Package(PackageOptions options);

  Result Push(PushOptions options);

  Result RegistryLogin(RegistryOptions options);

  Result RegistryLogout(RegistryOptions options);

  Result RepoAdd(RepoOptions options);

  Result RepoList(RepoOptions options);

  Result RepoRemove(RepoOptions options);

  Result RepoServerStart(RepoServerOptions options);

  Result RepoOciServerStart(RepoServerOptions options);

  Result RepoServerStop(String url);

  Result RepoServerStopAll();

  Result SearchRepo(SearchOptions options);

  Result Show(ShowOptions options);

  Result Test(TestOptions options);

  Result Uninstall(UninstallOptions options);

  Result Upgrade(UpgradeOptions options);

  Result Version();

  void Free(Result result);

}
