# Security Policy

## Supported Versions

We release patches for security vulnerabilities for the following versions:

| Version | Supported          |
| ------- | ------------------ |
| 1.x.x   | :white_check_mark: |
| < 1.0   | :x:                |

## Reporting a Vulnerability

**Please do not report security vulnerabilities through public GitHub issues.**

Instead, please report them via:
- **GitHub Security Advisories**: [Report a vulnerability](https://github.com/architect-platform/architect/security/advisories/new)
- **Email**: security@architect-platform.io (if available)

You should receive a response within 48 hours. If for some reason you do not, please follow up to ensure we received your original message.

Please include the following information in your report:

- Type of vulnerability
- Full paths of source file(s) related to the vulnerability
- Location of the affected source code (tag/branch/commit or direct URL)
- Step-by-step instructions to reproduce the issue
- Proof-of-concept or exploit code (if possible)
- Impact of the vulnerability
- Any potential mitigations you've identified

## Security Update Process

1. Security vulnerabilities are reviewed and validated by maintainers
2. A fix is developed in a private repository
3. A security advisory is drafted
4. The fix is tested thoroughly
5. A new version is released with the security patch
6. The security advisory is published
7. Users are notified through release notes and GitHub notifications

## Security Best Practices

### For Contributors

- Never commit sensitive data (credentials, tokens, keys)
- Use environment variables for secrets
- Sanitize all user inputs, especially in shell commands
- Follow secure coding guidelines
- Run security scanners on code changes
- Keep dependencies up to date

### For Users

- Keep Architect Platform updated to the latest version
- Use strong authentication methods
- Regularly review access permissions
- Monitor logs for suspicious activity
- Use secure communication channels (HTTPS, SSH)
- Scan plugins from untrusted sources

## Known Security Considerations

### Command Execution

The platform executes shell commands. Always:
- Sanitize inputs before passing to CommandExecutor
- Use parameterized commands when possible
- Validate file paths to prevent traversal attacks
- Run with least privilege necessary

### Plugin System

Plugins have significant access to the system. Only use:
- Official plugins from architect-platform organization
- Plugins from trusted sources
- Plugins you've reviewed the source code for

### Configuration Files

- Store sensitive configuration outside of version control
- Use environment variables for secrets
- Restrict file permissions on configuration files
- Encrypt sensitive data at rest

## Dependencies

We use automated tools to track and update dependencies:
- **Renovate**: Automatically creates PRs for dependency updates
- **GitHub Dependabot**: Security alerts for vulnerable dependencies
- **CodeQL**: Static analysis for security vulnerabilities

## Security Tools

The project uses several security tools:
- **CodeQL**: Automated code scanning
- **OWASP Dependency Check**: Dependency vulnerability scanning
- **Trivy**: Container vulnerability scanning
- **SonarQube**: Code quality and security analysis (if available)

## Disclosure Policy

- Security issues are disclosed publicly after a fix is available
- Credit is given to researchers who report vulnerabilities responsibly
- We aim for full transparency while protecting users

## Contact

For security inquiries: security@architect-platform.io (or create a private security advisory)

For general questions: Use [GitHub Discussions](https://github.com/architect-platform/architect/discussions)

## Acknowledgments

We appreciate the security research community and thank all researchers who help keep Architect Platform secure.

---

Last updated: 2024-11-02
