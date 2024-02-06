package helm

import (
	"helm.sh/helm/v3/pkg/action"
	"time"
)

type TestOptions struct {
	ReleaseName string
	Namespace   string
	KubeConfig  string
	Timeout     time.Duration
	Debug       bool
}

func Test(options *TestOptions) (string, error) {
	cfgOptions := &CfgOptions{
		KubeConfig: options.KubeConfig,
		namespace:  options.Namespace,
	}
	client := action.NewReleaseTesting(NewCfg(cfgOptions))
	client.Namespace = options.Namespace
	client.Timeout = options.Timeout
	release, err := client.Run(options.ReleaseName)
	out := StatusReport(release, false, options.Debug)
	return out, err
}
