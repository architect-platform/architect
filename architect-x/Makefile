BINARY=architect
VERSION?=dev
LDFLAGS=-ldflags "-X github.com/architect-platform/architect/internal/cli.Version=$(VERSION)"

.PHONY: build test clean install

build:
	go build $(LDFLAGS) -o bin/$(BINARY) ./cmd/architect

test:
	go test ./...

clean:
	rm -rf bin/

install: build
	cp bin/$(BINARY) $(GOPATH)/bin/$(BINARY)

lint:
	golangci-lint run ./...

fmt:
	go fmt ./...

vet:
	go vet ./...
