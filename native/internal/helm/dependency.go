package helm

import (
	"bytes"
	"helm.sh/helm/v3/pkg/action"
	"helm.sh/helm/v3/pkg/downloader"
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
	registryClient, registryClientOut, err := newRegistryClient(
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
	return appendToOutOrErr(registryClientOut, out.String(), err)
}

func DependencyList(options *DependencyOptions) (string, error) {
	client := action.NewDependency()
	out := bytes.NewBuffer(make([]byte, 0))
	err := client.List(options.Path, out)
	return out.String(), err
}

func DependencyUpdate(options *DependencyOptions) (string, error) {
	registryClient, registryClientOut, err := newRegistryClient(
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
	return appendToOutOrErr(registryClientOut, out.String(), err)
}

func newManager(options *DependencyOptions, registryClient *registry.Client) (*downloader.Manager, *bytes.Buffer) {
	out := bytes.NewBuffer(make([]byte, 0))
	manager := &downloader.Manager{
		Out:            out,
		ChartPath:      options.Path,
		Keyring:        options.Keyring,
		SkipUpdate:     options.SkipRefresh,
		RegistryClient: registryClient,
		Debug:          options.Debug,
	}
	return manager, out
}
