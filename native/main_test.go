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

package main

import (
	"net/http"
	"os"
	"path"
	"path/filepath"
	"strings"
	"testing"
	"time"

	"github.com/manusa/helm-java/native/internal/helm"
	"helm.sh/helm/v3/pkg/repo/repotest"
)

// helmEnvVars contains the list of Helm-related environment variables that should be isolated
var helmEnvVars = []string{
	"HELM_CACHE_HOME",
	"HELM_CONFIG_HOME",
	"HELM_DATA_HOME",
	"HELM_DEBUG",
	"HELM_DRIVER",
	"HELM_KUBEAPISERVER",
	"HELM_KUBECONTEXT",
	"HELM_KUBETOKEN",
	"HELM_NAMESPACE",
	"HELM_REGISTRY_CONFIG",
	"HELM_REPOSITORY_CACHE",
	"HELM_REPOSITORY_CONFIG",
	"KUBECONFIG",
	// Docker/Registry credential storage
	"DOCKER_CONFIG",
	"REGISTRY_AUTH_FILE",
	// GPG-related environment variables
	"GNUPGHOME",
	"GPG_TTY",
	// Pass credential helper (uses GPG)
	"PASSWORD_STORE_DIR",
	"PASSWORD_STORE_GPG_OPTS",
	// Other credential helpers
	"DOCKER_CREDENTIAL_HELPERS",
	// PATH to avoid credential helpers
	"PATH",
}

// TestMain sets up an isolated environment for tests by clearing Helm-related
// environment variables and setting up temporary directories
func TestMain(m *testing.M) {
	// Save original environment
	originalEnv := make(map[string]string)
	for _, envVar := range helmEnvVars {
		if val, exists := os.LookupEnv(envVar); exists {
			originalEnv[envVar] = val
		}
	}

	// Create temporary directories for Helm
	tempDir, err := os.MkdirTemp("", "helm-test-*")
	if err != nil {
		panic(err)
	}
	defer os.RemoveAll(tempDir)

	helmCacheHome := filepath.Join(tempDir, "cache")
	helmConfigHome := filepath.Join(tempDir, "config")
	helmDataHome := filepath.Join(tempDir, "data")
	dockerConfigHome := filepath.Join(tempDir, "docker")
	gnupgHome := filepath.Join(tempDir, "gnupg")

	// Create the directories
	_ = os.MkdirAll(helmCacheHome, 0755)
	_ = os.MkdirAll(helmConfigHome, 0755)
	_ = os.MkdirAll(helmDataHome, 0755)
	_ = os.MkdirAll(dockerConfigHome, 0755)
	_ = os.MkdirAll(gnupgHome, 0700)

	// Create a minimal Docker config to avoid GPG credential helper
	dockerConfigFile := filepath.Join(dockerConfigHome, "config.json")
	dockerConfig := `{
	"auths": {},
	"credHelpers": {},
	"credsStore": ""
}`
	_ = os.WriteFile(dockerConfigFile, []byte(dockerConfig), 0644)

	// Clear all Helm-related environment variables
	for _, envVar := range helmEnvVars {
		_ = os.Unsetenv(envVar)
	}

	// Set up clean test environment
	_ = os.Setenv("HELM_CACHE_HOME", helmCacheHome)
	_ = os.Setenv("HELM_CONFIG_HOME", helmConfigHome)
	_ = os.Setenv("HELM_DATA_HOME", helmDataHome)
	_ = os.Setenv("HELM_DRIVER", "memory")
	_ = os.Setenv("DOCKER_CONFIG", dockerConfigHome)
	_ = os.Setenv("GNUPGHOME", gnupgHome)
	// Set minimal PATH to avoid credential helper binaries
	_ = os.Setenv("PATH", "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin")

	// Run tests
	exitCode := m.Run()

	// Restore original environment
	for _, envVar := range helmEnvVars {
		_ = os.Unsetenv(envVar)
		if val, exists := originalEnv[envVar]; exists {
			_ = os.Setenv(envVar, val)
		}
	}

	os.Exit(exitCode)
}

