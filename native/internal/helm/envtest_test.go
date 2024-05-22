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
	"fmt"
	"github.com/spf13/afero"
	"k8s.io/client-go/tools/clientcmd"
	clientcmdapi "k8s.io/client-go/tools/clientcmd/api"
	"os"
	"path"
	"path/filepath"
	"runtime"
	"sigs.k8s.io/controller-runtime/pkg/envtest"
	"sigs.k8s.io/controller-runtime/tools/setup-envtest/env"
	"sigs.k8s.io/controller-runtime/tools/setup-envtest/remote"
	"sigs.k8s.io/controller-runtime/tools/setup-envtest/store"
	"sigs.k8s.io/controller-runtime/tools/setup-envtest/versions"
	"sigs.k8s.io/controller-runtime/tools/setup-envtest/workflows"
	"strings"
	"testing"
	"time"
)

func setupEnvTest() (func(), *os.File) {
	// EnvTest download and start
	envTestDir, err := store.DefaultStoreDir()
	if err != nil {
		panic(err)
	}
	envTest := &env.Env{
		FS:  afero.Afero{Fs: afero.NewOsFs()},
		Out: os.Stdout,
		Client: &remote.Client{
			Bucket: "kubebuilder-tools",
			Server: "storage.googleapis.com",
		},
		Platform: versions.PlatformItem{
			Platform: versions.Platform{
				OS:   runtime.GOOS,
				Arch: runtime.GOARCH,
			},
		},
		Version: versions.AnyVersion,
		Store:   store.NewAt(envTestDir),
	}
	envTest.CheckCoherence()
	workflows.Use{}.Do(envTest)
	versionDir := envTest.Platform.Platform.BaseName(*envTest.Version.AsConcrete())
	envTestEnvironment := &envtest.Environment{
		BinaryAssetsDirectory: filepath.Join(envTestDir, "k8s", versionDir),
	}
	envTestEnvironmentConfig, err := envTestEnvironment.Start()
	if err != nil {
		panic(fmt.Sprintf("Error starting test environment: %s", err))
	}

	// Kube Config
	kubeConfigFile, _ := os.CreateTemp("", "kubeconfig")
	clusters := make(map[string]*clientcmdapi.Cluster)
	clusters["default-cluster"] = &clientcmdapi.Cluster{
		Server:                   envTestEnvironmentConfig.Host,
		CertificateAuthorityData: envTestEnvironmentConfig.CAData,
	}
	contexts := make(map[string]*clientcmdapi.Context)
	contexts["default-context"] = &clientcmdapi.Context{
		Cluster:  "default-cluster",
		AuthInfo: "default-user",
	}

	authinfos := make(map[string]*clientcmdapi.AuthInfo)
	authinfos["default-user"] = &clientcmdapi.AuthInfo{
		ClientCertificateData: envTestEnvironmentConfig.CertData,
		ClientKeyData:         envTestEnvironmentConfig.KeyData,
	}
	clientConfig := clientcmdapi.Config{
		Kind:           "Config",
		APIVersion:     "v1",
		Clusters:       clusters,
		Contexts:       contexts,
		CurrentContext: "default-context",
		AuthInfos:      authinfos,
	}
	_ = clientcmd.WriteToFile(clientConfig, kubeConfigFile.Name())
	return func() {
		_ = envTestEnvironment.Stop()
		_ = os.Remove(kubeConfigFile.Name())
	}, kubeConfigFile
}

func TestInstallNonExistentNamespace(t *testing.T) {
	cleanUp, kubeConfigFile := setupEnvTest()
	defer cleanUp()
	create, _ := Create(&CreateOptions{
		Name: "test",
		Dir:  t.TempDir(),
	})
	_, err := Install(&InstallOptions{
		KubeConfig: kubeConfigFile.Name(),
		Chart:      create,
		Name:       "test",
		Namespace:  "non-existent",
	})
	if err == nil {
		t.Error("Expected install to fail")
		return
	}
	if !strings.Contains(err.Error(), "failed to create: namespaces \"non-existent\" not found") {
		t.Errorf("Expected namespace failure, got %s", err)
		return
	}
}

func TestInstall(t *testing.T) {
	cleanUp, kubeConfigFile := setupEnvTest()
	defer cleanUp()
	create, _ := Create(&CreateOptions{
		Name: "test",
		Dir:  t.TempDir(),
	})
	out, err := Install(&InstallOptions{
		KubeConfig: kubeConfigFile.Name(),
		Chart:      create,
		Name:       "test",
	})
	if err != nil {
		t.Errorf("Expected install to succeed, got %s", err)
		return
	}
	if !strings.Contains(out, "NAME: test") || !strings.Contains(out, "STATUS: deployed") {
		t.Errorf("Expected install to succeed, got %s", out)
		return
	}
}

