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

package test

import (
	"os"
	"path/filepath"
	"testing"
)

// RepositoryConfig returns the path of a repositories.yaml file in a directory that is removed when the test
// ends, together with the lock file that repository commands create next to it.
// The file is created with the given content, or not at all if the content is empty (RepoAdd creates it).
func RepositoryConfig(t *testing.T, content string) string {
	t.Helper()
	repositoryConfig := filepath.Join(t.TempDir(), "repositories.yaml")
	if content != "" {
		if err := os.WriteFile(repositoryConfig, []byte(content), 0600); err != nil {
			t.Fatalf("Error writing %s: %s", repositoryConfig, err)
		}
	}
	return repositoryConfig
}
