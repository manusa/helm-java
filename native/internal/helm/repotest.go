package helm

import (
	"github.com/orcaman/concurrent-map/v2"
	"helm.sh/helm/v3/pkg/repo/repotest"
	"net/http"
	"testing"
)

var servers = cmap.New[*repotest.Server]()

type RepoServerOptions struct {
	Glob     string
	Username string
	Password string
}

func RepoTempServerStart(options *RepoServerOptions) (*repotest.Server, error) {
	server, err := repotest.NewTempServerWithCleanup(&testing.T{}, options.Glob)
	if err != nil {
		return nil, err
	}
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

func RepoServerStop(url string) {
	if server, _ := servers.Get(url); server != nil {
		server.Stop()
		servers.Remove(url)
	}
}

func RepoServerStopAll() {
	for server := range servers.IterBuffered() {
		server.Val.Stop()
	}
	servers.Clear()
}
