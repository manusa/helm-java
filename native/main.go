package main

/*
#include <stdlib.h>
typedef struct {
	char* err;
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
	"unsafe"
)

//export Create
func Create(options *C.struct_CreateOptions) C.Result {
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
		return C.Result{}
	}
	cstr := C.CString(err)
	//defer C.free(unsafe.Pointer(cstr))
	return C.Result{err: cstr}
}

//export Free
func Free(result C.Result) {
	C.free(unsafe.Pointer(result.err))
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