func TestInstallCreateNamespace(t *testing.T) {
	cleanUp, kubeConfigFile := setupEnvTest()
	defer cleanUp()
	create, _ := Create(&CreateOptions{
		Name: "test",
		Dir:  t.TempDir(),
	})
	out, err := Install(&InstallOptions{
		KubeConfig:      kubeConfigFile.Name(),
		Chart:           create,
		Name:            "test",
		Namespace:       "to-be-created",
		CreateNamespace: true,
	})
	if err != nil {
		t.Errorf("Expected install to succeed, got %s", err)
		return
	}
	if !strings.Contains(out, "NAME: test") || !strings.Contains(out, "NAMESPACE: to-be-created") {
		t.Errorf("Expected install to succeed, got %s", out)
		return
	}
}

func TestInstallDebug(t *testing.T) {
	cleanUp, kubeConfigFile := setupEnvTest()
	defer cleanUp()
	create, _ := Create(&CreateOptions{
		Name: "test",
		Dir:  t.TempDir(),
	})
	out, err := Install(&InstallOptions{
		KubeConfig: kubeConfigFile.Name(),
		Chart:      create,
		Name:       "test",
		Debug:      true,
	})
	if err != nil {
		t.Errorf("Expected install to succeed, got %s", err)
		return
	}
	if !strings.Contains(out, "---\ncreating 3 resource(s)") {
		t.Errorf("Expected install to succeed, got %s", out)
		return
	}
}

func TestInstallWaitFails(t *testing.T) {
	cleanUp, kubeConfigFile := setupEnvTest()
	defer cleanUp()
	create, _ := Create(&CreateOptions{
		Name: "test-wait",
		Dir:  t.TempDir(),
	})
	out, err := Install(&InstallOptions{
		KubeConfig: kubeConfigFile.Name(),
		Chart:      create,
		Name:       "test-wait",
		Wait:       true,
		Timeout:    1 * time.Millisecond,
	})
	if err == nil {
		t.Errorf("Expected install to fail, got %s", out)
		return
	}
	if !strings.Contains(err.Error(), "context deadline exceeded") {
		t.Errorf("Expected install to fail, got %s", err.Error())
		return
	}
}

func TestList(t *testing.T) {
	cleanUp, kubeConfigFile := setupEnvTest()
	defer cleanUp()
	create, _ := Create(&CreateOptions{
		Name: "test-list",
		Dir:  t.TempDir(),
	})
	_, _ = Install(&InstallOptions{
		KubeConfig: kubeConfigFile.Name(),
		Chart:      create,
		Name:       "test-list",
	})
	out, err := List(&ListOptions{
		KubeConfig: kubeConfigFile.Name(),
	})
	if err != nil {
		t.Errorf("Expected list to succeed, got %s", err)
		return
	}
	if !strings.Contains(out, "name=test-list") {
		t.Errorf("Expected list to include test-list, got %s", out)
		return
	}
	if !strings.Contains(out, "chart=test-list-0.1.0") {
		t.Errorf("Expected list to include test-list, got %s", out)
		return
	}
}

func TestTest(t *testing.T) {
	cleanUp, kubeConfigFile := setupEnvTest()
	defer cleanUp()
	create, _ := Create(&CreateOptions{
		Name: "test-test",
		Dir:  t.TempDir(),
	})
	// Delete default test
	_ = os.Remove(path.Join(create, "templates", "tests", "test-connection.yaml"))
	// Create a simple test compatible with envtest
	testYaml, _ := os.OpenFile(path.Join(create, "templates", "tests", "simple.test.yaml"), os.O_CREATE|os.O_WRONLY, 0666)
	_, _ = testYaml.WriteString("" +
		"apiVersion: v1\n" +
		"kind: ConfigMap\n" +
		"metadata:\n" +
		"  name: test-connection\n" +
		"  annotations:\n" +
		"    helm.sh/hook: test\n" +
		"data:\n" +
		"  test: \"test\"\n")
	_ = testYaml.Close()
	_, _ = Install(&InstallOptions{
		KubeConfig: kubeConfigFile.Name(),
		Chart:      create,
		Name:       "test-test",
	})
	out, err := Test(&TestOptions{
		KubeConfig:  kubeConfigFile.Name(),
		ReleaseName: "test-test",
	})
	if err != nil {
		t.Errorf("Expected test to succeed, got %s", err)
		return
	}
	if !strings.Contains(out, "NAME: test-test") {
		t.Errorf("Expected install to succeed, got %s", out)
		return
	}
}

