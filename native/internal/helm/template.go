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
	"strings"
)

type TemplateOptions struct {
	CertOptions
	Name             string
	Version          string
	Chart            string
	Namespace        string
	KubeVersion      string
	DependencyUpdate bool
	SkipCRDs         bool
	Values           string
	ValuesFiles      string
	Debug            bool
	RepositoryConfig string
}

func Template(options *TemplateOptions) (string, error) {
	var releaseName string
	if options.Name == "" {
		releaseName = "release-name"
	} else {
		releaseName = options.Name
	}
	rel, _, err := install(&InstallOptions{
		DryRun:           true,
		ClientOnly:       true,
		CertOptions:      options.CertOptions,
		Name:             releaseName,
		Version:          options.Version,
		Chart:            options.Chart,
		Namespace:        options.Namespace,
		KubeVersion:      options.KubeVersion,
		DependencyUpdate: options.DependencyUpdate,
		SkipCRDs:         options.SkipCRDs,
		Values:           options.Values,
		ValuesFiles:      options.ValuesFiles,
		Debug:            options.Debug,
		RepositoryConfig: options.RepositoryConfig,
	})

	if err != nil && !options.Debug {
		if rel != nil {
			return "", fmt.Errorf("%w\n\nUse --debug flag to render out invalid YAML", err)
		}
		return "", err
	}

	var manifests bytes.Buffer
	if _, fmtErr := fmt.Fprintln(&manifests, strings.TrimSpace(rel.Manifest)); fmtErr != nil {
		return "", fmtErr
	}
	return appendToOutOrErr(&manifests, "", err)
}