func TestCreate(t *testing.T) {
	create, _ := helm.Create(&helm.CreateOptions{
		Name: "test",
		Dir:  t.TempDir(),
	})
	fi, _ := os.Stat(create)
	if !fi.IsDir() {
		t.Errorf("Expected %s to be a directory", create)
	}
}

func TestDependencyBuildWithPreviousUpdate(t *testing.T) {
	chart, _ := helm.Create(&helm.CreateOptions{
		Name: "test",
		Dir:  t.TempDir(),
	})
	dependency, _ := helm.Create(&helm.CreateOptions{
		Name: "dependency",
		Dir:  t.TempDir(),
	})
	chartYaml, _ := os.OpenFile(path.Join(chart, "Chart.yaml"), os.O_APPEND|os.O_WRONLY, 0666)
	_, _ = chartYaml.WriteString("\ndependencies:\n" +
		"  - name: dependency\n" +
		"    version: 0.1.0\n" +
		"    repository: file://" + dependency + "\n")
	_ = chartYaml.Close()
	_, _ = helm.DependencyUpdate(&helm.DependencyOptions{
		Path: chart,
	})
	out, err := helm.DependencyBuild(&helm.DependencyOptions{
		Path:  chart,
		Debug: true,
	})
	if err != nil {
		t.Errorf("Expected dependency update to succeed, got %s", err)
		return
	}
	if !strings.Contains(out, "Saving 1 charts") ||
		!strings.Contains(out, "Deleting outdated charts") ||
		!strings.Contains(out, "Archiving dependency from repo ") {
		t.Errorf("Expected dependency update to update dependencies, got %s", out)
		return
	}
}

func TestDependencyList(t *testing.T) {
	dir := t.TempDir()
	chart, _ := helm.Create(&helm.CreateOptions{
		Name: "test",
		Dir:  dir,
	})
	_, _ = helm.Create(&helm.CreateOptions{
		Name: "unlisted",
		Dir:  path.Join(dir, "test", "charts"),
	})
	chartYaml, _ := os.OpenFile(path.Join(chart, "Chart.yaml"), os.O_APPEND|os.O_WRONLY, 0666)
	_, _ = chartYaml.WriteString("\ndependencies:\n" +
		"  - name: dependency\n" +
		"    version: 0.1.0\n" +
		"    repository: file://../dependency\n")
	_ = chartYaml.Close()
	out, err := helm.DependencyList(&helm.DependencyOptions{
		Path: chart,
	})
	if err != nil {
		t.Errorf("Expected dependency update to succeed, got %s", err)
		return
	}
	if strings.Index(out, "NAME      \tVERSION\tREPOSITORY          \tSTATUS") != 0 {
		t.Errorf("Expected dependency list to show titles, got %s", out)
		return
	}
	if !strings.Contains(out, "dependency\t0.1.0  \tfile://../dependency\tmissing") {
		t.Errorf("Expected dependency list to list dependencies, got %s", out)
		return
	}
	if !strings.Contains(out, "WARNING: \""+dir+"/test/charts/unlisted\" is not in Chart.yaml.\n") {
		t.Errorf("Expected dependency list to warn on missing dependencies, got %s", out)
		return
	}
}

