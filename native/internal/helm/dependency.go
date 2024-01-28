package helm

import (
	"bytes"
	"helm.sh/helm/v3/pkg/downloader"
)

type DependencyOptions struct {
	Path        string
	Keyring     string
	SkipRefresh bool
	Verify      bool
	Debug       bool
}

func DependencyUpdate(options *DependencyOptions) (string, error) {
	registryClient, registryClientOut, err := newRegistryClient(
		"", "", "", false, false,
		options.Debug,
	)
	if err != nil {
		return "", err
	}
	out := bytes.NewBuffer(make([]byte, 0))
	manager := &downloader.Manager{
		Out:            out,
		ChartPath:      options.Path,
		Keyring:        options.Keyring,
		SkipUpdate:     options.SkipRefresh,
		RegistryClient: registryClient,
		Debug:          options.Debug,
	}
	if options.Verify {
		manager.Verify = downloader.VerifyAlways
	}
	err = manager.Update() // needs to be evaluated first so that out gets populated during the update
	return appendToOutOrErr(registryClientOut, out.String(), err)
}
