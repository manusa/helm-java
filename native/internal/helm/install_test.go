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
	"os"
	"path"
	"strings"
	"testing"
)

func TestInstallDry(t *testing.T) {
	create, _ := Create(&CreateOptions{
		Name: "test",
		Dir:  t.TempDir(),
	})
	out, err := Install(&InstallOptions{
		Chart:        create,
		Name:         "test",
		Namespace:    "a-namespace",
		DryRun:       true,
		DryRunOption: "client",
		ClientOnly:   true,
	})
	if err != nil {
		t.Errorf("Expected install to succeed, got %s", err)
		return
	}
	if !strings.Contains(out, "NAME: test") {
		t.Errorf("Expected install to succeed, got %s", out)
		return
	}
}

func TestInstallDryUrl(t *testing.T) {
	out, err := Install(&InstallOptions{
		Chart:        "https://github.com/kubernetes/ingress-nginx/releases/download/helm-chart-4.9.1/ingress-nginx-4.9.1.tgz",
		Name:         "test",
		DryRun:       true,
		DryRunOption: "client",
		ClientOnly:   true,
	})
	if err != nil {
		t.Errorf("Expected install to succeed, got %s", err)
		return
	}
	if !strings.Contains(out, "CHART: ingress-nginx-4.9.1") {
		t.Errorf("Expected install to succeed, got %s", out)
		return
	}
}

func TestInstallFromRepoAndInvalidVersion(t *testing.T) {
	// Add a temp repository to retrieve the chart from (should include ingress-nginx)
	repositoryConfigFile, _ := os.CreateTemp("", "repositories.yaml")
	defer os.Remove(repositoryConfigFile.Name())
	err := RepoAdd(&RepoOptions{
		Name:                  "helm",
		Url:                   "https://charts.helm.sh/stable",
		InsecureSkipTlsVerify: true,
		RepositoryConfig:      repositoryConfigFile.Name(),
	})
	if err != nil {
		t.Errorf("Expected repo add to succeed, got %s", err)
		return
	}
	// When
	_, err = Install(&InstallOptions{
		Chart:            "helm/ingress-nginx",
		Name:             "ingress-nginx",
		Version:          "9999.9999.9999",
		ClientOnly:       true,
		RepositoryConfig: repositoryConfigFile.Name(),
	})
	// Then
	if err == nil {
		t.Error("Expected install to fail but was successful")
		return
	}
	if !strings.Contains(err.Error(), "chart \"ingress-nginx\" matching 9999.9999.9999 not found") {
		t.Errorf("Expected error with version not found, got %s", err.Error())
		return
	}
}

func TestInstallValues(t *testing.T) {
	create, _ := Create(&CreateOptions{
		Name: "test",
		Dir:  t.TempDir(),
	})
	out, err := Install(&InstallOptions{
		Chart:      create,
		Name:       "test",
		Values:     "corner=%22%27%5C%3D%7B%5B%2C.%5D%7D%C2%A1%21%C2%BF%3F-_test%3D1%2Cother%3D2",
		Debug:      true,
		ClientOnly: true,
	})
	if err != nil {
		t.Errorf("Expected install to succeed, got %s", err)
		return
	}
	if !strings.Contains(out, "USER-SUPPLIED VALUES:") || !strings.Contains(out, "corner: '\"''\\={[,.]}¡!¿?-_test=1,other=2'") {
		t.Errorf("Expected install to contain specific values, got %s", out)
		return
	}
}


func TestInstallWithValuesFile(t *testing.T) {
	tmpDir := t.TempDir()
	create, _ := Create(&CreateOptions{
		Name: "test",
		Dir:  tmpDir,
	})
	valuesFilePath := path.Join(tmpDir, "valuesFile.yaml")
	valuesBytes := []byte("nix: baz\n")
	err := os.WriteFile(valuesFilePath, valuesBytes, 0666)
	out, err := Install(&InstallOptions{
		Chart:      create,
		Name:       "test",
		ValuesFile: valuesFilePath,
		Debug:      true,
		ClientOnly: true,
	})
	if err != nil {
		t.Errorf("Expected install to succeed, got %s", err)
		return
	}
	if !strings.Contains(out, "USER-SUPPLIED VALUES:") || !strings.Contains(out, "nix: baz") {
		t.Errorf("Expected install to contain specific values from valuesFile, got %s", out)
		return
	}
}

func TestInstallDependencyUpdate(t *testing.T) {
	chart, _ := Create(&CreateOptions{
		Name: "test",
		Dir:  t.TempDir(),
	})
	dependency, _ := Create(&CreateOptions{
		Name: "dependency",
		Dir:  t.TempDir(),
	})
	chartYaml, _ := os.OpenFile(path.Join(chart, "Chart.yaml"), os.O_APPEND|os.O_WRONLY, 0666)
	_, _ = chartYaml.WriteString("\ndependencies:\n" +
		"  - name: dependency\n" +
		"    version: 0.1.0\n" +
		"    repository: file://" + dependency + "\n")
	_ = chartYaml.Close()
	out, err := Install(&InstallOptions{
		Chart:            chart,
		Name:             "test",
		DependencyUpdate: true,
		ClientOnly:       true,
	})
	if err != nil {
		t.Errorf("Expected install to succeed, got %s", err)
		return
	}
	if !strings.Contains(out, "NAME: test") {
		t.Errorf("Expected install to succeed, got %s", out)
		return
	}
	if !strings.Contains(out, "Saving 1 charts") || !strings.Contains(out, "Deleting outdated charts") {
		t.Errorf("Expected install update dependencies, got %s", out)
		return
	}
	_, err = os.Stat(path.Join(chart, "Chart.lock"))
	if err != nil {
		t.Error("Expected install to create lock file")
	}
}
