package helm

import (
	"bytes"
	"fmt"
	"helm.sh/helm/v3/pkg/action"
)

type UninstallOptions struct {
	ReleaseName    string
	DryRun         bool
	NoHooks        bool
	IgnoreNotFound bool
	KeepHistory    bool
	Cascade        string
	Namespace      string
	KubeConfig     string
	Debug          bool
}

func Uninstall(options *UninstallOptions) (string, error) {
	cfgOptions := &CfgOptions{
		KubeConfig: options.KubeConfig,
		namespace:  options.Namespace,
	}
	kubeOut := bytes.NewBuffer(make([]byte, 0))
	if options.Debug {
		cfgOptions.KubeOut = kubeOut
	}
	client := action.NewUninstall(NewCfg(cfgOptions))
	client.DryRun = options.DryRun
	client.DisableHooks = options.NoHooks
	client.IgnoreNotFound = options.IgnoreNotFound
	client.KeepHistory = options.KeepHistory
	client.DeletionPropagation = "background"
	if options.Cascade != "" {
		client.DeletionPropagation = options.Cascade
	}
	if invalidCascadeFlag := validateCascadeFlag(client); invalidCascadeFlag != nil {
		return "", invalidCascadeFlag
	}
	res, err := client.Run(options.ReleaseName)
	out := bytes.NewBuffer(make([]byte, 0))
	if res != nil && res.Info != "" {
		_, _ = fmt.Fprintln(out, res.Info)
	}
	_, _ = fmt.Fprintf(out, "release \"%s\" uninstalled\n", options.ReleaseName)
	return appendToOutOrErr(kubeOut, out.String(), err)
}

// https://github.com/helm/helm/blob/48dbda2fa8d1e8981c271a56fe51bdf8b131fac2/cmd/helm/uninstall.go#L87
func validateCascadeFlag(client *action.Uninstall) error {
	if client.DeletionPropagation != "background" && client.DeletionPropagation != "foreground" && client.DeletionPropagation != "orphan" {
		return fmt.Errorf("invalid cascade value (%s). Must be \"background\", \"foreground\", or \"orphan\"", client.DeletionPropagation)
	}
	return nil
}