func TestDependencyUpdate(t *testing.T) {
	chart, _ := helm.Create(&helm.CreateOptions{
		Name: "test",
		Dir:  t.TempDir(),
	})
	dependency, _ := helm.Create(&helm.CreateOptions{
		Name: "dependency",
		Dir:  t.TempDir(),
	})
	chartYaml, _ := os.OpenFile(path.Join(chart, "Chart.yaml"), os.O_APPEND|os.O_WRONLY, 0666)
	_, _ = chartYaml.WriteString("\ndependencies:\n" +
		"  - name: dependency\n" +
		"    version: 0.1.0\n" +
		"    repository: file://" + dependency + "\n")
	_ = chartYaml.Close()
	out, err := helm.DependencyUpdate(&helm.DependencyOptions{
		Path:   chart,
		Verify: true,
	})
	if err != nil {
		t.Errorf("Expected dependency update to succeed, got %s", err)
		return
	}
	if !strings.Contains(out, "Saving 1 charts") || !strings.Contains(out, "Deleting outdated charts") {
		t.Errorf("Expected dependency update to update dependencies, got %s", out)
		return
	}
	_, err = os.Stat(path.Join(chart, "Chart.lock"))
	if err != nil {
		t.Error("Expected dependency update to create lock file")
	}
}

func TestPush(t *testing.T) {
	defer helm.RepoServerStopAll()
	srv, _ := helm.RepoOciServerStart(&helm.RepoServerOptions{})
	dir := t.TempDir()
	create, _ := helm.Create(&helm.CreateOptions{
		Name: "test",
		Dir:  dir,
	})
	_ = helm.Package(&helm.PackageOptions{Path: create, Destination: dir})
	_, _ = helm.RegistryLogin(&helm.RegistryOptions{
		Hostname: srv.RegistryURL,
		Username: "username",
		Password: "password",
		Debug:    true,
	})
	out, err := helm.Push(&helm.PushOptions{
		Chart:  path.Join(dir, "test-0.1.0.tgz"),
		Remote: "oci://" + srv.RegistryURL,
		Debug:  true,
	})
	if err != nil {
		t.Errorf("Expected push to succeed, got %s", err)
	}
	if !strings.Contains(out, "Pushed:") || !strings.Contains(out, "Digest:") {
		t.Errorf("Expected push to succeed, got %s", out)
	}
}

func TestPushUnauthorized(t *testing.T) {
	defer helm.RepoServerStopAll()
	srv, _ := helm.RepoOciServerStart(&helm.RepoServerOptions{Password: "not-known"})
	dir := t.TempDir()
	create, _ := helm.Create(&helm.CreateOptions{
		Name: "test",
		Dir:  dir,
	})
	_ = helm.Package(&helm.PackageOptions{Path: create, Destination: dir})
	out, err := helm.Push(&helm.PushOptions{
		Chart:       path.Join(dir, "test-0.1.0.tgz"),
		Remote:      "oci://" + srv.RegistryURL,
		Debug:       true,
		CertOptions: helm.CertOptions{PlainHttp: true},
	})
	if err == nil {
		t.Fatal("Expected push to fail")
	}
	if !strings.Contains(err.Error(), "basic credential not found") {
		t.Errorf("Expected push to fail with message, got %v", err)
	}
	if !strings.Contains(out, "level=debug") || !strings.Contains(out, "msg=Unauthorized") {
		t.Errorf("Expected out to contain debug info, got %s", out)
	}
}

func TestRegistryLogin(t *testing.T) {
	defer helm.RepoServerStopAll()
	srv, err := helm.RepoOciServerStart(&helm.RepoServerOptions{})
	if err != nil {
		t.Errorf("Expected server to be started")
	}
	_, err = helm.RegistryLogin(&helm.RegistryOptions{
		Hostname: srv.RegistryURL,
		Username: "username",
		Password: "password",
	})
	if err != nil {
		t.Errorf("Expected login to succeed, got %s", err)
	}
}

func TestRegistryLoginInvalidCredentials(t *testing.T) {
	defer helm.RepoServerStopAll()
	srv, _ := helm.RepoOciServerStart(&helm.RepoServerOptions{})
	_, err := helm.RegistryLogin(&helm.RegistryOptions{
		Hostname: srv.RegistryURL,
		Username: "username",
		Password: "invalid",
	})
	if err == nil {
		t.Fatal("Expected login to fail")
	}
	if !strings.Contains(err.Error(), "401: unauthorized: authentication required") {
		t.Errorf("Expected login to fail with authentication error, got %s", err.Error())
	}
}

