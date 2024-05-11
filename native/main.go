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

package main

/*
#include <stdbool.h>
#include <stdlib.h>

typedef struct {
	char* out;
	char* err;
	char* stdOut;
	char* stdErr;
} Result;

struct CreateOptions {
	char* name;
	char* dir;
};

struct DependencyOptions  {
	char* path;
	char* keyring;
	int   skipRefresh;
	int   verify;
	int   debug;
};

struct InstallOptions {
	char* name;
	int   generateName;
	char* nameTemplate;
	char* chart;
	char* namespace;
	int   createNamespace;
	char* description;
	int   devel;
	int   dependencyUpdate;
	int   dryRun;
	char* dryRunOption;
	int   wait;
	char* values;
	char* kubeConfig;
	char* certFile;
	char* keyFile;
	char* caFile;
	int   insecureSkipTlsVerify;
	int   plainHttp;
	char* keyring;
	int   debug;
	int   clientOnly;
};

struct LintOptions {
	char* path;
	int   strict;
	int   quiet;
};

struct ListOptions {
	int   all;
	int   allNamespaces;
	int   deployed;
	int   failed;
	int   pending;
	int   superseded;
	int   uninstalled;
	int   uninstalling;
	char* namespace;
	char* kubeConfig;
};

struct PackageOptions {
	char* path;
	char* destination;
	int   sign;
	char* key;
	char* keyring;
	char* passhraseFile;
};

struct PushOptions {
	char* chart;
	char* remote;
	char* certFile;
	char* keyFile;
	char* caFile;
	int   insecureSkipTlsVerify;
	int   plainHttp;
	int   debug;
};

struct RegistryOptions {
	char* hostname;
	char* username;
	char* password;
	char* certFile;
	char* keyFile;
	char* caFile;
	int   insecure;
	int   plainHttp;
	int   debug;
};

struct RepoOptions {
	char* repositoryConfig;
	char* name;
	char* names;
	char* url;
	char* username;
	char* password;
	char* certFile;
	char* keyFile;
	char* caFile;
	int   insecureSkipTlsVerify;
};

struct RepoServerOptions {
	char* glob;
	char* username;
	char* password;
};

struct SearchOptions {
	char* repositoryConfig;
	char* keyword;
	int   regexp;
	int   devel;
	char* version;
};

struct ShowOptions {
	char* path;
	char* outputFormat;
	char* version;
	char* certFile;
	char* keyFile;
	char* caFile;
	int   insecure;
	int   plainHttp;
	int   debug;
};

struct TestOptions {
	char* releaseName;
	int   timeout;
	char* namespace;
	char* kubeConfig;
	int   debug;
};

struct UninstallOptions {
	char* releaseName;
	int   dryRun;
	int   noHooks;
	int   ignoreNotFound;
	int   keepHistory;
	char* cascade;
	char* namespace;
	char* kubeConfig;
	int   debug;
};

struct UpgradeOptions {
	char* name;
	char* chart;
	char* namespace;
	int   install;
	int   force;
	int   resetValues;
	int   reuseValues;
	int   resetThenReuseValues;
	int   atomic;
	int   cleanupOnFail;
	int   createNamespace;
	char* description;
	int   devel;
	int   dependencyUpdate;
	int   dryRun;
	char* dryRunOption;
	int   wait;
	char* values;
	char* kubeConfig;
	char* certFile;
	char* keyFile;
	char* caFile;
	int   insecureSkipTlsVerify;
	int   plainHttp;
	char* keyring;
	int   debug;
	int   clientOnly;
};
*/
import "C"
import (
	"fmt"
	"github.com/manusa/helm-java/native/internal/helm"
	"io"
	"os"
	"strings"
	"time"
	"unsafe"
)

// Run the given function and return the result as a C struct.
// The returned C struct contains the text representation of the result or the function execution error.
// This function replaces the stdout and stderr streams to be able to capture them and return them in the C.Result struct.
// The original stdout and stderr streams are restored after the function execution.
func runCommand(f func() (string, error)) C.Result {
	oldOut := os.Stdout
	oldErr := os.Stderr
	defer func() {
		os.Stdout = oldOut
		os.Stderr = oldErr
	}()
	outR, outW, _ := os.Pipe()
	errR, errW, _ := os.Pipe()
	os.Stdout = outW
	os.Stderr = errW
	var err string
	defer func() {
		if r := recover(); r != nil {
			err = fmt.Sprintf("%v", r)
		}
	}()
	out, ex := f()
	if ex != nil {
		err = ex.Error()
	}
	return C.Result{
		out:    toCString(out),
		err:    toCString(err),
		stdOut: toCString(toString(outR, outW)),
		stdErr: toCString(toString(errR, errW)),
	}
}

