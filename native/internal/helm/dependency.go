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

package helm

import (
	"bytes"
	"helm.sh/helm/v3/pkg/action"
	"helm.sh/helm/v3/pkg/cli"
	"helm.sh/helm/v3/pkg/downloader"
	"helm.sh/helm/v3/pkg/getter"
	"helm.sh/helm/v3/pkg/registry"
)

type DependencyOptions struct {
	Path        string
	Keyring     string
	SkipRefresh bool
	Verify      bool
	Debug       bool
}

func DependencyBuild(options *DependencyOptions) (string, error) {
	registryClient, getRegistryClientOut, err := newRegistryClient(
		"", "", "", false, false,
		options.Debug,
	)
	if err != nil {
		return "", err
	}
	manager, out := newManager(options, registryClient)
	if options.Verify {
		// https://github.com/helm/helm/blob/1135392b482f26f244c3c69f51511a1d82590eb7/cmd/helm/dependency_build.go#L69
		manager.Verify = downloader.VerifyIfPossible
	}
	err = manager.Build() // needs to be evaluated first so that out gets populated during the update
	return appendToOutOrErr(getRegistryClientOut(), out.String(), err)
}

func DependencyList(options *DependencyOptions) (string, error) {
	client := action.NewDependency()
	out := bytes.NewBuffer(make([]byte, 0))
	err := client.List(options.Path, out)
	return out.String(), err
}

func DependencyUpdate(options *DependencyOptions) (string, error) {
	registryClient, getRegistryClientOut, err := newRegistryClient(
		"", "", "", false, false,
		options.Debug,
	)
	if err != nil {
		return "", err
	}
	manager, out := newManager(options, registryClient)
	if options.Verify {
		// https://github.com/helm/helm/blob/3ad08f3ea9c09d16ddf6519d65f3f6f2ceee2c37/cmd/helm/dependency_update.go#L72
		manager.Verify = downloader.VerifyAlways
	}
	err = manager.Update() // needs to be evaluated first so that out gets populated during the update
	return appendToOutOrErr(getRegistryClientOut(), out.String(), err)
}

func newManager(options *DependencyOptions, registryClient *registry.Client) (*downloader.Manager, *bytes.Buffer) {
	out := bytes.NewBuffer(make([]byte, 0))
	settings := cli.New()
	manager := &downloader.Manager{
		Out:            out,
		ChartPath:      options.Path,
		Keyring:        options.Keyring,
		SkipUpdate:     options.SkipRefresh,
		RegistryClient: registryClient,
		Debug:          options.Debug,
		Getters:        getter.All(settings),
	}
	return manager, out
}
