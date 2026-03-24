# terraform-architected

Terraform infrastructure-as-code integration for Architect.

**Source**: `architect-platform/terraform-architected`

## Installation

```yaml
plugins:
  - name: terraform-architected
    type: github
    repo: architect-platform/terraform-architected
```

## Tasks

| Task ID | Phase | Description |
|---------|-------|-------------|
| `tf-init` | `INIT` | Initialize Terraform working directory (`terraform init`) |
| `tf-plan` | `VERIFY` | Generate and show execution plan |
| `tf-apply` | `PUBLISH` | Apply changes to reach desired state |
| `tf-destroy` | `PUBLISH` | Destroy provisioned infrastructure |

## Configuration

Configuration key: `terraform`

```yaml
terraform-architected:
  workingDirectory: infra/
  workspace: production
  varFile: infra/production.tfvars
  vars:
    environment: production
    region: eu-west-1
  backendConfig: infra/backend.hcl
  autoApprove: false
```

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `workingDirectory` | `string` | `.` | Directory containing Terraform configuration |
| `workspace` | `string` | `default` | Terraform workspace |
| `varFile` | `string` | — | Path to `.tfvars` file |
| `vars` | `map<string,string>` | `{}` | Inline variable overrides (`-var key=value`) |
| `backendConfig` | `string` | — | Path to backend configuration file |
| `autoApprove` | `boolean` | `false` | Automatically approve `tf-apply` and `tf-destroy` |

## Usage examples

```bash
# Initialize
architect tf-init

# Plan changes (safe, read-only)
architect tf-plan

# Apply changes
architect tf-apply

# Destroy (destructive — confirm before running in production)
architect tf-destroy
```

## Security notes

Variable values are shell-escaped before being passed to Terraform. Do not store sensitive values (passwords, tokens) in `architect.yml`; use environment variables or a secrets manager and reference them via `TF_VAR_*` environment variables.
