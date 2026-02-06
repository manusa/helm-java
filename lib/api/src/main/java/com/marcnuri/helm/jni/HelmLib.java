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

package com.marcnuri.helm.jni;

import com.sun.jna.Library;

/**
 * @author Marc Nuri
 * @author Andres F. Vallecilla
 * @author Antonio Fernandez Alhambra
 */
public interface HelmLib extends Library {

  Result Create(CreateOptions options);

  Result DependencyBuild(DependencyOptions options);

  Result DependencyList(DependencyOptions options);

  Result DependencyUpdate(DependencyOptions options);

  Result Install(InstallOptions options);

  Result Lint(LintOptions options);

  Result List(ListOptions options);

  Result GetValues(GetValuesOptions options);

  Result Package(PackageOptions options);

  Result Push(PushOptions options);

  Result RegistryLogin(RegistryOptions options);

  Result RegistryLogout(RegistryOptions options);

  Result RepoAdd(RepoOptions options);

  Result RepoList(RepoOptions options);

  Result RepoRemove(RepoOptions options);

  Result RepoUpdate(RepoOptions options);

  Result RepoServerStart(RepoServerOptions options);

  Result RepoOciServerStart(RepoServerOptions options);

  Result RepoServerStop(String url);

  Result RepoServerStopAll();

  Result SearchRepo(SearchOptions options);

  Result Show(ShowOptions options);

  Result Template(TemplateOptions options);

  Result Test(TestOptions options);

  Result Uninstall(UninstallOptions options);

  Result Upgrade(UpgradeOptions options);

  Result Version();

  void Free(Result result);

}
