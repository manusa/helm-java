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
	"fmt"
	"github.com/pkg/errors"
	"helm.sh/helm/v3/pkg/action"
	"helm.sh/helm/v3/pkg/chart"
	"helm.sh/helm/v3/pkg/chart/loader"
	"helm.sh/helm/v3/pkg/cli"
	"helm.sh/helm/v3/pkg/cli/values"
	"helm.sh/helm/v3/pkg/getter"
	"helm.sh/helm/v3/pkg/release"
	"net/url"
	"os"
	"os/signal"
	"slices"
	"strings"
	"syscall"
	"time"
)

type InstallOptions struct {
	CertOptions
	Name                     string
	GenerateName             bool
	NameTemplate             string
	Version                  string
	Chart                    string
	Namespace                string
	Atomic                   bool
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
	Debug                    bool
	// For testing purposes only, prevents connecting to Kubernetes (happens even with DryRun=true and DryRunOption=client)
	ClientOnly       bool
	RepositoryConfig string
}

type installOutputs struct {
	updateOutput      string
	registryClientOut *bytes.Buffer
	kubeOut           *bytes.Buffer
}

func Install(options *InstallOptions) (string, error) {
	rel, outputs, err := install(options)
	// Generate report
	out := StatusReport(rel, false, options.Debug)
	return appendToOutOrErr(concat(cStr(outputs.updateOutput), cBuf(outputs.registryClientOut), cBuf(outputs.kubeOut)), out, err)
}

func install(options *InstallOptions) (*release.Release, *installOutputs, error) {
	outputs := &installOutputs{
		kubeOut: bytes.NewBuffer(make([]byte, 0)),
	}
	if options.Version == "" && options.Devel {
		options.Version = ">0.0.0-0"
	}
	registryClient, registryClientOut, err := newRegistryClient(
		options.CertFile,
		options.KeyFile,
		options.CaFile,
		options.InsecureSkipTLSverify,
		options.PlainHttp,
		options.Debug,
	)
	outputs.registryClientOut = registryClientOut
	if err != nil {
		return nil, outputs, err
	}
	cfgOptions := &CfgOptions{
		RegistryClient: registryClient,
		KubeConfig:     options.KubeConfig,
		Namespace:      options.Namespace,
	}
	if options.Debug {
		cfgOptions.KubeOut = outputs.kubeOut
	}
	client := action.NewInstall(NewCfg(cfgOptions))
	client.GenerateName = options.GenerateName
	client.NameTemplate = options.NameTemplate
	client.Version = options.Version
	var name, chartReference string
	if options.GenerateName {
		// Generate name if applicable
		name, chartReference, _ = client.NameAndChart([]string{options.Chart})
	} else {
		name = options.Name
		chartReference = options.Chart
	}
	client.ReleaseName = name
	client.Namespace = options.Namespace
	client.Atomic = options.Atomic
	client.CreateNamespace = options.CreateNamespace
	client.Description = options.Description
	client.Devel = options.Devel
	client.DryRun = options.DryRun
	client.DryRunOption = dryRunOption(options.DryRunOption)
	client.Wait = options.Wait
	client.Timeout = options.Timeout
	client.ClientOnly = options.ClientOnly
	client.CertFile = options.CertFile
	client.KeyFile = options.KeyFile
	client.CaFile = options.CaFile
	client.DisableOpenAPIValidation = options.DisableOpenApiValidation
	client.InsecureSkipTLSverify = options.InsecureSkipTLSverify
	client.PlainHTTP = options.PlainHttp
	chartRequested, chartPath, err := loadChart(client.ChartPathOptions, options.RepositoryConfig, chartReference)
	if err != nil {
		return nil, outputs, err
	}
	if notInstallable := checkIfInstallable(chartRequested); notInstallable != nil {
		return nil, outputs, notInstallable
	}
	// Dependency management
	chartRequested, updateOutput, err := updateDependencies(&updateDependenciesOptions{
		DependencyUpdate: options.DependencyUpdate,
		Keyring:          options.Keyring,
		Debug:            options.Debug,
	}, chartRequested, chartPath)
	if err != nil {
		return nil, outputs, err
	}
	outputs.updateOutput = updateOutput
	// Dry Run options
	if invalidDryRun := validateDryRunOptionFlag(client.DryRunOption); invalidDryRun != nil {
		return nil, outputs, invalidDryRun
	}
	// Values
	vals, err := mergeValues(options.Values, options.ValuesFiles)
	if err != nil {
		return nil, outputs, err
	}

	// Create context that handles SIGINT, SIGTERM
	ctx := context.Background()
	ctx, cancel := context.WithCancel(ctx)
	// Set up channel on which to send signal notifications.
	// We must use a buffered channel or risk missing the signal
	// if we're not ready to receive when the signal is sent.
	cSignal := make(chan os.Signal, 4)
	signal.Notify(cSignal, syscall.SIGINT, syscall.SIGTERM, syscall.SIGKILL, syscall.SIGQUIT)
	go func() {
		<-cSignal
		cancel()
	}()

	// Run
	rel, err := client.RunWithContext(ctx, chartRequested, vals)
	return rel, outputs, err
}

