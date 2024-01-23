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

func TestPush(t *testing.T) {
	srv, _ := helm.RepoOciServerStart(&helm.RepoServerOptions{})
	dir := t.TempDir()
	create, _ := helm.Create(&helm.CreateOptions{
		Name: "test",
		Dir:  dir,
	})
	_ = helm.Package(&helm.PackageOptions{Path: create, Destination: dir})
	_, _ = helm.RegistryLogin(&helm.RegistryLoginOptions{
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
	srv, _ := helm.RepoOciServerStart(&helm.RepoServerOptions{})
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
	if !strings.Contains(err.Error(), "push access denied, repository does not exist or may require authorization") {
		t.Errorf("Expected push to fail with message, got %s", err.Error())
	}
	if !strings.Contains(out, "level=debug") || !strings.Contains(out, "msg=Unauthorized") {
		t.Errorf("Expected out to contain debug info, got %s", out)
	}
}

func TestRegistryLogin(t *testing.T) {
	srv, err := helm.RepoOciServerStart(&helm.RepoServerOptions{})
	if err != nil {
		t.Errorf("Expected server to be started")
	}
	_, err = helm.RegistryLogin(&helm.RegistryLoginOptions{
		Hostname: srv.RegistryURL,
		Username: "username",
		Password: "password",
	})
	if err != nil {
		t.Errorf("Expected login to succeed, got %s", err)
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