func TestUninstall(t *testing.T) {
	cleanUp, kubeConfigFile := setupEnvTest()
	defer cleanUp()
	create, _ := Create(&CreateOptions{
		Name: "test-uninstall",
		Dir:  t.TempDir(),
	})
	_, _ = Install(&InstallOptions{
		KubeConfig: kubeConfigFile.Name(),
		Chart:      create,
		Name:       "test-uninstall",
	})
	out, err := Uninstall(&UninstallOptions{
		KubeConfig:  kubeConfigFile.Name(),
		ReleaseName: "test-uninstall",
	})
	if err != nil {
		t.Errorf("Expected uninstall to succeed, got %s", err)
		return
	}
	if !strings.Contains(out, "release \"test-uninstall\" uninstalled\n") {
		t.Errorf("Expected uninstall to succeed, got %s", out)
		return
	}
}

func TestUninstallDebug(t *testing.T) {
	cleanUp, kubeConfigFile := setupEnvTest()
	defer cleanUp()
	create, _ := Create(&CreateOptions{
		Name: "test-uninstall-debug",
		Dir:  t.TempDir(),
	})
	_, _ = Install(&InstallOptions{
		KubeConfig: kubeConfigFile.Name(),
		Chart:      create,
		Name:       "test-uninstall-debug",
	})
	out, err := Uninstall(&UninstallOptions{
		KubeConfig:  kubeConfigFile.Name(),
		ReleaseName: "test-uninstall-debug",
		Debug:       true,
	})
	if err != nil {
		t.Errorf("Expected uninstall to succeed, got %s", err)
		return
	}
	if !strings.Contains(out, "release \"test-uninstall-debug\" uninstalled\n") &&
		!strings.Contains(out, "uninstall: Deleting test-uninstall-debug\n") &&
		!strings.Contains(out, "Starting delete for \"test-uninstall-debug\" Service\n") &&
		!strings.Contains(out, "Starting delete for \"test-uninstall-debug\" Deployment\n") &&
		!strings.Contains(out, "Starting delete for \"test-uninstall-debug\" ServiceAccount\n") &&
		!strings.Contains(out, "purge requested for test-uninstall-debug") {
		t.Errorf("Expected uninstall to log debug messages, got %s", out)
		return
	}
}

func TestUpgrade(t *testing.T) {
	cleanUp, kubeConfigFile := setupEnvTest()
	defer cleanUp()
	chart, _ := Create(&CreateOptions{
		Name: "test-upgrade",
		Dir:  t.TempDir(),
	})
	_, _ = Install(&InstallOptions{
		KubeConfig: kubeConfigFile.Name(),
		Chart:      chart,
		Name:       "test-upgrade",
	})
	out, err := Upgrade(&UpgradeOptions{
		KubeConfig: kubeConfigFile.Name(),
		Chart:      chart,
		Name:       "test-upgrade",
	})
	if err != nil {
		t.Errorf("Expected upgrade to succeed, got %s", err)
		return
	}
	if !strings.Contains(out, "NAME: test-upgrade") ||
		!strings.Contains(out, "REVISION: 2") ||
		!strings.Contains(out, "STATUS: deployed") {
		t.Errorf("Expected upgrade to succeed, got %s", out)
		return
	}
}

func TestUpgradeInstall(t *testing.T) {
	cleanUp, kubeConfigFile := setupEnvTest()
	defer cleanUp()
	chart, _ := Create(&CreateOptions{
		Name: "test-upgrade",
		Dir:  t.TempDir(),
	})
	out, err := Upgrade(&UpgradeOptions{
		KubeConfig: kubeConfigFile.Name(),
		Install:    true,
		Chart:      chart,
		Name:       "test-upgrade-install",
	})
	if err != nil {
		t.Errorf("Expected upgrade to succeed, got %s", err)
		return
	}
	if !strings.Contains(out, "NAME: test-upgrade-install") ||
		!strings.Contains(out, "REVISION: 1") ||
		!strings.Contains(out, "STATUS: deployed") {
		t.Errorf("Expected upgrade to succeed, got %s", out)
		return
	}
}