// https://github.com/helm/helm/blob/ef02cafdd0a0be75b1f83f1b2c9ca4d1ac3edda5/cmd/helm/install.go#L309-L318
// checkIfInstallable validates if a chart can be installed
//
// Application chart type is only installable
func checkIfInstallable(ch *chart.Chart) error {
	switch ch.Metadata.Type {
	case "", "application":
		return nil
	}
	return errors.Errorf("%s charts are not installable", ch.Metadata.Type)
}

// https://github.com/helm/helm/blob/ef02cafdd0a0be75b1f83f1b2c9ca4d1ac3edda5/cmd/helm/install.go#L332-L346
func validateDryRunOptionFlag(dryRunOptionFlagValue string) error {
	// Validate dry-run flag value with a set of allowed value
	allowedDryRunValues := []string{"false", "true", "none", "client", "server"}
	isAllowed := false
	for _, v := range allowedDryRunValues {
		if dryRunOptionFlagValue == v {
			isAllowed = true
			break
		}
	}
	if !isAllowed {
		return errors.New("Invalid dry-run flag. Flag must one of the following: false, true, none, client, server")
	}
	return nil
}

type updateDependenciesOptions struct {
	DependencyUpdate bool
	Keyring          string
	Debug            bool
}

func loadChart(chartPathOptions action.ChartPathOptions, repositoryConfig string, chartReference string) (*chart.Chart, string, error) {
	settings := cli.New()
	if repositoryConfig != "" {
		settings.RepositoryConfig = repositoryConfig
	}
	chartPath, err := chartPathOptions.LocateChart(chartReference, settings)
	if err != nil {
		return nil, "", err
	}
	chartRequested, err := loader.Load(chartPath)
	return chartRequested, chartPath, err
}

func updateDependencies(options *updateDependenciesOptions, chart *chart.Chart, chartPath string) (*chart.Chart, string, error) {
	dependencies := chart.Metadata.Dependencies
	if dependencies == nil {
		return chart, "", nil
	}
	invalidDependencies := action.CheckDependencies(chart, dependencies)
	if invalidDependencies == nil {
		return chart, "", nil
	}
	// Dependencies are invalid, try to update them
	invalidDependencies = errors.Wrap(invalidDependencies, "An error occurred while checking for chart dependencies. You may need to run `helm dependency build` to fetch missing dependencies")
	if options.DependencyUpdate {
		updateOutput, updateError := DependencyUpdate(&DependencyOptions{
			Path:        chartPath,
			Keyring:     options.Keyring,
			SkipRefresh: false,
			Debug:       options.Debug,
		})
		if updateError != nil {
			return nil, updateOutput, errors.Wrap(updateError, "An error occurred while updating chart dependencies")
		}
		reloadedChart, reloadError := loader.Load(chartPath)
		if reloadError != nil {
			return nil, updateOutput, errors.Wrap(reloadError, "An error occurred while reloading chart dependencies")
		}
		return reloadedChart, updateOutput, nil
	}
	return chart, "", invalidDependencies
}

func dryRunOption(dryRunOption string) string {
	if dryRunOption == "" {
		return "none"
	} else {
		return dryRunOption
	}
}

var escapedChars = []rune("\"'\\={[,.]}")

func parseValuesSet(values string) ([]string, error) {
	result := make([]string, 0)
	if values != "" {
		params, err := url.ParseQuery(values)
		if err != nil {
			return nil, err
		}
		for key, value := range params {
			escapedValue := bytes.NewBuffer(make([]byte, 0))
			for _, char := range value[0] {
				if slices.Contains(escapedChars, char) {
					escapedValue.WriteString("\\")
				}
				escapedValue.WriteRune(char)
			}
			result = append(result, fmt.Sprintf("%s=%s", key, escapedValue))
		}
	}
	return result, nil
}

// mergeValues returns a map[string]interface{} with the provided processed values
func mergeValues(encodedValuesMap, encodedValuesFiles string) (map[string]interface{}, error) {
	valuesSet, err := parseValuesSet(encodedValuesMap)
	if err != nil {
		return nil, err
	}
	valueFiles := make([]string, 0)
	if encodedValuesFiles != "" {
		for _, valuesFile := range strings.Split(encodedValuesFiles, ",") {
			valueFiles = append(valueFiles, valuesFile)
		}
	}
	return (&values.Options{
		Values:     valuesSet,
		ValueFiles: valueFiles,
	}).MergeValues(make(getter.Providers, 0))
}
