# terraform-architected

> **Status**: Experimental (incubating tier) — see [STATUS.md](STATUS.md)

## Overview

terraform-architected integrates HashiCorp Terraform into the Architect task lifecycle, providing tasks for planning, applying, and destroying infrastructure. It enables teams to manage infrastructure-as-code through standardized Architect commands.

## Tasks

| Task ID | Description |
|---|---|
| `tf-init` | Initialize the Terraform working directory |
| `tf-plan` | Create and show a Terraform execution plan |
| `tf-apply` | Apply infrastructure changes |
| `tf-destroy` | Destroy managed infrastructure |

## Configuration

Add to your `architect.yml`:

```yaml
plugins:
  - name: terraform-architected
    repo: architect-platform/terraform-architected

terraform:
  workspace: production
  backend: infra/backend.hcl
  vars:
    environment: production
    region: eu-west-1
  varFile: infra/production.tfvars
  autoApprove: false
  enabled: true
```

## Local Build and Test

```bash
cd plugins/terraform-architected/app
gradle build
gradle test
```

## Limitations

This plugin is at incubating tier. Tests and documentation are minimal, and the current implementation still needs broader validation across real Terraform lifecycle scenarios. See [STATUS.md](STATUS.md) for graduation criteria.