//export Create
func Create(options *C.struct_CreateOptions) C.Result {
	return runCommand(func() (string, error) {
		_, ex := helm.Create(&helm.CreateOptions{
			Name: C.GoString(options.name),
			Dir:  C.GoString(options.dir),
		})
		return "", ex
	})
}

//export DependencyBuild
func DependencyBuild(options *C.struct_DependencyOptions) C.Result {
	return runCommand(func() (string, error) {
		return helm.DependencyBuild(&helm.DependencyOptions{
			Path:        C.GoString(options.path),
			Keyring:     C.GoString(options.keyring),
			SkipRefresh: options.skipRefresh == 1,
			Verify:      options.verify == 1,
			Debug:       options.debug == 1,
		})
	})
}

//export DependencyList
func DependencyList(options *C.struct_DependencyOptions) C.Result {
	return runCommand(func() (string, error) {
		return helm.DependencyList(&helm.DependencyOptions{
			Path: C.GoString(options.path),
		})
	})
}

//export DependencyUpdate
func DependencyUpdate(options *C.struct_DependencyOptions) C.Result {
	return runCommand(func() (string, error) {
		return helm.DependencyUpdate(&helm.DependencyOptions{
			Path:        C.GoString(options.path),
			Keyring:     C.GoString(options.keyring),
			SkipRefresh: options.skipRefresh == 1,
			Verify:      options.verify == 1,
			Debug:       options.debug == 1,
		})
	})
}

//export Install
func Install(options *C.struct_InstallOptions) C.Result {
	return runCommand(func() (string, error) {
		return helm.Install(&helm.InstallOptions{
			Name:                  C.GoString(options.name),
			GenerateName:          options.generateName == 1,
			NameTemplate:          C.GoString(options.nameTemplate),
			Chart:                 C.GoString(options.chart),
			Namespace:             C.GoString(options.namespace),
			CreateNamespace:       options.createNamespace == 1,
			Description:           C.GoString(options.description),
			Devel:                 options.devel == 1,
			DependencyUpdate:      options.dependencyUpdate == 1,
			DryRun:                options.dryRun == 1,
			DryRunOption:          C.GoString(options.dryRunOption),
			Wait:                  options.wait == 1,
			Values:                C.GoString(options.values),
			KubeConfig:            C.GoString(options.kubeConfig),
			CertFile:              C.GoString(options.certFile),
			KeyFile:               C.GoString(options.keyFile),
			CaFile:                C.GoString(options.caFile),
			InsecureSkipTLSverify: options.insecureSkipTlsVerify == 1,
			PlainHttp:             options.plainHttp == 1,
			Keyring:               C.GoString(options.keyring),
			Debug:                 options.debug == 1,
			ClientOnly:            options.clientOnly == 1,
		})
	})
}

//export Lint
func Lint(options *C.struct_LintOptions) C.Result {
	return runCommand(func() (string, error) {
		result, failed := helm.Lint(&helm.LintOptions{
			Path:   C.GoString(options.path),
			Strict: options.strict == 1,
			Quiet:  options.quiet == 1,
		})
		result = append(result, fmt.Sprintf("Failed: %v", failed))
		report := strings.Join(result, "\n")
		return report, nil
	})
}

//export List
func List(options *C.struct_ListOptions) C.Result {
	return runCommand(func() (string, error) {
		return helm.List(&helm.ListOptions{
			All:           options.all == 1,
			AllNamespaces: options.allNamespaces == 1,
			Deployed:      options.deployed == 1,
			Failed:        options.failed == 1,
			Pending:       options.pending == 1,
			Superseded:    options.superseded == 1,
			Uninstalled:   options.uninstalled == 1,
			Uninstalling:  options.uninstalling == 1,
			Namespace:     C.GoString(options.namespace),
			KubeConfig:    C.GoString(options.kubeConfig),
		})
	})
}

//export Package
func Package(options *C.struct_PackageOptions) C.Result {
	return runCommand(func() (string, error) {
		err := helm.Package(&helm.PackageOptions{
			Path:           C.GoString(options.path),
			Destination:    C.GoString(options.destination),
			Sign:           options.sign == 1,
			Key:            C.GoString(options.key),
			Keyring:        C.GoString(options.keyring),
			PassphraseFile: C.GoString(options.passhraseFile),
		})
		return "", err
	})
}

