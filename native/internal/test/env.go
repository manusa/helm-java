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
)

// helmEnvVars contains the list of Helm-related environment variables that should be isolated
var helmEnvVars = []string{
	"HELM_CACHE_HOME",
	"HELM_CONFIG_HOME",
	"HELM_DATA_HOME",
	"HELM_DEBUG",
	"HELM_DRIVER",
	"HELM_KUBEAPISERVER",
	"HELM_KUBECONTEXT",
	"HELM_KUBETOKEN",
	"HELM_NAMESPACE",
	"HELM_REGISTRY_CONFIG",
	"HELM_REPOSITORY_CACHE",
	"HELM_REPOSITORY_CONFIG",
	"KUBECONFIG",
	// Docker/Registry credential storage
	"DOCKER_CONFIG",
	"REGISTRY_AUTH_FILE",
	// GPG-related environment variables
	"GNUPGHOME",
	"GPG_TTY",
	// Pass credential helper (uses GPG)
	"PASSWORD_STORE_DIR",
	"PASSWORD_STORE_GPG_OPTS",
	// Other credential helpers
	"DOCKER_CREDENTIAL_HELPERS",
	// PATH to avoid credential helpers
	"PATH",
}

// EnvCleanup is a function that restores the original environment
type EnvCleanup func()

// SetupIsolatedEnv sets up an isolated environment for Helm tests by clearing
// Helm-related environment variables and setting up temporary directories.
// Returns a cleanup function that should be called to restore the original environment.
func SetupIsolatedEnv() (EnvCleanup, error) {
	// Save original environment
	originalEnv := make(map[string]string)
	for _, envVar := range helmEnvVars {
		if val, exists := os.LookupEnv(envVar); exists {
			originalEnv[envVar] = val
		}
	}

	// Create temporary directories for Helm
	tempDir, err := os.MkdirTemp(os.TempDir(), "helm-test-*")
	if err != nil {
		return nil, err
	}

	helmCacheHome := filepath.Join(tempDir, "cache")
	helmConfigHome := filepath.Join(tempDir, "config")
	helmDataHome := filepath.Join(tempDir, "data")
	dockerConfigHome := filepath.Join(tempDir, "docker")
	gnupgHome := filepath.Join(tempDir, "gnupg")

	// Create the directories
	_ = os.MkdirAll(helmCacheHome, 0755)
	_ = os.MkdirAll(helmConfigHome, 0755)
	_ = os.MkdirAll(helmDataHome, 0755)
	_ = os.MkdirAll(dockerConfigHome, 0755)
	_ = os.MkdirAll(gnupgHome, 0700)

	// Create a minimal Docker config to avoid GPG credential helper
	dockerConfigFile := filepath.Join(dockerConfigHome, "config.json")
	dockerConfig := `{
	"auths": {},
	"credHelpers": {},
	"credsStore": ""
}`
	_ = os.WriteFile(dockerConfigFile, []byte(dockerConfig), 0644)

	// Clear all Helm-related environment variables
	for _, envVar := range helmEnvVars {
		_ = os.Unsetenv(envVar)
	}

	// Set up clean test environment
	_ = os.Setenv("HELM_CACHE_HOME", helmCacheHome)
	_ = os.Setenv("HELM_CONFIG_HOME", helmConfigHome)
	_ = os.Setenv("HELM_DATA_HOME", helmDataHome)
	_ = os.Setenv("DOCKER_CONFIG", dockerConfigHome)
	_ = os.Setenv("GNUPGHOME", gnupgHome)
	// Set minimal PATH to avoid credential helper binaries
	_ = os.Setenv("PATH", "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin")
	// Note: We don't set HELM_DRIVER, leaving it to use the default driver

	// Return cleanup function
	cleanup := func() {
		// Clean up temporary directory
		_ = os.RemoveAll(tempDir)

		// Restore original environment
		for _, envVar := range helmEnvVars {
			_ = os.Unsetenv(envVar)
			if val, exists := originalEnv[envVar]; exists {
				_ = os.Setenv(envVar, val)
			}
		}
	}

	return cleanup, nil
}
