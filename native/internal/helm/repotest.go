package helm

import (
	"github.com/orcaman/concurrent-map/v2"
	"github.com/sirupsen/logrus"
	"helm.sh/helm/v3/pkg/repo/repotest"
	"io"
	"net/http"
	"os"
	"testing"
)

var servers = cmap.New[*repotest.Server]()
var ociServers = cmap.New[*repotest.OCIServer]()

type RepoServerOptions struct {
	Glob     string
	Username string
	Password string
}

func RepoServerStart(options *RepoServerOptions) (*repotest.Server, error) {
	logrus.SetOutput(io.Discard)
	server, err := repotest.NewTempServerWithCleanup(&testing.T{}, options.Glob)
	if err != nil {
		return nil, err
	}
	logrus.SetLevel(logrus.ErrorLevel)
	if len(options.Username) > 0 && len(options.Password) > 0 {
		server.Stop()
		server.WithMiddleware(func(w http.ResponseWriter, r *http.Request) {
			username, password, ok := r.BasicAuth()
			if !ok || username != options.Username || password != options.Password {
				w.WriteHeader(http.StatusUnauthorized)
			}
		})
		server.Start()
	}
	servers.Set(server.URL(), server)
	return server, nil
}

func RepoOciServerStart(options *RepoServerOptions) (*repotest.OCIServer, error) {
	logrus.SetOutput(io.Discard)
	tdir, err := os.MkdirTemp("", "helm-repotest-")
	server, err := repotest.NewOCIServer(&testing.T{}, tdir)
	if err != nil {
		return nil, err
	}
	go server.ListenAndServe()
	ociServers.Set(server.RegistryURL, server)
	return server, nil
}

func serverStop(srv *repotest.Server) {
	srv.Stop()
	_ = os.RemoveAll(srv.Root())
}

func ociServerStop(srv *repotest.OCIServer) {
	// srv.Stop() //TODO can't be stopped for now ¯\_(ツ)_/¯
	_ = os.RemoveAll(srv.Dir)
}

func RepoServerStop(url string) {
	if server, _ := servers.Get(url); server != nil {
		serverStop(server)
		servers.Remove(url)
	}
	if ociServer, _ := ociServers.Get(url); ociServer != nil {
		ociServerStop(ociServer)
		ociServers.Remove(url)
	}
}

func RepoServerStopAll() {
	for server := range servers.IterBuffered() {
		serverStop(server.Val)
	}
	for server := range ociServers.IterBuffered() {
		ociServerStop(server.Val)
	}
	servers.Clear()
}
