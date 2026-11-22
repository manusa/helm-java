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
	"strings"
	"testing"
	"time"

	"github.com/manusa/helm-java/native/internal/helm"
	"github.com/manusa/helm-java/native/internal/test"
)

// TestMain sets up an isolated environment for tests by clearing Helm-related
// environment variables and setting up temporary directories
func TestMain(m *testing.M) {
	cleanup, err := test.SetupIsolatedEnv()
	if err != nil {
		panic(err)
	}
	// Not deferred: os.Exit skips deferred calls, which left the isolated directories behind
	code := m.Run()
	cleanup()
	os.Exit(code)
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
		Hostname:    srv.RegistryURL,
		Username:    "username",
		Password:    "password",
		Debug:       true,
		CertOptions: helm.CertOptions{PlainHttp: true},
	})
	out, err := helm.Push(&helm.PushOptions{
		Chart:       path.Join(dir, "test-0.1.0.tgz"),
		Remote:      "oci://" + srv.RegistryURL,
		Debug:       true,
		CertOptions: helm.CertOptions{PlainHttp: true},
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
	_, err := helm.Push(&helm.PushOptions{
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
	if !strings.Contains(err.Error(), "level=DEBUG") || !strings.Contains(err.Error(), `status="401 Unauthorized"`) {
		t.Errorf("Expected out to contain debug info, got %v", err)
	}
}

func TestRegistryLogin(t *testing.T) {
	defer helm.RepoServerStopAll()
	srv, err := helm.RepoOciServerStart(&helm.RepoServerOptions{})
	if err != nil {
		t.Errorf("Expected server to be started")
	}
	_, err = helm.RegistryLogin(&helm.RegistryOptions{
		Hostname:    srv.RegistryURL,
		Username:    "username",
		Password:    "password",
		CertOptions: helm.CertOptions{PlainHttp: true},
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
		Hostname:    srv.RegistryURL,
		Username:    "username",
		Password:    "password",
		CertOptions: helm.CertOptions{PlainHttp: true},
	})
	if err != nil {
		t.Error("Expected initial login to succeed")
	}
	var out string
	out, err = helm.RegistryLogout(&helm.RegistryOptions{
		Hostname:    srv.RegistryURL,
		CertOptions: helm.CertOptions{PlainHttp: true},
	})
	if err != nil {
		t.Errorf("Expected logout to succeed, got %s", err)
	}
	if !strings.Contains(out, "Removing login credentials for") {
		t.Errorf("Expected logout to succeed, got %s", out)
	}
}

func TestRepoAdd(t *testing.T) {
	repositoryConfigFile := test.RepositoryConfig(t, "")
	err := helm.RepoAdd(&helm.RepoOptions{
		Name:                  "helm",
		Url:                   "https://charts.helm.sh/stable",
		InsecureSkipTlsVerify: true,
		RepositoryConfig:      repositoryConfigFile,
	})
	if err != nil {
		t.Errorf("Expected repo add to succeed, got %s", err)
		return
	}
	fileContent, _ := os.ReadFile(repositoryConfigFile)
	repositoryConfig := string(fileContent)
	if !strings.Contains(repositoryConfig, " name: helm\n") {
		t.Errorf("Expected file to contain 'helm' as name, got %s", repositoryConfig)
	}
	if !strings.Contains(repositoryConfig, " url: https://charts.helm.sh/stable\n") {
		t.Errorf("Expected file to contain 'https://charts.helm.sh/stable' as url, got %s", repositoryConfig)
	}
}

func TestRepoAddInvalidName(t *testing.T) {
	repositoryConfigFile := test.RepositoryConfig(t, "")
	err := helm.RepoAdd(&helm.RepoOptions{
		Name:             "helm/invalid",
		Url:              "https://charts.helm.sh/stable",
		RepositoryConfig: repositoryConfigFile,
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
	repositoryConfigFile := test.RepositoryConfig(t, "")
	err := helm.RepoAdd(&helm.RepoOptions{
		Name:             "helm",
		Url:              "https://localhost/stable",
		RepositoryConfig: repositoryConfigFile,
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
	repositoryConfigFile := test.RepositoryConfig(t, "apiVersion: \"\"\n"+
		"repositories:\n"+
		"  - name: stable\n"+
		"    url: https://charts.helm.sh/stable\n"+
		"  - name: other\n"+
		"    url: https://charts.example.com/other\n")
	out, err := helm.RepoList(&helm.RepoOptions{RepositoryConfig: repositoryConfigFile})
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
	repositoryConfigFile := test.RepositoryConfig(t, "apiVersion: \"\"\n"+
		"repositories:\n"+
		"  - name: stable\n"+
		"    url: https://charts.helm.sh/stable\n"+
		"  - name: other\n"+
		"    url: https://charts.example.com/other\n")
	err := helm.RepoRemove(&helm.RepoOptions{
		RepositoryConfig: repositoryConfigFile,
		Names:            "stable",
	})
	if err != nil {
		t.Error("Expected repo remove to succeed")
		return
	}
	out, _ := helm.RepoList(&helm.RepoOptions{RepositoryConfig: repositoryConfigFile})
	if strings.Contains(out, "name=stable") {
		t.Errorf("Expected 'stable' repo to be removed, got %s", out)
	}
	if !strings.Contains(out, "name=other") {
		t.Errorf("Expected 'other' repo to stay, got %s", out)
	}
}

func TestRepoRemoveWithMissing(t *testing.T) {
	repositoryConfigFile := test.RepositoryConfig(t, "apiVersion: \"\"\n"+
		"repositories:\n"+
		"  - name: stable\n"+
		"    url: https://charts.helm.sh/stable\n"+
		"  - name: other\n"+
		"    url: https://charts.example.com/other\n")
	err := helm.RepoRemove(&helm.RepoOptions{
		RepositoryConfig: repositoryConfigFile,
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
	repositoryConfigFile := test.RepositoryConfig(t, "apiVersion: \"\"\n"+
		"repositories:\n"+
		"  - name: stable\n"+
		"    url: https://charts.helm.sh/stable\n")
	updated, err := helm.RepoUpdate(&helm.RepoOptions{
		RepositoryConfig: repositoryConfigFile,
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
	repositoryConfigFile := test.RepositoryConfig(t, "apiVersion: \"\"\n"+
		"repositories:\n"+
		"  - name: stable\n"+
		"    url: https://charts.helm.sh/stable\n"+
		"  - name: other\n"+
		"    url: https://charts.example.com/other\n")
	updated, err := helm.RepoUpdate(&helm.RepoOptions{
		RepositoryConfig: repositoryConfigFile,
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
	repositoryConfigFile := test.RepositoryConfig(t, "apiVersion: \"\"\n"+
		"repositories:\n"+
		"  - name: stable\n"+
		"    url: https://charts.helm.sh/stable\n"+
		"  - name: other\n"+
		"    url: https://charts.example.com/other\n")
	updated, err := helm.RepoUpdate(&helm.RepoOptions{
		RepositoryConfig: repositoryConfigFile,
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
	repositoryConfigFile := test.RepositoryConfig(t, "apiVersion: \"\"\n"+
		"repositories:\n"+
		"  - name: stable\n"+
		"    url: https://charts.helm.sh/stable\n"+
		"  - name: other\n"+
		"    url: https://charts.example.com/other\n")
	updated, err := helm.RepoUpdate(&helm.RepoOptions{
		RepositoryConfig: repositoryConfigFile,
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

// testHttpClient bounds every request to the test servers; keep-alives are disabled so that a request
// after a server is stopped opens a new connection instead of reusing a pooled one
var testHttpClient = &http.Client{
	Timeout:   10 * time.Second,
	Transport: &http.Transport{DisableKeepAlives: true},
}

// statusCode performs a GET request and returns the response status code
func statusCode(url string) (int, error) {
	resp, err := testHttpClient.Get(url)
	if err != nil {
		return 0, err
	}
	_ = resp.Body.Close()
	return resp.StatusCode, nil
}

// waitForOciServer waits (bounded) until the test OCI server, which starts serving in a goroutine, responds
func waitForOciServer(t *testing.T, registryURL string) {
	t.Helper()
	deadline := time.Now().Add(10 * time.Second)
	for {
		_, err := statusCode("http://" + registryURL + "/v2/")
		if err == nil {
			return
		}
		if time.Now().After(deadline) {
			t.Fatalf("Expected OCI server to be started, got %s", err)
		}
		time.Sleep(50 * time.Millisecond)
	}
}

func TestRepoServerStart(t *testing.T) {
	defer helm.RepoServerStopAll()
	srv, err := helm.RepoServerStart(&helm.RepoServerOptions{})
	if err != nil {
		t.Fatalf("Expected server to be started, got %s", err)
	}
	code, err := statusCode(srv.URL())
	if err != nil {
		t.Fatalf("Expected server to be started, got %s", err)
	}
	if code != 200 {
		t.Errorf("Expected server to return 200, got %d", code)
	}
}

func TestRepoServerStartMultipleInstances(t *testing.T) {
	defer helm.RepoServerStopAll()
	srv1, err1 := helm.RepoServerStart(&helm.RepoServerOptions{})
	srv2, err2 := helm.RepoServerStart(&helm.RepoServerOptions{})
	if err1 != nil || err2 != nil {
		t.Fatalf("Expected all servers to be started, got %v and %v", err1, err2)
	}
	code1, err1 := statusCode(srv1.URL())
	code2, err2 := statusCode(srv2.URL())
	if err1 != nil || err2 != nil {
		t.Fatalf("Expected all servers to be started, got %v and %v", err1, err2)
	}
	if code1 != 200 || code2 != 200 {
		t.Errorf("Expected all server to return 200, got %d and %d", code1, code2)
	}
}

func TestRepoServerStop(t *testing.T) {
	defer helm.RepoServerStopAll()
	srv, err := helm.RepoServerStart(&helm.RepoServerOptions{})
	if err != nil {
		t.Fatalf("Expected server to be started, got %s", err)
	}
	helm.RepoServerStop(srv.URL())
	if _, err := statusCode(srv.URL()); err == nil {
		t.Errorf("Expected server to be stopped")
	}
}

func TestRepoServerStopWithOciServer(t *testing.T) {
	defer helm.RepoServerStopAll()
	srv, err := helm.RepoOciServerStart(&helm.RepoServerOptions{})
	if err != nil {
		t.Fatalf("Expected OCI server to be started, got %s", err)
	}
	waitForOciServer(t, srv.RegistryURL)
	helm.RepoServerStop(srv.RegistryURL)
	if _, err := statusCode("http://" + srv.RegistryURL + "/v2/"); err == nil {
		t.Errorf("Expected OCI server to be stopped")
	}
	if _, err := os.Stat(srv.Dir); !os.IsNotExist(err) {
		t.Errorf("Expected OCI server directory %s to be removed, got %v", srv.Dir, err)
	}
}

func TestRepoServerStopAll(t *testing.T) {
	defer helm.RepoServerStopAll() // in case the test fails before stopping them
	srv1, err1 := helm.RepoServerStart(&helm.RepoServerOptions{})
	srv2, err2 := helm.RepoOciServerStart(&helm.RepoServerOptions{})
	if err1 != nil || err2 != nil {
		t.Fatalf("Expected all servers to be started, got %v and %v", err1, err2)
	}
	waitForOciServer(t, srv2.RegistryURL)
	helm.RepoServerStopAll()
	_, err1 = statusCode(srv1.URL())
	_, err2 = statusCode("http://" + srv2.RegistryURL + "/v2/")
	if err1 == nil || err2 == nil {
		t.Errorf("Expected all servers to be stopped")
	}
}

func TestSearchRepo(t *testing.T) {
	repositoryConfig := test.RepositoryConfig(t, "")
	err := helm.RepoAdd(&helm.RepoOptions{
		Name:                  "helm",
		Url:                   "https://charts.helm.sh/stable",
		InsecureSkipTlsVerify: true,
		RepositoryConfig:      repositoryConfig,
	})
	if err != nil {
		t.Fatalf("Expected repo add to succeed, got %s", err)
	}
	out, err := helm.SearchRepo(&helm.SearchOptions{
		Keyword:          "nginx",
		RepositoryConfig: repositoryConfig,
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
