CGO_ENABLED=1
LD_FLAGS=-s -w
COMMON_BUILD_ARGS=-ldflags "$(LD_FLAGS)" -buildmode=c-shared

.PHONY: clean
clean:
	cd native && go clean ./...
	mvn clean
	rm -f lib/*.h lib/*.so lib/*.dylib lib/*.dll

.PHONY: build-native
build-native:
	GOOS=linux GOARCH=amd64 cd native && go build $(COMMON_BUILD_ARGS) -o ../lib/helm.so

.PHONY: build-native-cross-platform
build-native-cross-platform:
	go install src.techknowlogick.com/xgo@latest
	xgo $(COMMON_BUILD_ARGS) -out lib/helm --targets */arm64,*/amd64 ./native

.PHONY: build-java
build-java:
	mvn clean verify

.PHONY: build-all
build-all: build-native-cross-platform build-java

.PHONY: test
test:
	cd native && go test ./...