func TestRegistryLogout(t *testing.T) {
	defer helm.RepoServerStopAll()
	srv, _ := helm.RepoOciServerStart(&helm.RepoServerOptions{})
	_, err := helm.RegistryLogin(&helm.RegistryOptions{
		Hostname: srv.RegistryURL,
		Username: "username",
		Password: "password",
	})
	if err != nil {
		t.Error("Expected initial login to succeed")
	}
	var out string
	out, err = helm.RegistryLogout(&helm.RegistryOptions{
		Hostname: srv.RegistryURL,
	})
	if err != nil {
		t.Errorf("Expected logout to succeed, got %s", err)
	}
	if !strings.Contains(out, "Removing login credentials for") {
		t.Errorf("Expected logout to succeed, got %s", out)
	}
}

func TestRepoAdd(t *testing.T) {
	repositoryConfigFile, _ := os.CreateTemp("", "repositories.yaml")
	defer func(name string) { _ = os.Remove(name) }(repositoryConfigFile.Name())
	err := helm.RepoAdd(&helm.RepoOptions{
		Name:                  "helm",
		Url:                   "https://charts.helm.sh/stable",
		InsecureSkipTlsVerify: true,
		RepositoryConfig:      repositoryConfigFile.Name(),
	})
	if err != nil {
		t.Errorf("Expected repo add to succeed, got %s", err)
		return
	}
	fileContent, _ := os.ReadFile(repositoryConfigFile.Name())
	repositoryConfig := string(fileContent)
	if !strings.Contains(repositoryConfig, " name: helm\n") {
		t.Errorf("Expected file to contain 'helm' as name, got %s", repositoryConfig)
	}
	if !strings.Contains(repositoryConfig, " url: https://charts.helm.sh/stable\n") {
		t.Errorf("Expected file to contain 'https://charts.helm.sh/stable' as url, got %s", repositoryConfig)
	}
}

func TestRepoAddInvalidName(t *testing.T) {
	repositoryConfigFile, _ := os.CreateTemp("", "repositories.yaml")
	defer func(name string) { _ = os.Remove(name) }(repositoryConfigFile.Name())
	err := helm.RepoAdd(&helm.RepoOptions{
		Name:             "helm/invalid",
		Url:              "https://charts.helm.sh/stable",
		RepositoryConfig: repositoryConfigFile.Name(),
	})
	if err == nil {
		t.Error("Expected repo add to fail")
		return
	}
	if !strings.Contains(err.Error(), "repository name (helm/invalid) contains '/'") {
		t.Errorf("Expected invalid name error, got %s", err.Error())
	}
}

func TestRepoAddInvalidPath(t *testing.T) {
	err := helm.RepoAdd(&helm.RepoOptions{
		Name:             "helm",
		Url:              "https://charts.helm.sh/stable",
		RepositoryConfig: "/invalid-path/invalid-file",
	})
	if err == nil {
		t.Error("Expected repo add to fail")
		return
	}
}

func TestRepoAddInvalidRepo(t *testing.T) {
	repositoryConfigFile, _ := os.CreateTemp("", "repositories.yaml")
	defer func(name string) { _ = os.Remove(name) }(repositoryConfigFile.Name())
	err := helm.RepoAdd(&helm.RepoOptions{
		Name:             "helm",
		Url:              "https://localhost/stable",
		RepositoryConfig: repositoryConfigFile.Name(),
	})
	if err == nil {
		t.Error("Expected repo add to fail")
		return
	}
	if !strings.Contains(err.Error(), "looks like \"https://localhost/stable\" is not a valid chart repository or cannot be reached") {
		t.Errorf("Expected invalid repo error, got %s", err.Error())
	}
}

