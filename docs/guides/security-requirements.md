# Security Requirements

This document defines security requirements for the Architect Platform, covering remote downloads, plugin signatures, secret management, and generated workflow safety.

## 1. Remote Downloads

Plugin JARs and assets may be downloaded from GitHub Releases or other remote sources.

### Requirements

| Requirement                   | Enforcement                                          |
|-------------------------------|------------------------------------------------------|
| HTTPS only                    | All download URLs must use `https://` scheme          |
| Domain allowlist              | GitHub (`github.com`, `api.github.com`, `objects.githubusercontent.com`) and npm registry (`registry.npmjs.org`) |
| Content verification          | Downloaded JARs must match expected size or checksum when available |
| Timeout limits                | HTTP connections: 30s connect, 60s read timeout       |
| No redirects to HTTP          | Follow redirects only if target is also HTTPS         |
| Temporary file cleanup        | Downloaded files in temp directories must be cleaned up on failure |

### Current Implementation

- `GitHubPluginSource` downloads from GitHub API release assets
- `JdkRemoteContentFetcher` handles HTTP downloads with redirect following
- `ShellArgumentSanitizer` validates shell arguments before passing to process commands

## 2. Plugin Signatures (GPG Verification)

Plugins from GitHub or local sources may optionally include `.asc` signature files for GPG verification.

### Requirements

| Requirement                        | Enforcement                                                |
|------------------------------------|------------------------------------------------------------|
| Signature verification is opt-in   | Controlled by `verify: true` in plugin config              |
| Trusted keys must be explicit      | `trustedKeys` list in plugin config; no implicit trust      |
| Key format                         | Accept both short (16 hex) and long (40 hex) key IDs       |
| Verification failure = hard stop   | Throw `PluginLoadException`; never load unverified plugin   |
| Process/npm plugins cannot verify  | No discrete artifact exists; reject `verify: true` for these |

### GPG Invocation Safety

- Use `--batch --no-tty --status-fd 1` flags to prevent interactive prompts
- Parse machine-readable status output (`[GNUPG:]` lines), not human-readable output
- Normalize key IDs (strip `0x` prefix, remove non-hex characters, uppercase)
- Use suffix matching for key comparison (short key ID is suffix of fingerprint)

## 3. Secret Management

Secrets are resolved at runtime for task execution. Multiple resolution strategies exist.

### Requirements

| Requirement                     | Enforcement                                              |
|---------------------------------|----------------------------------------------------------|
| Never log secrets               | Secret values must not appear in logs at any level       |
| Never persist secrets to disk   | Secrets live only in memory during execution              |
| Environment variable isolation  | Secrets passed as env vars are scoped to the task process |
| Vault TLS required              | Vault connections must use HTTPS                          |
| Token rotation support          | Vault tokens should be short-lived; support renewal       |

### Resolution Chain

Secrets are resolved in priority order by `CompositeSecretResolver`:

1. **Environment variables** — `${env:VAR_NAME}` syntax
2. **File-based secrets** — `${file:/path/to/secret}` syntax  
3. **HashiCorp Vault** — `${vault:path/to/secret#key}` syntax

If a secret cannot be resolved, task execution fails with a clear error message (no fallback to empty string).

### Anti-Patterns

| ❌ Don't                                           | ✅ Do Instead                                |
|----------------------------------------------------|-----------------------------------------------|
| Log secret values for debugging                    | Log secret key names only                     |
| Write secrets to temp files                        | Pass via environment variables or stdin        |
| Hardcode secrets in `architect.yml`                | Use `${env:...}` or `${vault:...}` references |
| Fall back to empty string on resolution failure    | Fail fast with descriptive error              |

## 4. Generated Workflow Safety

The `github-architected` plugin generates GitHub Actions workflow YAML files. Generated content must be safe.

### Requirements

| Requirement                          | Enforcement                                              |
|--------------------------------------|----------------------------------------------------------|
| No secret injection in YAML          | Never interpolate secret values into generated YAML       |
| Use `${{ secrets.NAME }}` references | GitHub Actions resolves secrets at runtime, not generation |
| Template-based generation            | Use template files, not string concatenation               |
| Input sanitization                   | Sanitize branch names, version strings, and paths          |
| Path traversal prevention            | Validate output paths stay within project directory        |

### Input Sanitization Functions

Use the provided sanitizers in all plugins that handle external input:

| Function                                    | Purpose                                     |
|---------------------------------------------|---------------------------------------------|
| `ShellArgumentSanitizer.escapeShellArg()`   | Escape shell arguments for command execution |
| `ShellArgumentSanitizer.requireSafeIdentifier()` | Validate identifiers (no special chars) |
| `InputSanitizer.sanitizePath()`             | Remove path traversal sequences             |
| `InputSanitizer.sanitizeBranch()`           | Validate git branch name format             |
| `InputSanitizer.sanitizeVersion()`          | Validate semver-like version strings        |
| `GitCommandValidator.isValidGitCommand()`   | Validate git subcommand names               |
| `ShellCommandSanitizer.validateCommand()`   | Validate shell command safety               |

### Shell Execution Safety

All plugins that execute shell commands must:

1. **Escape arguments** — Use `ShellArgumentSanitizer.escapeShellArg()` for all user-provided values
2. **Validate commands** — Use domain-specific validators before execution
3. **Set working directory** — Always set `workingDir` to the project directory
4. **Limit environment** — Only pass required environment variables to subprocesses

## 5. Classloader Isolation

JAR-based plugins run in isolated classloaders to prevent dependency conflicts and limit capability.

### Requirements

| Requirement                         | Enforcement                                              |
|-------------------------------------|----------------------------------------------------------|
| Child-first class loading           | Plugin classes take precedence over platform classes     |
| Shared API packages only            | Only `io.github.architectplatform.api.*` and `org.slf4j.*` delegate to parent |
| No filesystem access beyond project | Plugins receive `ProjectContext` scoped to project dir    |
| No network access by default        | Plugins must declare network needs explicitly             |

## 6. Security Review Checklist

When reviewing code that touches security-sensitive areas:

- [ ] All external input is sanitized before use in shell commands
- [ ] No secret values appear in log output
- [ ] Downloaded content is verified (size, checksum, or signature)
- [ ] Generated YAML uses template references, not interpolated secrets
- [ ] File paths are validated against traversal attacks
- [ ] HTTP connections use HTTPS only
- [ ] Subprocess commands are escaped and validated
- [ ] Plugin classloader isolation is maintained (no shared mutable state)
