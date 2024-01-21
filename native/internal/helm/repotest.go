package helm

import (
	"github.com/orcaman/concurrent-map/v2"
	"helm.sh/helm/v3/pkg/repo/repotest"
	"testing"
)

var servers = cmap.New[*repotest.Server]()

func TestRepoServerStart() (*repotest.Server, error) {
	server, err := repotest.NewTempServerWithCleanup(&testing.T{}, "")
	if err != nil {
		return nil, err
	}
	servers.Set(server.URL(), server)
	return server, nil
}

func TestRepoServerStop(url string) {
	if server, _ := servers.Get(url); server != nil {
		server.Stop()
		servers.Remove(url)
	}
}

func TestRepoServerStopAll() {
	for server := range servers.IterBuffered() {
		server.Val.Stop()
	}
	servers.Clear()
}
