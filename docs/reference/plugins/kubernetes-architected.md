# kubernetes-architected

Kubernetes cluster management integration for Architect.

**Source**: `architect-platform/kubernetes-architected`

## Installation

```yaml
plugins:
  - name: kubernetes-architected
    type: github
    repo: architect-platform/kubernetes-architected
```

## Tasks

| Task ID | Phase | Description |
|---------|-------|-------------|
| `k8s-apply` | `PUBLISH` | Apply manifests (`kubectl apply`) |
| `k8s-status` | `VERIFY` | Check rollout / pod status |
| `k8s-rollout` | `RUN` | Watch and manage rollout progress |
| `k8s-port-forward` | `RUN` | Forward a service port to localhost |

## Configuration

Configuration key: `kubernetes`

```yaml
kubernetes-architected:
  namespace: production
  context: my-cluster
  manifests:
    - k8s/deployment.yaml
    - k8s/service.yaml
    - k8s/ingress.yaml
  resource: deployment/my-app
  portForward:
    service: my-app
    localPort: 8080
    remotePort: 8080
```

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `namespace` | `string` | `default` | Kubernetes namespace |
| `context` | `string` | — | Kubeconfig context name |
| `manifests` | `string[]` | `[]` | Paths to manifest files for `k8s-apply` |
| `resource` | `string` | — | Resource name for `k8s-rollout` / `k8s-status` (e.g. `deployment/my-app`) |
| `portForward.service` | `string` | — | Service name for port-forwarding |
| `portForward.localPort` | `int` | `8080` | Local port |
| `portForward.remotePort` | `int` | `80` | Remote port |

## Usage examples

```bash
# Deploy to cluster
architect k8s-apply

# Watch rollout
architect k8s-rollout

# Check status
architect k8s-status

# Forward service port for local testing
architect k8s-port-forward
```
