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
	"fmt"
	"strings"

	"context"
	"helm.sh/helm/v3/pkg/action"
)

type TemplateOptions struct {
	CertOptions
	Name                     string
	Chart                    string
	Namespace                string
	DependencyUpdate         bool
	Values                   string
	KubeConfig               string
	Debug                    bool
}

func Template(options *TemplateOptions) (string, error) {
	registryClient, registryClientOut, err := newRegistryClient(
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
		RegistryClient: registryClient,
		KubeConfig:     options.KubeConfig,
		Namespace:      options.Namespace,
	}
	if options.Debug {
		cfgOptions.KubeOut = kubeOut
	}
	client := action.NewInstall(NewCfg(cfgOptions))

	// This is for the case where "" is specifically passed in as a
	// value. When there is no value passed in NoOptDefVal will be used
	// and it is set to client. See addInstallFlags.
	if client.DryRunOption == "" {
		client.DryRunOption = "true"
	}
	client.DryRun = true
	client.ReleaseName = options.Name
	client.Replace = true
	client.Namespace = options.Namespace
	client.CreateNamespace = true
	client.ClientOnly = true
	
	chartReference := options.Chart
	chartRequested, chartPath, err := loadChart(client.ChartPathOptions, chartReference)
	if err != nil {
		return "", err
	}
	// Dependency management
	chartRequested, updateOutput, err := updateDependencies(&updateDependenciesOptions{
		DependencyUpdate: options.DependencyUpdate,
		Keyring:          options.Keyring,
		Debug:            options.Debug,
	}, chartRequested, chartPath)

	ctx := context.Background()
	// Values
	var values, invalidValues = parseValues(options.Values)
	if invalidValues != nil {
		return "", invalidValues
	}
	// Run
	rel, err := client.RunWithContext(ctx, chartRequested, values)
	if err != nil {
		return "", err
	}
	// Generate report
	out := StatusReport(rel, false, options.Debug)
	appendToOutOrErr(concat(cStr(updateOutput), cBuf(registryClientOut), cBuf(kubeOut)), out, err)

	var manifests bytes.Buffer
	fmt.Fprintln(&manifests, strings.TrimSpace(rel.Manifest))
	return rel.Manifest,err
}