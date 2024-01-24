package helm

import (
	"helm.sh/helm/v3/pkg/action"
	"helm.sh/helm/v3/pkg/cli"
	"helm.sh/helm/v3/pkg/registry"
	"os"
)

type CfgOptions struct {
	registryClient *registry.Client
	KubeConfig     string
	namespace      string
}

func NewCfg(options *CfgOptions) *action.Configuration {
	settings := cli.New()
	settings.KubeConfig = options.KubeConfig
	actionConfig := new(action.Configuration)
	discard := func(format string, v ...interface{}) {
		// TODO implement in case we want to debug Kubernetes
	}
	if options.namespace != "" {
		settings.SetNamespace(options.namespace)
	}
	err := actionConfig.Init(settings.RESTClientGetter(), settings.Namespace(), os.Getenv("HELM_DRIVER"), discard)
	if err != nil {
		panic(err)
	}
	actionConfig.RegistryClient = options.registryClient
	return actionConfig
}