func TestRepoList(t *testing.T) {
	repositoryConfigFile, _ := os.CreateTemp("", "repositories.yaml")
	defer func(name string) { _ = os.Remove(name) }(repositoryConfigFile.Name())
	_, _ = repositoryConfigFile.WriteString("apiVersion: \"\"\n" +
		"repositories:\n" +
		"  - name: stable\n" +
		"    url: https://charts.helm.sh/stable\n" +
		"  - name: other\n" +
		"    url: https://charts.example.com/other\n")
	out, err := helm.RepoList(&helm.RepoOptions{RepositoryConfig: repositoryConfigFile.Name()})
	if err != nil {
		t.Error("Expected repo list to succeed")
	}
	if !strings.Contains(out, "name=stable&password=&url=https%3A%2F%2Fcharts.helm.sh%2Fstable") {
		t.Errorf("Expected out to contain encoded 'stable' repo, got %s", out)
	}
	if !strings.Contains(out, "name=other&password=&url=https%3A%2F%2Fcharts.example.com%2Fother") {
		t.Errorf("Expected out to contain encoded 'other' repo, got %s", out)
	}
}

func TestRepoRemove(t *testing.T) {
	repositoryConfigFile, _ := os.CreateTemp("", "repositories.yaml")
	defer func(name string) { _ = os.Remove(name) }(repositoryConfigFile.Name())
	_, _ = repositoryConfigFile.WriteString("apiVersion: \"\"\n" +
		"repositories:\n" +
		"  - name: stable\n" +
		"    url: https://charts.helm.sh/stable\n" +
		"  - name: other\n" +
		"    url: https://charts.example.com/other\n")
	err := helm.RepoRemove(&helm.RepoOptions{
		RepositoryConfig: repositoryConfigFile.Name(),
		Names:            "stable",
	})
	if err != nil {
		t.Error("Expected repo remove to succeed")
		return
	}
	out, _ := helm.RepoList(&helm.RepoOptions{RepositoryConfig: repositoryConfigFile.Name()})
	if strings.Contains(out, "name=stable") {
		t.Errorf("Expected 'stable' repo to be removed, got %s", out)
	}
	if !strings.Contains(out, "name=other") {
		t.Errorf("Expected 'other' repo to stay, got %s", out)
	}
}

func TestRepoRemoveWithMissing(t *testing.T) {
	repositoryConfigFile, _ := os.CreateTemp("", "repositories.yaml")
	defer func(name string) { _ = os.Remove(name) }(repositoryConfigFile.Name())
	_, _ = repositoryConfigFile.WriteString("apiVersion: \"\"\n" +
		"repositories:\n" +
		"  - name: stable\n" +
		"    url: https://charts.helm.sh/stable\n" +
		"  - name: other\n" +
		"    url: https://charts.example.com/other\n")
	err := helm.RepoRemove(&helm.RepoOptions{
		RepositoryConfig: repositoryConfigFile.Name(),
		Names:            "missing\nstable",
	})
	if err == nil {
		t.Error("Expected repo remove to fail")
		return
	}
	if !strings.Contains(err.Error(), "no repo named \"missing\" found") {
		t.Errorf("Expected invalid repo error, got %s", err.Error())
	}
}

func TestRepoUpdateAll(t *testing.T) {
	repositoryConfigFile, _ := os.CreateTemp("", "repositories.yaml")
	defer func(name string) { _ = os.Remove(name) }(repositoryConfigFile.Name())
	_, _ = repositoryConfigFile.WriteString("apiVersion: \"\"\n" +
		"repositories:\n" +
		"  - name: stable\n" +
		"    url: https://charts.helm.sh/stable\n")
	updated, err := helm.RepoUpdate(&helm.RepoOptions{
		RepositoryConfig: repositoryConfigFile.Name(),
	})
	if err != nil {
		t.Error("Expected repo update to succeed")
	}
	if !strings.Contains(updated, "&name=stable&") {
		t.Errorf("Expected to update stable repository, got %s", updated)
		return
	}
}

