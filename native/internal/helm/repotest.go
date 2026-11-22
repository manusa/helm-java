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
	"context"
	"fmt"
	"github.com/distribution/distribution/v3/configuration"
	"github.com/distribution/distribution/v3/registry"
	"github.com/orcaman/concurrent-map/v2"
	"github.com/phayes/freeport"
	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
	"golang.org/x/crypto/bcrypt"
	"helm.sh/helm/v3/pkg/repo/repotest"
	"io"
	"net/http"
	"os"
	"path/filepath"
	"testing"
	"time"
)

var servers = cmap.New[*ActiveServer]()

const ociServerShutdownTimeout = 5 * time.Second

type RepoServerOptions struct {
	Glob     string
	Username string
	Password string
}

type ActiveServer struct {
	id        string
	server    *repotest.Server
	ociServer *repotest.OCIServer
	dir       string
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
	servers.Set(server.URL(), &ActiveServer{
		id:     server.URL(),
		server: server,
		dir:    server.Root(),
	})
	return server, nil
}

func RepoOciServerStart(options *RepoServerOptions) (*repotest.OCIServer, error) {
	logrus.SetOutput(io.Discard)
	if options.Username == "" {
		options.Username = "username"
	}
	if options.Password == "" {
		options.Password = "password"
	}
	var err error
	var testDir string
	if testDir, err = os.MkdirTemp("", "helm-repotest-"); err != nil {
		return nil, errors.Wrap(err, "Error creating temp dir")
	}
	var pwBytes []byte
	if pwBytes, err = bcrypt.GenerateFromPassword([]byte(options.Password), bcrypt.DefaultCost); err != nil {
		return nil, errors.Wrap(err, "Error generating password hash")
	}
	htpasswdPath := filepath.Join(testDir, "authtest.htpasswd")
	if err = os.WriteFile(htpasswdPath, []byte(fmt.Sprintf("%s:%s\n", options.Username, string(pwBytes))), 0644); err != nil {
		return nil, errors.Wrap(err, "Error writing htpasswd file")
	}
	var port int
	if port, err = freeport.GetFreePort(); err != nil {
		return nil, errors.Wrap(err, "Error getting free port")
	}
	config := &configuration.Configuration{}
	config.HTTP.Addr = fmt.Sprintf(":%d", port)
	// config.HTTP.DrainTimeout is deliberately left at 0: with a non-zero value, distribution's
	// Registry.ListenAndServe calls signal.Notify for SIGINT/SIGTERM, which in a c-shared library
	// takes over the host process' (e.g. the JVM's) handlers for good. With 0 it serves directly.
	// See https://github.com/manusa/helm-java/issues/409
	config.Storage = map[string]configuration.Parameters{"inmemory": map[string]interface{}{}}
	config.Auth = configuration.Auth{
		"htpasswd": configuration.Parameters{
			"realm": "localhost",
			"path":  htpasswdPath,
		},
	}
	var reg *registry.Registry
	if reg, err = registry.NewRegistry(context.Background(), config); err != nil {
		return nil, errors.Wrap(err, "Error creating registry")
	}
	server := &repotest.OCIServer{
		Registry:     reg,
		RegistryURL:  fmt.Sprintf("localhost:%d", port),
		TestUsername: options.Username,
		TestPassword: options.Password,
		Dir:          testDir,
	}
	go server.ListenAndServe()
	servers.Set(server.RegistryURL, &ActiveServer{
		id:        server.RegistryURL,
		ociServer: server,
		dir:       server.Dir,
	})
	return server, nil
}

func serverStop(server *ActiveServer) {
	if server.server != nil {
		server.server.Stop()
	}
	if server.ociServer != nil {
		// Closes the listener and lets ListenAndServe return; bounded in case a connection never goes idle
		ctx, cancel := context.WithTimeout(context.Background(), ociServerShutdownTimeout)
		_ = server.ociServer.Shutdown(ctx)
		cancel()
	}
	_ = os.RemoveAll(server.dir)
}

func RepoServerStop(url string) {
	if server, _ := servers.Get(url); server != nil {
		serverStop(server)
		servers.Remove(url)
	}
}

func RepoServerStopAll() {
	for server := range servers.IterBuffered() {
		serverStop(server.Val)
	}
	servers.Clear()
}
