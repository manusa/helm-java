package helm

import (
	"errors"
	"helm.sh/helm/v3/pkg/repo/repotest"
	"testing"
)

var server *repotest.Server

func TestServerStart() (*repotest.Server, error) {
	if server != nil {
		return server, errors.New("server already started, only one instance allowed")
	}
	srv, err := repotest.NewTempServerWithCleanup(&testing.T{}, "")
	if err != nil {
		return nil, err
	}
	server = srv
	return server, nil
}

func TestServerStop() {
	if server != nil {
		server.Stop()
		server = nil
	}
}
