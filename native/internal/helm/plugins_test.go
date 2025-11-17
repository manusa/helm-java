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
	"strings"
	"testing"

	"k8s.io/client-go/rest"
)

const azureExecKubeConfig = `
apiVersion: v1
clusters:
- cluster:
    server: https://azure.example.com
  name: azure-cluster
contexts:
- context:
    cluster: azure-cluster
    user: azure-user
  name: azure-context
current-context: azure-context
kind: Config
users:
- name: azure-user
  user:
    exec:
      apiVersion: client.authentication.k8s.io/v1beta1
      command: kubelogin
      args:
      - get-token
      - --environment
      - AzurePublicCloud
      - --server-id
      - test-server-id
      - --client-id
      - test-client-id
      - --tenant-id
      - test-tenant-id
`

const gcpExecKubeConfig = `
apiVersion: v1
clusters:
- cluster:
    server: https://gcp.example.com
  name: gcp-cluster
contexts:
- context:
    cluster: gcp-cluster
    user: gcp-user
  name: gcp-context
current-context: gcp-context
kind: Config
users:
- name: gcp-user
  user:
    exec:
      apiVersion: client.authentication.k8s.io/v1beta1
      command: gke-gcloud-auth-plugin
      installHint: Install gke-gcloud-auth-plugin for use with kubectl by following https://cloud.google.com/kubernetes-engine/docs/how-to/cluster-access-for-kubectl#install_plugin
      provideClusterInfo: true
`

const oidcKubeConfig = `
apiVersion: v1
clusters:
- cluster:
    server: https://oidc.example.com
  name: oidc-cluster
contexts:
- context:
    cluster: oidc-cluster
    user: oidc-user
  name: oidc-context
current-context: oidc-context
kind: Config
users:
- name: oidc-user
  user:
    auth-provider:
      name: oidc
      config:
        client-id: test-client-id
        idp-issuer-url: https://issuer.example.com
        id-token: test-id-token
        refresh-token: test-refresh-token
`

const execKubeConfig = `
apiVersion: v1
clusters:
- cluster:
    server: https://exec.example.com
  name: exec-cluster
contexts:
- context:
    cluster: exec-cluster
    user: exec-user
  name: exec-context
current-context: exec-context
kind: Config
users:
- name: exec-user
  user:
    exec:
      apiVersion: client.authentication.k8s.io/v1beta1
      command: kubectl
      args:
      - get-token
      - --cluster-id=test-cluster
`

func TestAuthPlugins(t *testing.T) {
	t.Run("should support azure via exec plugin (kubelogin)", func(t *testing.T) {
		cfg, err := NewCfg(&CfgOptions{
			KubeConfigContents: azureExecKubeConfig,
		})
		if err != nil {
			t.Errorf("Expected NewCfg to succeed, got %s", err)
			return
		}
		restConfig, err := cfg.RESTClientGetter.ToRESTConfig()
		if err != nil {
			t.Errorf("Expected azure exec kubeconfig to parse successfully, got %s", err)
			return
		}
		if restConfig.ExecProvider == nil {
			t.Error("Expected ExecProvider to be configured for azure")
			return
		}
		if restConfig.ExecProvider.Command != "kubelogin" {
			t.Errorf("Expected ExecProvider command to be 'kubelogin', got %s", restConfig.ExecProvider.Command)
		}
	})
	t.Run("should support gcp via exec plugin (gke-gcloud-auth-plugin)", func(t *testing.T) {
		cfg, err := NewCfg(&CfgOptions{
			KubeConfigContents: gcpExecKubeConfig,
		})
		if err != nil {
			t.Errorf("Expected NewCfg to succeed, got %s", err)
			return
		}
		restConfig, err := cfg.RESTClientGetter.ToRESTConfig()
		if err != nil {
			t.Errorf("Expected gcp exec kubeconfig to parse successfully, got %s", err)
			return
		}
		if restConfig.ExecProvider == nil {
			t.Error("Expected ExecProvider to be configured for gcp")
			return
		}
		if restConfig.ExecProvider.Command != "gke-gcloud-auth-plugin" {
			t.Errorf("Expected ExecProvider command to be 'gke-gcloud-auth-plugin', got %s", restConfig.ExecProvider.Command)
		}
	})
	t.Run("should register oidc auth provider plugin", func(t *testing.T) {
		cfg, err := NewCfg(&CfgOptions{
			KubeConfigContents: oidcKubeConfig,
		})
		if err != nil {
			t.Errorf("Expected NewCfg to succeed, got %s", err)
			return
		}
		restConfig, err := cfg.RESTClientGetter.ToRESTConfig()
		if err != nil {
			t.Errorf("Expected oidc kubeconfig to parse successfully, got %s", err)
			return
		}
		if restConfig.AuthProvider == nil {
			t.Error("Expected AuthProvider to be configured for oidc")
			return
		}
		_, err = rest.TransportFor(restConfig)
		if err != nil {
			t.Errorf("Expected oidc auth provider plugin to be registered, got %s", err)
		}
	})
	t.Run("should support exec auth provider", func(t *testing.T) {
		cfg, err := NewCfg(&CfgOptions{
			KubeConfigContents: execKubeConfig,
		})
		if err != nil {
			t.Errorf("Expected NewCfg to succeed, got %s", err)
			return
		}
		restConfig, err := cfg.RESTClientGetter.ToRESTConfig()
		if err != nil {
			t.Errorf("Expected exec kubeconfig to parse successfully, got %s", err)
			return
		}
		if restConfig.ExecProvider == nil {
			t.Error("Expected ExecProvider to be configured for exec auth")
			return
		}
		_, err = rest.TransportFor(restConfig)
		if err != nil {
			t.Errorf("Expected exec auth provider to work, got %s", err)
		}
	})
	t.Run("should fail with unknown auth provider plugin", func(t *testing.T) {
		unknownAuthConfig := `
apiVersion: v1
clusters:
- cluster:
    server: https://unknown.example.com
  name: unknown-cluster
contexts:
- context:
    cluster: unknown-cluster
    user: unknown-user
  name: unknown-context
current-context: unknown-context
kind: Config
users:
- name: unknown-user
  user:
    auth-provider:
      name: unknown-provider-that-does-not-exist
      config:
        some-key: some-value
`
		cfg, err := NewCfg(&CfgOptions{
			KubeConfigContents: unknownAuthConfig,
		})
		if err != nil {
			t.Errorf("Expected NewCfg to succeed, got %s", err)
			return
		}
		restConfig, err := cfg.RESTClientGetter.ToRESTConfig()
		if err != nil {
			t.Errorf("Expected parsing to succeed, got %s", err)
			return
		}
		_, err = rest.TransportFor(restConfig)
		if err == nil {
			t.Error("Expected unknown auth provider to fail when creating transport")
			return
		}
		if !strings.Contains(err.Error(), "no Auth Provider found for name") {
			t.Errorf("Expected 'no Auth Provider found' error message, got %s", err)
		}
	})
}
