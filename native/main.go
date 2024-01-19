package main

/*
#include <stdlib.h>
typedef struct {
	char* err;
	char* stdOut;
	char* stdErr;
} Result;

struct CreateOptions {
	char* name;
	char* dir;
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

func runCommand(f func() error) C.Result {
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
	if ex := f(); ex != nil {
		err = ex.Error()
	}
	return C.Result{
		err:    toCString(err),
		stdOut: toCString(toString(outR, outW)),
		stdErr: toCString(toString(errR, errW)),
	}
}

//export Create
func Create(options *C.struct_CreateOptions) C.Result {
	return runCommand(func() error {
		_, ex := helm.Create(&helm.CreateOptions{
			Name: C.GoString(options.name),
			Dir:  C.GoString(options.dir),
		})
		return ex
	})
}

//export Free
func Free(result C.Result) {
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
	////Test
	//ret := Create(&C.struct_CreateOptions{
	//	name: C.CString("test"),
	//	dir:  C.CString("/tmp"),
	//})
	//Free(ret)
	//Free(C.Result{})
	//fmt.Println(ret)
}
