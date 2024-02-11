package helm

import (
	"bytes"
	"context"
	"fmt"
	"github.com/pkg/errors"
	"helm.sh/helm/v3/pkg/action"
	"helm.sh/helm/v3/pkg/chart"
	"helm.sh/helm/v3/pkg/chart/loader"
	"helm.sh/helm/v3/pkg/strvals"
	"net/url"
	"slices"
	"time"
)

type InstallOptions struct {
	Name                  string
	GenerateName          bool
	NameTemplate          string
	Chart                 string
	Namespace             string
	CreateNamespace       bool
	Description           string
	Devel                 bool
	DependencyUpdate      bool
	DryRun                bool
	DryRunOption          string
	Wait                  bool
	Timeout               time.Duration
	Values                string
	KubeConfig            string
	CertFile              string
	KeyFile               string
	CaFile                string
	InsecureSkipTLSverify bool
	PlainHttp             bool
	Keyring               string
	Debug                 bool
	// For testing purposes only, prevents connecting to Kubernetes (happens even with DryRun=true and DryRunOption=client)
	ClientOnly bool
}

func Install(options *InstallOptions) (string, error) {
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
	client.GenerateName = options.GenerateName
	client.NameTemplate = options.NameTemplate
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
	client.CreateNamespace = options.CreateNamespace
	client.Description = options.Description
	client.Devel = options.Devel
	client.DryRun = options.DryRun
	if options.DryRunOption == "" {
		client.DryRunOption = "none"
	} else {
		client.DryRunOption = options.DryRunOption
	}
	client.Wait = options.Wait
	// Timeout defaults to 5 minutes (used when wait is enabled)
	if options.Timeout == 0 {
		client.Timeout = 300 * time.Second
	}
	client.ClientOnly = options.ClientOnly
	client.CertFile = options.CertFile
	client.KeyFile = options.KeyFile
	client.CaFile = options.CaFile
	client.InsecureSkipTLSverify = options.InsecureSkipTLSverify
	client.PlainHTTP = options.PlainHttp
	chartRequested, err := loader.Load(chartReference)
	if err != nil {
		return "", err
	}
	if notInstallable := checkIfInstallable(chartRequested); notInstallable != nil {
		return "", notInstallable
	}
	// Dependency management
	chartRequested, updateOutput, err := updateDependencies(options, chartRequested, chartReference)
	if err != nil {
		return "", err
	}
	// Dry Run options
	if invalidDryRun := validateDryRunOptionFlag(client.DryRunOption); invalidDryRun != nil {
		return "", invalidDryRun
	}
	ctx := context.Background()
	// Values
	var values, invalidValues = parseValues(options.Values)
	if invalidValues != nil {
		return "", invalidValues
	}
	// Run
	release, err := client.RunWithContext(ctx, chartRequested, values)
	// Generate report
	out := StatusReport(release, false, options.Debug)
	return appendToOutOrErr(concat(cStr(updateOutput), cBuf(registryClientOut), cBuf(kubeOut)), out, err)
}

var escapedChars = []rune("\"'\\={[,.]}")

func parseValues(values string) (map[string]interface{}, error) {
	result := make(map[string]interface{})
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
			_ = strvals.ParseInto(fmt.Sprintf("%s=%s", key, escapedValue), result)
		}
	}
	return result, nil
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

func updateDependencies(options *InstallOptions, chart *chart.Chart, chartReference string) (*chart.Chart, string, error) {
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
			Path:        chartReference,
			Keyring:     options.Keyring,
			SkipRefresh: false,
			Debug:       options.Debug,
		})
		if updateError != nil {
			return nil, updateOutput, errors.Wrap(updateError, "An error occurred while updating chart dependencies")
		}
		reloadedChart, reloadError := loader.Load(chartReference)
		if reloadError != nil {
			return nil, updateOutput, errors.Wrap(reloadError, "An error occurred while reloading chart dependencies")
		}
		return reloadedChart, updateOutput, nil
	}
	return chart, "", invalidDependencies
}
