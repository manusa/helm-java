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
	int  strict;
	int  quiet;
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
	//Free(C.Result{})
	//create := Create(&C.struct_CreateOptions{
	//	name: C.CString("test"),
	//	dir:  C.CString("/tmp"),
	//})
	//fmt.Println(create)
	//Free(create)
	lint := Lint(&C.struct_LintOptions{
		path:   C.CString("/tmp/test"),
		strict: 1,
		quiet:  1,
	})
	fmt.Println(lint)
	Free(lint)
	version := Version()
	fmt.Println(version)
}
