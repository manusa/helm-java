CGO_ENABLED=1
LD_FLAGS=-s -w
COMMON_BUILD_ARGS=-ldflags "$(LD_FLAGS)" -buildmode=c-shared
MAVEN_OPTIONS=
LICENSE_FILE=license-header.txt
# Detect OS to be able to run build-native target and provide a name
OS_NAME=linux
ARCH=amd64
EXTENSION=so
ifeq ($(OS), Windows_NT)
	OS_NAME := windows-4.0
	EXTENSION := dll
else
	UNAME_MACHINE := $(shell uname -m)
	ifneq ($(UNAME_MACHINE), x86_64)
		ARCH := arm64
	endif
	UNAME_KERNEL := $(shell uname -s)
	ifeq ($(UNAME_KERNEL), Darwin)
		OS_NAME := darwin-10.12
		EXTENSION := dylib
	endif
endif
NATIVE_NAME := $(OS_NAME)-$(ARCH).$(EXTENSION)

.PHONY: clean
clean:
	cd native && go clean ./...
	mvn clean
	rm -f native/out/*.h native/out/*.so native/out/*.dylib native/out/*.dll

.PHONY: test-go
test-go:
	cd native && go clean -testcache && go test ./...

.PHONY: build-native
build-native:
	cd native && go build $(COMMON_BUILD_ARGS) -o ./out/helm-$(NATIVE_NAME)

.PHONY: build-native-cross-platform
build-native-cross-platform:
	go install src.techknowlogick.com/xgo@latest
	xgo -image ghcr.io/techknowlogick/xgo:go-1.25.7 $(COMMON_BUILD_ARGS) -out native/out/helm --targets */arm64,*/amd64 ./native

.PHONY: build-native-wasi
build-native-wasi:
	#cd native && GOOS=wasip1 GOARCH=wasm go build -o ./out/helm.wasm ./wasm/main.go
	# Andrea recommends using TinyGo
	# Doesn't work, need to find the right combination of ENV variables
	#cd native && GOOS=wasip1 GOARCH=wasm tinygo build -o ./out/helm.wasm
	# Working Version:
	cd native && tinygo build -target=wasi -o ./out/helm.wasm ./wasm/main.go

.PHONY: build-java
build-java:
	mvn $(MAVEN_OPTIONS) clean verify

.PHONY: build-current-platform
build-current-platform: MAVEN_OPTIONS=-Denforcer.skipRules=requireFilesExist
build-current-platform: build-native build-java

.PHONY: build-all
build-all: build-native-cross-platform build-java

.PHONY: test
test: test-go

.PHONY: release
release:
	@if [ -z "$(V)" ]; then echo "V is not set"; exit 1; fi
	@if [ -z "$(VS)" ]; then echo "VS is not set"; exit 1; fi
	@mvn versions:set -DnewVersion=$(V) -DgenerateBackupPoms=false
	@git add .
	@git commit -m "[RELEASE] Updated project version to v$(V)"
	@git tag v$(V)
	@git push origin v$(V)
	@mvn versions:set -DnewVersion=$(VS)-SNAPSHOT -DgenerateBackupPoms=false
	@git add .
	@git commit -m "[RELEASE] v$(V) released, prepare for next development iteration"
	@git push origin main

.PHONY: license
license:
	@license_len=$$(cat $(LICENSE_FILE) | wc -l) &&											\
	 files=$$(git ls-files | grep -E "\.go|\.java") &&											\
	 for file in $$files; do																	\
	   echo "Applying license to $$file";														\
	   head -n $$license_len $$file | cmp -s $(LICENSE_FILE) - ||								\
	     ( ( cat $(LICENSE_FILE); echo; cat $$file ) > $$file.temp; mv $$file.temp $$file )		\
	 done

.PHONY: update-go-deps
update-go-deps:
	@echo ">> updating Go dependencies"
	@cd native && for m in $$(go list -mod=readonly -m -f '{{ if and (not .Indirect) (not .Main)}}{{.Path}}{{end}}' all); do \
		go get $$m; \
	done
	cd native && go mod tidy
ifneq (,$(wildcard native/vendor))
	cd native && go mod vendor
endif
