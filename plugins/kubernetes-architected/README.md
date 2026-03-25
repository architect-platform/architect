# kubernetes-architected

> **Status**: Experimental (incubating tier) — see [STATUS.md](STATUS.md)

## Overview

kubernetes-architected integrates Kubernetes into the Architect task lifecycle, providing tasks for deploying, scaling, and managing Kubernetes resources. It enables teams to manage cluster operations through standardized Architect commands.

## Tasks

| Task ID | Description |
|---|---|
| `k8s-apply` | Apply manifests to the target cluster |
| `k8s-rollout` | Monitor a rollout status |
| `k8s-status` | Print current deployment status |
| `k8s-port-forward` | Start a port-forward for local access |

## Configuration

Add to your `architect.yml`:

```yaml
plugins:
  - name: kubernetes-architected
    repo: architect-platform/kubernetes-architected

kubernetes:
  namespace: my-namespace
  context: my-cluster-context
  manifests: k8s/
  kubeconfig: ""
```

## Local Build and Test

```bash
cd plugins/kubernetes-architected/app
gradle build
gradle test
```

## Limitations

This plugin is at incubating tier. Tests and documentation are minimal. See [STATUS.md](STATUS.md) for graduation criteria.
