package main

import (
	"github.com/manusa/helm-java/native/internal/helm"
	"helm.sh/helm/v3/pkg/repo/repotest"
	"net/http"
	"os"
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

func testServerStart(t *testing.T, serverInfoChannel chan *repotest.Server, stopChannel chan bool) {
	srv, err := helm.TestServerStart()
	if err != nil {
		t.Errorf("Expected server to be started")
	}
	serverInfoChannel <- srv
	for {
		select {
		case <-stopChannel:
			helm.TestServerStop()
			return
		default:
			time.Sleep(10 * time.Millisecond)
		}
	}
}
func TestTestServerStart(t *testing.T) {
	serverInfoChannel := make(chan *repotest.Server)
	stopChannel := make(chan bool)
	defer func() { stopChannel <- true }()
	go testServerStart(t, serverInfoChannel, stopChannel)
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

func TestTestServerStartMultipleInstances(t *testing.T) {
	serverInfoChannel := make(chan *repotest.Server)
	stopChannel := make(chan bool)
	defer func() { stopChannel <- true }()
	go testServerStart(t, serverInfoChannel, stopChannel)
	time.Sleep(100 * time.Millisecond)
	<-serverInfoChannel
	if _, err := helm.TestServerStart(); err == nil {
		t.Errorf("Expected server to return error")
	}
}

func TestTestServerStop(t *testing.T) {
	serverInfoChannel := make(chan *repotest.Server)
	stopChannel := make(chan bool)
	defer func() { stopChannel <- true }()
	go testServerStart(t, serverInfoChannel, stopChannel)
	time.Sleep(100 * time.Millisecond)
	serverInfo := <-serverInfoChannel
	helm.TestServerStop()
	_, err := http.Get(serverInfo.URL())
	if err == nil {
		t.Errorf("Expected server to be stopped")
	}
}
