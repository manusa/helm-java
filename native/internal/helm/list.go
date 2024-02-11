package helm

import (
	"bytes"
	"fmt"
	"helm.sh/helm/v3/pkg/action"
	"net/url"
	"strconv"
)

type ListOptions struct {
	All           bool
	AllNamespaces bool
	Deployed      bool
	Failed        bool
	Pending       bool
	Superseded    bool
	Uninstalled   bool
	Uninstalling  bool
	Namespace     string
	KubeConfig    string
}

func List(options *ListOptions) (string, error) {
	cfg := NewCfg(&CfgOptions{
		KubeConfig:    options.KubeConfig,
		Namespace:     options.Namespace,
		AllNamespaces: options.AllNamespaces,
	})
	client := action.NewList(cfg)
	client.All = options.All
	client.AllNamespaces = options.AllNamespaces
	client.Deployed = options.Deployed
	client.Failed = options.Failed
	client.Pending = options.Pending
	client.Superseded = options.Superseded
	client.Uninstalled = options.Uninstalled
	client.Uninstalling = options.Uninstalling
	client.SetStateMask()

	results, err := client.Run()
	if err != nil {
		return "", err
	}
	out := bytes.NewBuffer(make([]byte, 0))
	for _, release := range results {
		values := make(url.Values)
		values.Set("name", release.Name)
		values.Set("namespace", release.Namespace)
		values.Set("revision", strconv.Itoa(release.Version))
		if tspb := release.Info.LastDeployed; !tspb.IsZero() {
			values.Set("updated", strconv.FormatInt(tspb.UnixMilli(), 10))
		}
		values.Set("status", release.Info.Status.String())
		values.Set("chart", formatChartname(release.Chart))
		values.Set("appVersion", formatAppVersion(release.Chart))
		_, _ = fmt.Fprintln(out, values.Encode())
	}
	return out.String(), nil
}