//export Push
func Push(options *C.struct_PushOptions) C.Result {
	return runCommand(func() (string, error) {
		return helm.Push(&helm.PushOptions{
			Chart:                 C.GoString(options.chart),
			Remote:                C.GoString(options.remote),
			CertFile:              C.GoString(options.certFile),
			KeyFile:               C.GoString(options.keyFile),
			CaFile:                C.GoString(options.caFile),
			InsecureSkipTlsVerify: options.insecureSkipTlsVerify == 1,
			PlainHttp:             options.plainHttp == 1,
			Debug:                 options.debug == 1,
		})
	})
}

//export RegistryLogin
func RegistryLogin(options *C.struct_RegistryOptions) C.Result {
	return runCommand(func() (string, error) {
		return helm.RegistryLogin(&helm.RegistryOptions{
			Hostname:  C.GoString(options.hostname),
			Username:  C.GoString(options.username),
			Password:  C.GoString(options.password),
			CertFile:  C.GoString(options.certFile),
			KeyFile:   C.GoString(options.keyFile),
			CaFile:    C.GoString(options.caFile),
			Insecure:  options.insecure == 1,
			PlainHttp: options.plainHttp == 1,
			Debug:     options.debug == 1,
		})
	})
}

//export RegistryLogout
func RegistryLogout(options *C.struct_RegistryOptions) C.Result {
	return runCommand(func() (string, error) {
		return helm.RegistryLogout(&helm.RegistryOptions{
			Hostname:  C.GoString(options.hostname),
			CertFile:  C.GoString(options.certFile),
			KeyFile:   C.GoString(options.keyFile),
			CaFile:    C.GoString(options.caFile),
			Insecure:  options.insecure == 1,
			PlainHttp: options.plainHttp == 1,
			Debug:     options.debug == 1,
		})
	})
}

//export RepoAdd
func RepoAdd(options *C.struct_RepoOptions) C.Result {
	return runCommand(func() (string, error) {
		return "", helm.RepoAdd(&helm.RepoOptions{
			RepositoryConfig:      C.GoString(options.repositoryConfig),
			Name:                  C.GoString(options.name),
			Url:                   C.GoString(options.url),
			Username:              C.GoString(options.username),
			Password:              C.GoString(options.password),
			CertFile:              C.GoString(options.certFile),
			KeyFile:               C.GoString(options.keyFile),
			CaFile:                C.GoString(options.caFile),
			InsecureSkipTlsVerify: options.insecureSkipTlsVerify == 1,
		})
	})
}

//export RepoList
func RepoList(options *C.struct_RepoOptions) C.Result {
	return runCommand(func() (string, error) {
		return helm.RepoList(&helm.RepoOptions{
			RepositoryConfig: C.GoString(options.repositoryConfig),
		})
	})
}

//export RepoRemove
func RepoRemove(options *C.struct_RepoOptions) C.Result {
	return runCommand(func() (string, error) {
		return "", helm.RepoRemove(&helm.RepoOptions{
			RepositoryConfig: C.GoString(options.repositoryConfig),
			Names:            C.GoString(options.names),
		})
	})
}

//export RepoServerStart
func RepoServerStart(options *C.struct_RepoServerOptions) C.Result {
	return runCommand(func() (string, error) {
		srv, err := helm.RepoServerStart(&helm.RepoServerOptions{
			Glob:     C.GoString(options.glob),
			Username: C.GoString(options.username),
			Password: C.GoString(options.password),
		})
		if srv != nil {
			return srv.URL(), err
		}
		return "", err
	})
}

//export RepoOciServerStart
func RepoOciServerStart(options *C.struct_RepoServerOptions) C.Result {
	return runCommand(func() (string, error) {
		srv, err := helm.RepoOciServerStart(&helm.RepoServerOptions{
			Glob:     C.GoString(options.glob),
			Username: C.GoString(options.username),
			Password: C.GoString(options.password),
		})
		if srv != nil {
			return srv.RegistryURL, err
		}
		return "", err
	})
}

//export RepoServerStop
func RepoServerStop(url *C.char) C.Result {
	return runCommand(func() (string, error) {
		helm.RepoServerStop(C.GoString(url))
		return "", nil
	})
}

//export RepoServerStopAll
func RepoServerStopAll() C.Result {
	return runCommand(func() (string, error) {
		helm.RepoServerStopAll()
		return "", nil
	})
}

