package main

/*
#include <stdlib.h>
typedef struct {
	char* err;
} HelmResult;

struct CreateOptions {
	char* name;
	char* dir;
};
*/
import "C"
import (
	"fmt"
	"github.com/manusa/helm-java/native/internal/helm"
	"unsafe"
)

//export Create
func Create(options *C.struct_CreateOptions) C.HelmResult {
	var err string
	defer func() {
		if r := recover(); r != nil {
			err = fmt.Sprintf("%v", r)
		}
	}()
	if _, ex := helm.Create(&helm.CreateOptions{
		Name: C.GoString(options.name),
		Dir:  C.GoString(options.dir),
	}); ex != nil {
		err = ex.Error()
	}
	if err == "" {
		return C.HelmResult{}
	}
	cstr := C.CString(err)
	defer C.free(unsafe.Pointer(cstr))
	return C.HelmResult{err: cstr}
}

func main() {
	// NO OP
	////Test
	//ret := Create(&C.struct_CreateOptions{
	//	name: C.CString("test"),
	//	dir:  C.CString("/invalid"),
	//})
	//fmt.Println(ret)
}
