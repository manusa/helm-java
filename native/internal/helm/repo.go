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
	"bytes"
	"context"
	"fmt"
	"github.com/pkg/errors"
	"helm.sh/helm/v3/pkg/cli"
	"helm.sh/helm/v3/pkg/getter"
	"helm.sh/helm/v3/pkg/repo"
	"net/url"
	"os"
	"path/filepath"
	"strconv"
	"strings"
	"sync"
	"time"

	"github.com/gofrs/flock"
	"sigs.k8s.io/yaml"
)

type RepoOptions struct {
	RepositoryConfig      string
	Name                  string
	Names                 string
	Url                   string
	Username              string
	Password              string
	CertFile              string
	KeyFile               string
	CaFile                string
	InsecureSkipTlsVerify bool
}

func RepoAdd(options *RepoOptions) error {
	// https://github.com/helm/helm/blob/d7805e68ae646e60411dad365a7de8baa728e631/cmd/helm/repo_add.go#L101
	// Implementation logic is in cmd package
	repoFile := repositoryConfig(options)
	// Ensure the file directory exists as it is required for file locking
	err := os.MkdirAll(filepath.Dir(repoFile), os.ModePerm)
	if err != nil && !os.IsExist(err) {
		return err
	}

	// Acquire a file lock for process synchronization
	repoFileExt := filepath.Ext(repoFile)
	var lockPath string
	if len(repoFileExt) > 0 && len(repoFileExt) < len(repoFile) {
		lockPath = strings.TrimSuffix(repoFile, repoFileExt) + ".lock"
	} else {
		lockPath = repoFile + ".lock"
	}
	fileLock := flock.New(lockPath)
	lockCtx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()
	locked, err := fileLock.TryLockContext(lockCtx, time.Second)
	if err == nil && locked {
		defer fileLock.Unlock()
	}
	if err != nil {
		return err
	}

	b, err := os.ReadFile(repoFile)
	if err != nil && !os.IsNotExist(err) {
		return err
	}

	var f repo.File
	if err := yaml.Unmarshal(b, &f); err != nil {
		return err
	}

	c := repo.Entry{
		Name:                  options.Name,
		URL:                   options.Url,
		Username:              options.Username,
		Password:              options.Password,
		CertFile:              options.CertFile,
		KeyFile:               options.KeyFile,
		CAFile:                options.CaFile,
		InsecureSkipTLSverify: options.InsecureSkipTlsVerify,
	}

	// Check if the repo name is legal
	if strings.Contains(options.Name, "/") {
		return errors.Errorf(
			"repository name (%s) contains '/', please specify a different name without '/'", options.Name)
	}

	r, err := repo.NewChartRepository(&c, getter.All(cli.New()))
	if err != nil {
		return err
	}

	if _, err := r.DownloadIndexFile(); err != nil {
		return errors.Wrapf(err, "looks like %q is not a valid chart repository or cannot be reached", options.Url)
	}

	f.Update(&c)

	if err := f.WriteFile(repoFile, 0600); err != nil {
		return err
	}
	return nil
}

func RepoList(options *RepoOptions) (string, error) {
	f, err := repo.LoadFile(repositoryConfig(options))
	if err != nil {
		return "", err
	}
	out := bytes.NewBuffer(make([]byte, 0))
	for _, repository := range f.Repositories {
		values := make(url.Values)
		values.Set("name", repository.Name)
		values.Set("url", repository.URL)
		values.Set("username", repository.Username)
		values.Set("password", repository.Password)
		values.Set("insecureSkipTlsVerify", strconv.FormatBool(repository.InsecureSkipTLSverify))
		_, _ = fmt.Fprintln(out, values.Encode())
	}
	return out.String(), nil
}

func RepoRemove(options *RepoOptions) error {
	if len(options.Names) == 0 {
		return nil
	}
	repoFile := repositoryConfig(options)
	r, err := repo.LoadFile(repoFile)
	if err != nil {
		return err
	}
	for _, name := range strings.Split(options.Names, "\n") {
		if !r.Remove(name) {
			return errors.Errorf("no repo named %q found", name)
		}
		if err := r.WriteFile(repoFile, 0600); err != nil {
			return err
		}
	}
	return nil
}

func RepoUpdate(options *RepoOptions) error {
	repoFile := repositoryConfig(options)
	r, err := repo.LoadFile(repoFile)
	if err != nil {
		return err
	}

	var repos []*repo.ChartRepository
	updateAllRepos := len(options.Names) == 0

	// Grab list of repositories to update
	for _, repositoryFile := range r.Repositories {
		var addRepo bool
		if updateAllRepos {
			addRepo = true
		} else {
			for _, name := range strings.Split(options.Names, "\n") {
				if name == repositoryFile.Name {
					addRepo = true
					break
				}
			}
		}
		if addRepo {
			repo, err := repo.NewChartRepository(repositoryFile, getter.All(cli.New()))
			if err != nil {
				return err
			}
			repos = append(repos, repo)
		}
	}

	// Update the repositories
	var wg sync.WaitGroup
	var repoFailList []string
	for _, re := range repos {
		wg.Add(1)
		go func(re *repo.ChartRepository) {
			defer wg.Done()
			if _, err := re.DownloadIndexFile(); err != nil {
				repoFailList = append(repoFailList, re.Config.URL)
			}
		}(re)
	}
	wg.Wait()
	if len(repoFailList) > 0 {
		return fmt.Errorf("failed to update the following repositories: %s", repoFailList)
	}
	return nil
}
