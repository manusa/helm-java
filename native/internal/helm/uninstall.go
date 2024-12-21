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
	"helm.sh/helm/v3/pkg/action"
)

type UninstallOptions struct {
	ReleaseName        string
	DryRun             bool
	NoHooks            bool
	IgnoreNotFound     bool
	KeepHistory        bool
	Cascade            string
	Namespace          string
	KubeConfig         string
	KubeConfigContents string
	Debug              bool
}

func Uninstall(options *UninstallOptions) (string, error) {
	cfgOptions := &CfgOptions{
		KubeConfig:         options.KubeConfig,
		KubeConfigContents: options.KubeConfigContents,
		Namespace:          options.Namespace,
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
