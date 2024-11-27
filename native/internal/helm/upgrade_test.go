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
	"strings"
	"testing"
)

func TestUpgradeFromRepoAndInvalidVersion(t *testing.T) {
	// Add a temp repository to retrieve the chart from (should include ingress-nginx)
	repositoryConfigFile, _ := os.CreateTemp(t.TempDir(), "repositories.yaml")
	defer func(name string) { _ = os.Remove(name) }(repositoryConfigFile.Name())
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
	_, err = Upgrade(&UpgradeOptions{
		Chart:            "helm/ingress-nginx",
		Name:             "ingress-nginx",
		Version:          "9999.9999.9999",
		ClientOnly:       true,
		RepositoryConfig: repositoryConfigFile.Name(),
	})
	// Then
	if err == nil {
		t.Error("Expected upgrade to fail but was successful")
		return
	}
	if !strings.Contains(err.Error(), "chart \"ingress-nginx\" matching 9999.9999.9999 not found") {
		t.Errorf("Expected error with version not found, got %s", err.Error())
		return
	}
}
