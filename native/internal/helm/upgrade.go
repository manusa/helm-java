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
	"context"
	"helm.sh/helm/v3/pkg/action"
	"helm.sh/helm/v3/pkg/storage/driver"
	"time"
)

type UpgradeOptions struct {
	CertOptions
	Name                     string
	Version                  string
	Chart                    string
	Namespace                string
	Install                  bool
	Force                    bool
	ResetValues              bool
	ReuseValues              bool
	ResetThenReuseValues     bool
	Atomic                   bool
	CleanupOnFail            bool
	CreateNamespace          bool
	Description              string
	Devel                    bool
	DependencyUpdate         bool
	DisableOpenApiValidation bool
	DryRun                   bool
	DryRunOption             string
	Wait                     bool
	Timeout                  time.Duration
	Values                   string
	ValuesFiles              string
	KubeConfig               string
	KubeConfigContents       string
	Debug                    bool
	// For testing purposes only, prevents connecting to Kubernetes (happens even with DryRun=true and DryRunOption=client)
	ClientOnly       bool
	RepositoryConfig string
}

func Upgrade(options *UpgradeOptions) (string, error) {
	registryClient, getRegistryClientOut, err := newRegistryClient(
		options.CertFile,
		options.KeyFile,
		options.CaFile,
		options.InsecureSkipTLSverify,
		options.PlainHttp,
		options.Debug,
	)
	if err != nil {
		return "", err
	}
	kubeOut := bytes.NewBuffer(make([]byte, 0))
	cfgOptions := &CfgOptions{
		RegistryClient:     registryClient,
		KubeConfig:         options.KubeConfig,
		KubeConfigContents: options.KubeConfigContents,
		Namespace:          options.Namespace,
	}
	if options.Debug {
		cfgOptions.KubeOut = kubeOut
	}
	cfg := NewCfg(cfgOptions)

	// Install if release doesn't exist
	if options.Install {
		histClient := action.NewHistory(cfg)
		histClient.Max = 1
		if _, err := histClient.Run(options.Name); err == driver.ErrReleaseNotFound {
			return Install(&InstallOptions{
				Name:                     options.Name,
				GenerateName:             false,
				NameTemplate:             "",
				Version:                  options.Version,
				Chart:                    options.Chart,
				Namespace:                options.Namespace,
				CreateNamespace:          options.CreateNamespace,
				Description:              options.Description,
				Devel:                    options.Devel,
				DependencyUpdate:         options.DependencyUpdate,
				DisableOpenApiValidation: options.DisableOpenApiValidation,
				DryRun:                   options.DryRun,
				DryRunOption:             options.DryRunOption,
				Wait:                     options.Wait,
				Timeout:                  options.Timeout,
				Values:                   options.Values,
				KubeConfig:               options.KubeConfig,
				CertOptions:              options.CertOptions,
				Debug:                    options.Debug,
				ClientOnly:               options.ClientOnly,
			})
		} else if err != nil {
			return "", err
		}
	}

	if options.Version == "" && options.Devel {
		options.Version = ">0.0.0-0"
	}
	client := action.NewUpgrade(cfg)
	client.Version = options.Version
	client.Namespace = options.Namespace
	client.Install = options.Install
	client.Force = options.Force
	client.ResetValues = options.ResetValues
	client.ReuseValues = options.ReuseValues
	client.ResetThenReuseValues = options.ResetThenReuseValues
	client.Atomic = options.Atomic
	client.CleanupOnFail = options.CleanupOnFail
	client.Description = options.Description
	client.Devel = options.Devel
	client.DisableOpenAPIValidation = options.DisableOpenApiValidation
	client.DryRun = options.DryRun
	client.DryRunOption = dryRunOption(options.DryRunOption)
	client.Wait = options.Wait
	client.Timeout = options.Timeout
	client.CertFile = options.CertFile
	client.KeyFile = options.KeyFile
	client.CaFile = options.CaFile
	client.InsecureSkipTLSverify = options.InsecureSkipTLSverify
	client.PlainHTTP = options.PlainHttp
	client.Keyring = options.Keyring

	chartReference := options.Chart
	chartRequested, chartPath, err := loadChart(client.ChartPathOptions, options.RepositoryConfig, chartReference)
	if err != nil {
		return "", err
	}
	// Dependency management
	chartRequested, updateOutput, err := updateDependencies(&updateDependenciesOptions{
		DependencyUpdate: options.DependencyUpdate,
		Keyring:          options.Keyring,
		Debug:            options.Debug,
	}, chartRequested, chartPath)
	if err != nil {
		return "", err
	}
	// Dry Run options
	if invalidDryRun := validateDryRunOptionFlag(client.DryRunOption); invalidDryRun != nil {
		return "", invalidDryRun
	}
	ctx := context.Background()
	// Values
	vals, err := mergeValues(options.Values, options.ValuesFiles)
	if err != nil {
		return "", err
	}
	// Run
	release, err := client.RunWithContext(ctx, options.Name, chartRequested, vals)
	// Generate report
	out := StatusReport(release, false, options.Debug)
	return appendToOutOrErr(concat(cStr(updateOutput), cBuf(getRegistryClientOut()), cBuf(kubeOut)), out, err)
}
