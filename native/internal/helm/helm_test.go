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
	"testing"
)

const kubeConfigContentsForTests = `
apiVersion: v1
clusters:
- cluster:
    server: https://host.example.com
  name: development
contexts:
- context:
    cluster: development
    namespace: the-namespace
    user: developer
  name: kube-config-test-contents
current-context: kube-config-test-contents
kind: Config
preferences: {}
users:
- name: developer
  user:
    username: test-user
    password: test-password
`

func TestNewCfg_KubeConfigContents(t *testing.T) {
	cfg := NewCfg(&CfgOptions{
		KubeConfigContents: kubeConfigContentsForTests,
	})
	restConfig, err := cfg.RESTClientGetter.ToRESTConfig()
	if err != nil {
		t.Fatalf("Expected converting to succeed, got %s", err)
	}
	if restConfig.Host != "https://host.example.com" {
		t.Fatalf("Expected https://host.example.com, got %s", restConfig.Host)
	}
	if restConfig.Username != "test-user" {
		t.Fatalf("Expected test-user, got %s", restConfig.Username)
	}
	if restConfig.Password != "test-password" {
		t.Fatalf("Expected test-password, got %s", restConfig.Password)
	}
}
