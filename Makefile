CGO_ENABLED=1
LD_FLAGS=-s -w
COMMON_BUILD_ARGS=-ldflags "$(LD_FLAGS)" -buildmode=c-shared
MAVEN_OPTIONS=
# Detect OS to be able to run build-native target and provide a name
NATIVE_NAME=linux-amd64.so
LICENSE_FILE=license-header.txt
ifeq ($(OS), Windows_NT)
	NATIVE_NAME := windows-4.0-amd64.dll
else
	UNAME := $(shell uname -s)
	ifeq ($(UNAME), Darwin)
	  NATIVE_NAME := darwin-10.12-amd64.dylib
	endif
endif

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
	xgo $(COMMON_BUILD_ARGS) -out native/out/helm --targets */arm64,*/amd64 ./native

.PHONY: build-java
build-java:
	mvn $(MAVEN_OPTIONS) clean verify

.PHONY: build-current-platform
build-current-platform: MAVEN_OPTIONS=-Denforcer.skipRules=requireFilesExist
build-current-platform: build-native build-java

.PHONY: build-all
build-all: build-native-cross-platform build-java

.PHONY: test
test:
	cd native && go test ./...

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
	@license_len=$$(wc -l $(LICENSE_FILE) | cut -f1 -d ' ') &&								\
	 files=$$(git ls-files | grep -E "\.go|\.java") &&											\
	 for file in $$files; do																	\
	   echo "Applying license to $$file";														\
	   head -n $$license_len $$file | diff -q $(LICENSE_FILE) - > /dev/null ||					\
	     ( ( cat $(LICENSE_FILE); echo; cat $$file ) > $$file.temp; mv $$file.temp $$file )		\
	 done