func TestRepoUpdateByName(t *testing.T) {
	repositoryConfigFile, _ := os.CreateTemp("", "repositories.yaml")
	defer func(name string) { _ = os.Remove(name) }(repositoryConfigFile.Name())
	_, _ = repositoryConfigFile.WriteString("apiVersion: \"\"\n" +
		"repositories:\n" +
		"  - name: stable\n" +
		"    url: https://charts.helm.sh/stable\n" +
		"  - name: other\n" +
		"    url: https://charts.example.com/other\n")
	updated, err := helm.RepoUpdate(&helm.RepoOptions{
		RepositoryConfig: repositoryConfigFile.Name(),
		Names:            "stable\n",
	})
	if err != nil {
		t.Errorf("Expected repo update to succeed")
	}
	if !strings.Contains(updated, "&name=stable&") {
		t.Errorf("Expected to update stable repository, got %s", updated)
		return
	}
}

func TestRepoUpdateByNameWithInvalidRepo(t *testing.T) {
	repositoryConfigFile, _ := os.CreateTemp("", "repositories.yaml")
	defer func(name string) { _ = os.Remove(name) }(repositoryConfigFile.Name())
	_, _ = repositoryConfigFile.WriteString("apiVersion: \"\"\n" +
		"repositories:\n" +
		"  - name: stable\n" +
		"    url: https://charts.helm.sh/stable\n" +
		"  - name: other\n" +
		"    url: https://charts.example.com/other\n")
	updated, err := helm.RepoUpdate(&helm.RepoOptions{
		RepositoryConfig: repositoryConfigFile.Name(),
		Names:            "other\n",
	})
	if err == nil {
		t.Error("Expected repo update to fail")
	}
	if !strings.Contains(err.Error(), "failed to update the following repositories:") ||
		!strings.Contains(err.Error(), "https://charts.example.com/other") {
		t.Errorf("Expected error to contain invalid chart, got %s", err.Error())
		return
	}
	if updated != "" {
		t.Errorf("Expected updated chart list to return empty, got %s", updated)
		return
	}
}

func TestRepoUpdateByNameWithInvalidAndValidRepo(t *testing.T) {
	repositoryConfigFile, _ := os.CreateTemp("", "repositories.yaml")
	defer func(name string) { _ = os.Remove(name) }(repositoryConfigFile.Name())
	_, _ = repositoryConfigFile.WriteString("apiVersion: \"\"\n" +
		"repositories:\n" +
		"  - name: stable\n" +
		"    url: https://charts.helm.sh/stable\n" +
		"  - name: other\n" +
		"    url: https://charts.example.com/other\n")
	updated, err := helm.RepoUpdate(&helm.RepoOptions{
		RepositoryConfig: repositoryConfigFile.Name(),
		Names:            "other\nstable\n",
	})
	if err == nil {
		t.Error("Expected repo update to fail")
	}
	if !strings.Contains(err.Error(), "failed to update the following repositories:") ||
		!strings.Contains(err.Error(), "https://charts.example.com/other") {
		t.Errorf("Expected error to contain invalid chart, got %s", err.Error())
		return
	}
	if !strings.Contains(updated, "&name=stable&") {
		t.Errorf("Expected to updated stable repository, got %s", updated)
		return
	}
}

func repoServerStartAsync(t *testing.T, serverInfoChannel chan *repotest.Server, stopChannel chan bool) {
	srv, err := helm.RepoServerStart(&helm.RepoServerOptions{})
	if err != nil {
		t.Errorf("Expected server to be started")
	}
	serverInfoChannel <- srv
	for {
		select {
		case <-stopChannel:
			helm.RepoServerStop(srv.URL())
			return
		default:
			time.Sleep(10 * time.Millisecond)
		}
	}
}

func TestRepoServerStart(t *testing.T) {
	defer helm.RepoServerStopAll()
	serverInfoChannel := make(chan *repotest.Server)
	stopChannel := make(chan bool)
	defer func() { stopChannel <- true }()
	go repoServerStartAsync(t, serverInfoChannel, stopChannel)
	time.Sleep(100 * time.Millisecond)
	serverInfo := <-serverInfoChannel
	resp, err := http.Get(serverInfo.URL())
	if err != nil {
		t.Errorf("Expected server to be started")
	}
	if resp.StatusCode != 200 {
		t.Errorf("Expected server to return 200, got %d", resp.StatusCode)
	}
}

