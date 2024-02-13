package helm

import (
	"bytes"
	"context"
	"helm.sh/helm/v3/pkg/action"
	"helm.sh/helm/v3/pkg/chart/loader"
	"helm.sh/helm/v3/pkg/storage/driver"
	"time"
)

type UpgradeOptions struct {
	Name                  string
	Chart                 string
	Namespace             string
	Install               bool
	Force                 bool
	ResetValues           bool
	ReuseValues           bool
	ResetThenReuseValues  bool
	Atomic                bool
	CleanupOnFail         bool
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

func Upgrade(options *UpgradeOptions) (string, error) {
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
	cfg := NewCfg(cfgOptions)

	// Install if release doesn't exist
	if options.Install {
		histClient := action.NewHistory(cfg)
		histClient.Max = 1
		if _, err := histClient.Run(options.Name); err == driver.ErrReleaseNotFound {
			return Install(&InstallOptions{
				Name:                  options.Name,
				GenerateName:          false,
				NameTemplate:          "",
				Chart:                 options.Chart,
				Namespace:             options.Namespace,
				CreateNamespace:       options.CreateNamespace,
				Description:           options.Description,
				Devel:                 options.Devel,
				DependencyUpdate:      options.DependencyUpdate,
				DryRun:                options.DryRun,
				DryRunOption:          options.DryRunOption,
				Wait:                  options.Wait,
				Timeout:               options.Timeout,
				Values:                options.Values,
				KubeConfig:            options.KubeConfig,
				CertFile:              options.CertFile,
				KeyFile:               options.KeyFile,
				CaFile:                options.CaFile,
				InsecureSkipTLSverify: options.InsecureSkipTLSverify,
				PlainHttp:             options.PlainHttp,
				Keyring:               options.Keyring,
				Debug:                 options.Debug,
				ClientOnly:            options.ClientOnly,
			})
		} else if err != nil {
			return "", err
		}
	}

	client := action.NewUpgrade(cfg)
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
	client.DryRun = options.DryRun
	client.DryRunOption = dryRunOption(options.DryRunOption)
	client.Wait = options.Wait
	// Timeout defaults to 5 minutes (used when wait is enabled)
	if options.Timeout == 0 {
		client.Timeout = 300 * time.Second
	}
	client.Timeout = options.Timeout
	client.CertFile = options.CertFile
	client.KeyFile = options.KeyFile
	client.CaFile = options.CaFile
	client.InsecureSkipTLSverify = options.InsecureSkipTLSverify
	client.PlainHTTP = options.PlainHttp
	client.Keyring = options.Keyring

	chartReference := options.Chart
	chartRequested, err := loader.Load(chartReference)
	if err != nil {
		return "", err
	}
	// Dependency management
	chartRequested, updateOutput, err := updateDependencies(&updateDependenciesOptions{
		DependencyUpdate: options.DependencyUpdate,
		Keyring:          options.Keyring,
		Debug:            options.Debug,
	}, chartRequested, chartReference)
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
	release, err := client.RunWithContext(ctx, options.Name, chartRequested, values)
	// Generate report
	out := StatusReport(release, false, options.Debug)
	return appendToOutOrErr(concat(cStr(updateOutput), cBuf(registryClientOut), cBuf(kubeOut)), out, err)
}