//export SearchRepo
func SearchRepo(options *C.struct_SearchOptions) C.Result {
	return runCommand(func() (string, error) {
		return helm.SearchRepo(&helm.SearchOptions{
			RepositoryConfig: C.GoString(options.repositoryConfig),
			Keyword:          C.GoString(options.keyword),
			Regexp:           options.regexp == 1,
			Devel:            options.devel == 1,
			Version:          C.GoString(options.version),
		})
	})
}

//export Show
func Show(options *C.struct_ShowOptions) C.Result {
	return runCommand(func() (string, error) {
		return helm.Show(&helm.ShowOptions{
			Path:         C.GoString(options.path),
			OutputFormat: C.GoString(options.outputFormat),
			Version:      C.GoString(options.version),
			CertFile:     C.GoString(options.certFile),
			KeyFile:      C.GoString(options.keyFile),
			CaFile:       C.GoString(options.caFile),
			Insecure:     options.insecure == 1,
			PlainHttp:    options.plainHttp == 1,
			Debug:        options.debug == 1,
		})
	})
}

//export Test
func Test(options *C.struct_TestOptions) C.Result {
	var timeout time.Duration
	if options.timeout > 0 {
		timeout = time.Duration(int(options.timeout)) * time.Second
	} else {
		timeout = time.Duration(300) * time.Second
	}
	return runCommand(func() (string, error) {
		return helm.Test(&helm.TestOptions{
			ReleaseName: C.GoString(options.releaseName),
			Namespace:   C.GoString(options.namespace),
			KubeConfig:  C.GoString(options.kubeConfig),
			Timeout:     timeout,
			Debug:       options.debug == 1,
		})
	})
}

//export Uninstall
func Uninstall(options *C.struct_UninstallOptions) C.Result {
	return runCommand(func() (string, error) {
		return helm.Uninstall(&helm.UninstallOptions{
			ReleaseName:    C.GoString(options.releaseName),
			DryRun:         options.dryRun == 1,
			NoHooks:        options.noHooks == 1,
			IgnoreNotFound: options.ignoreNotFound == 1,
			KeepHistory:    options.keepHistory == 1,
			Cascade:        C.GoString(options.cascade),
			Namespace:      C.GoString(options.namespace),
			KubeConfig:     C.GoString(options.kubeConfig),
			Debug:          options.debug == 1,
		})
	})
}

//export Upgrade
func Upgrade(options *C.struct_UpgradeOptions) C.Result {
	return runCommand(func() (string, error) {
		return helm.Upgrade(&helm.UpgradeOptions{
			Name:                  C.GoString(options.name),
			Chart:                 C.GoString(options.chart),
			Namespace:             C.GoString(options.namespace),
			Install:               options.install == 1,
			Force:                 options.force == 1,
			ResetValues:           options.resetValues == 1,
			ReuseValues:           options.reuseValues == 1,
			ResetThenReuseValues:  options.resetThenReuseValues == 1,
			Atomic:                options.atomic == 1,
			CleanupOnFail:         options.cleanupOnFail == 1,
			CreateNamespace:       options.createNamespace == 1,
			Description:           C.GoString(options.description),
			Devel:                 options.devel == 1,
			DependencyUpdate:      options.dependencyUpdate == 1,
			DryRun:                options.dryRun == 1,
			DryRunOption:          C.GoString(options.dryRunOption),
			Wait:                  options.wait == 1,
			Values:                C.GoString(options.values),
			KubeConfig:            C.GoString(options.kubeConfig),
			CertFile:              C.GoString(options.certFile),
			KeyFile:               C.GoString(options.keyFile),
			CaFile:                C.GoString(options.caFile),
			InsecureSkipTLSverify: options.insecureSkipTlsVerify == 1,
			PlainHttp:             options.plainHttp == 1,
			Keyring:               C.GoString(options.keyring),
			Debug:                 options.debug == 1,
			ClientOnly:            options.clientOnly == 1,
		})
	})
}

//export Version
func Version() C.Result {
	return runCommand(func() (string, error) {
		return helm.Version()
	})
}

//export Free
func Free(result C.Result) {
	C.free(unsafe.Pointer(result.out))
	C.free(unsafe.Pointer(result.err))
	C.free(unsafe.Pointer(result.stdOut))
	C.free(unsafe.Pointer(result.stdErr))
}

func toString(readFile *os.File, writeFile *os.File) string {
	_ = writeFile.Close()
	if readBytes, err := io.ReadAll(readFile); err != nil {
		return ""
	} else {
		return string(readBytes)
	}
}

func toCString(str string) *C.char {
	if len(strings.TrimSpace(str)) == 0 {
		return nil
	}
	return C.CString(str)
}

func main() {
	// NO OP
}
