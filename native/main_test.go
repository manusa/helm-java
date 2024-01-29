package main

import (
	"github.com/manusa/helm-java/native/internal/helm"
	"helm.sh/helm/v3/pkg/repo/repotest"
	"net/http"
	"os"
	"path"
	"strings"
	"testing"
	"time"
)

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

func TestInstallDry(t *testing.T) {
	create, _ := helm.Create(&helm.CreateOptions{
		Name: "test",
		Dir:  t.TempDir(),
	})
	out, err := helm.Install(&helm.InstallOptions{
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

func TestInstallValues(t *testing.T) {
	create, _ := helm.Create(&helm.CreateOptions{
		Name: "test",
		Dir:  t.TempDir(),
	})
	out, err := helm.Install(&helm.InstallOptions{
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

func TestInstallDependencyUpdate(t *testing.T) {
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
	out, err := helm.Install(&helm.InstallOptions{
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
		Chart:  path.Join(dir, "test-0.1.0.tgz"),
		Remote: "oci://" + srv.RegistryURL,
		Debug:  true,
	})
	if err == nil {
		t.Error("Expected push to fail")
	}
	if !strings.Contains(err.Error(), "push access denied, repository does not exist or may require authorization") &&
		!(strings.Contains(err.Error(), "unexpected status from HEAD request") && strings.Contains(err.Error(), "401 Unauthorized")) {
		t.Errorf("Expected push to fail with message, got %s", err.Error())
	}
	if !strings.Contains(err.Error(), "level=debug") || !strings.Contains(err.Error(), "msg=Unauthorized") {
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
	if err == nil || !strings.Contains(err.Error(), "failed with status: 401 Unauthorized") {
		t.Error("Expected login to fail")
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
