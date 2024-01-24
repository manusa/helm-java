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

struct LintOptions {
	char* path;
	int   strict;
	int   quiet;
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

struct RepoServerOptions {
	char* glob;
	char* username;
	char* password;
};

struct ShowOptions {
	char* path;
	char* outputFormat;
};
*/
import "C"
import (
	"fmt"
	"github.com/manusa/helm-java/native/internal/helm"
	"io"
	"os"
	"strings"
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
		srv, err := helm.RepoOciServerStart(&helm.RepoServerOptions{})
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

//export Show
func Show(options *C.struct_ShowOptions) C.Result {
	return runCommand(func() (string, error) {
		return helm.Show(&helm.ShowOptions{
			Path:         C.GoString(options.path),
			OutputFormat: C.GoString(options.outputFormat),
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
	//Test
	test := RepoOciServerStart(&C.struct_RepoServerOptions{})
	Free(test)
	RepoServerStopAll()
	Free(C.Result{})
	create := Create(&C.struct_CreateOptions{
		name: C.CString("test"),
		dir:  C.CString("/tmp"),
	})
	fmt.Println(create)
	Free(create)
	lint := Lint(&C.struct_LintOptions{
		path:   C.CString("/tmp/test"),
		strict: 1,
		quiet:  1,
	})
	fmt.Println(lint)
	Free(lint)
	pkg := Package(&C.struct_PackageOptions{
		path:        C.CString("/tmp/test"),
		destination: C.CString("/tmp/test"),
	})
	fmt.Println(pkg)
	Free(pkg)
	show := Show(&C.struct_ShowOptions{
		path:         C.CString("/tmp/test"),
		outputFormat: C.CString("all"),
	})
	fmt.Println(show)
	Free(show)
	version := Version()
	fmt.Println(version)
}