func TestRepoServerStartMultipleInstances(t *testing.T) {
	defer helm.RepoServerStopAll()
	serverInfoChannel := make(chan *repotest.Server)
	stopChannel := make(chan bool)
	defer func() { stopChannel <- true }()
	defer helm.RepoServerStopAll()
	go repoServerStartAsync(t, serverInfoChannel, stopChannel)
	srv1 := <-serverInfoChannel
	go repoServerStartAsync(t, serverInfoChannel, stopChannel)
	time.Sleep(100 * time.Millisecond) // ensure servers are started and continue to run
	srv2 := <-serverInfoChannel
	resp1, err1 := http.Get(srv1.URL())
	resp2, err2 := http.Get(srv2.URL())
	if err1 != nil || err2 != nil {
		t.Errorf("Expected all servers to be started")
	}
	if resp1.StatusCode != 200 || resp2.StatusCode != 200 {
		t.Errorf("Expected all server to return 200, got %d and %d", resp1.StatusCode, resp2.StatusCode)
	}
}

func TestRepoServerStop(t *testing.T) {
	serverInfoChannel := make(chan *repotest.Server)
	stopChannel := make(chan bool)
	defer func() { stopChannel <- true }()
	go repoServerStartAsync(t, serverInfoChannel, stopChannel)
	time.Sleep(100 * time.Millisecond)
	serverInfo := <-serverInfoChannel
	helm.RepoServerStop(serverInfo.URL())
	_, err := http.Get(serverInfo.URL())
	if err == nil {
		t.Errorf("Expected server to be stopped")
	}
}

func TestRepoServerStopAll(t *testing.T) {
	srv1, _ := helm.RepoServerStart(&helm.RepoServerOptions{})
	srv2, _ := helm.RepoServerStart(&helm.RepoServerOptions{})
	helm.RepoServerStopAll()
	_, err := http.Get(srv1.URL())
	_, err2 := http.Get(srv2.URL())
	if err == nil || err2 == nil {
		t.Errorf("Expected all servers to be stopped")
	}
}

func TestSearchRepo(t *testing.T) {
	repositoryConfigFile, _ := os.CreateTemp("", "repositories.yaml")
	err := helm.RepoAdd(&helm.RepoOptions{
		Name:                  "helm",
		Url:                   "https://charts.helm.sh/stable",
		InsecureSkipTlsVerify: true,
		RepositoryConfig:      repositoryConfigFile.Name(),
	})
	out, err := helm.SearchRepo(&helm.SearchOptions{
		Keyword:          "nginx",
		RepositoryConfig: repositoryConfigFile.Name(),
	})
	if err != nil {
		t.Errorf("Expected search to succeed, got %s", err)
		return
	}
	if !strings.Contains(out, "appVersion=") {
		t.Errorf("Expected search to contain 'appVersion=', got %s", out)
		return
	}
	if !strings.Contains(out, "&description=") {
		t.Errorf("Expected search to contain '&description=', got %s", out)
		return
	}
	if !strings.Contains(out, "&name=") {
		t.Errorf("Expected search to contain '&name=', got %s", out)
		return
	}
}

func TestShowAllLocal(t *testing.T) {
	create, _ := helm.Create(&helm.CreateOptions{
		Name: "test",
		Dir:  t.TempDir(),
	})
	out, err := helm.Show(&helm.ShowOptions{
		Path:         create,
		OutputFormat: "all",
	})
	if err != nil {
		t.Errorf("Expected show to succeed, got %s", err)
		return
	}
	if !strings.Contains(out, "apiVersion:") || !strings.Contains(out, "name: test") {
		t.Errorf("Expected show to contain chart info, got %s", out)
		return
	}
}
