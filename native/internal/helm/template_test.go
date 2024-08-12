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
	"helm.sh/helm/v3/pkg/chartutil"
	"os"
	"strings"
	"testing"
)

func TestTemplateFromLocal(t *testing.T) {
	create, _ := Create(&CreateOptions{
		Name: "chart-for-template-tests",
		Dir:  t.TempDir(),
	})
	t.Run("with defaults", func(t *testing.T) {
		manifests, err := Template(&TemplateOptions{
			Chart: create,
		})
		if err != nil {
			t.Errorf("Expected template to succeed, got %s", err)
			return
		}
		if !strings.Contains(manifests, "name: release-name-chart-for-template-tests") {
			t.Errorf("Expected template to include provided name, got %s", manifests)
			return
		}
	})
	t.Run("with name", func(t *testing.T) {
		manifests, err := Template(&TemplateOptions{
			Name:  "the-name",
			Chart: create,
		})
		if err != nil {
			t.Errorf("Expected template to succeed, got %s", err)
			return
		}
		if !strings.Contains(manifests, "name: the-name-chart-for-template-tests") {
			t.Errorf("Expected template to include provided name, got %s", manifests)
			return
		}
	})
	t.Run("with invalid values", func(t *testing.T) {
		manifests, err := Template(&TemplateOptions{
			Chart:  create,
			Values: "ingress.enabled=true&ingress.annotations=-+invalid+value+-+--+-+-",
		})
		if err == nil {
			t.Error("Expected template to fail")
			return
		}
		if manifests != "" {
			t.Errorf("Expected manifests to be \"\" after failure, got %v", manifests)
		}
	})
	t.Run("with invalid values and debug", func(t *testing.T) {
		manifests, err := Template(&TemplateOptions{
			Chart:  create,
			Values: "ingress.enabled=true&ingress.annotations=-+invalid+value+-+--+-+-",
			Debug:  true,
		})
		if err == nil {
			t.Error("Expected template to fail")
			return
		}
		if manifests != "" {
			t.Errorf("Expected manifests to be \"\" after failure, got %v", manifests)
		}
		if !strings.Contains(err.Error(), "name: release-name-chart-for-template-tests") {
			t.Errorf("Expected error to contain manifests with debug, got %s", manifests)
			return
		}
	})
}

func TestTemplateFromReference(t *testing.T) {
	// Add a temp repository to retrieve the chart from (should include ingress-nginx)
	repositoryConfigFile, _ := os.CreateTemp("", "repositories.yaml")
	defer os.Remove(repositoryConfigFile.Name())
	err := RepoAdd(&RepoOptions{
		Name:                  "ingress-nginx",
		Url:                   "https://kubernetes.github.io/ingress-nginx",
		InsecureSkipTlsVerify: true,
		RepositoryConfig:      repositoryConfigFile.Name(),
	})
	if err != nil {
		t.Errorf("Expected repo add to succeed, got %s", err)
		return
	}
	// Set default Kube version (clientOnly) to something compatible with the ingress-nginx chart
	chartutil.DefaultCapabilities.KubeVersion.Minor = "21"
	chartutil.DefaultCapabilities.KubeVersion.Version = "v1.21.0"
	t.Run("with name", func(t *testing.T) {
		manifests, err := Template(&TemplateOptions{
			Name:             "the-name",
			Chart:            "ingress-nginx/ingress-nginx",
			RepositoryConfig: repositoryConfigFile.Name(),
		})
		if err != nil {
			t.Errorf("Expected template to succeed, got %s", err)
			return
		}
		if !strings.Contains(manifests, "helm.sh/chart: ingress-nginx-") {
			t.Errorf("Expected template to include referenced chart, got %s", manifests)
			return
		}
		if !strings.Contains(manifests, "name: the-name-ingress-nginx-") {
			t.Errorf("Expected template to include provided name, got %s", manifests)
			return
		}
	})
	t.Run("with values", func(t *testing.T) {
		manifests, err := Template(&TemplateOptions{
			Chart:            "ingress-nginx/ingress-nginx",
			RepositoryConfig: repositoryConfigFile.Name(),
			Values:           "controller.image.registry=helm-java.registry.example.com",
			Debug:            true,
		})
		if err != nil {
			t.Errorf("Expected template to succeed, got %s", err)
			return
		}
		if !strings.Contains(manifests, "image: helm-java.registry.example.com/ingress-nginx/controller") {
			t.Errorf("Expected template to include overridden value, got %s", manifests)
			return
		}
	})
}
