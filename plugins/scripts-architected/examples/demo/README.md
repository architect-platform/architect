# Scripts Plugin Demo

This example demonstrates the capabilities of the `scripts-architected` plugin.

## Overview

This demo project shows:
- Simple standalone scripts
- Scripts attached to workflow phases (BUILD, TEST, PUBLISH)
- Scripts with environment variables
- Scripts with custom working directories
- Scripts that accept command-line arguments

## Configuration

The `architect.yml` file defines several example scripts:

1. **hello** - Simple greeting script (standalone)
2. **build-app** - Build script attached to BUILD phase
3. **run-tests** - Test script attached to TEST phase
4. **deploy** - Deployment script with environment variables (PUBLISH phase)
5. **list-files** - Script with custom working directory
6. **echo-args** - Script that accepts CLI arguments

## Usage

### Prerequisites

1. Install Architect CLI and Engine
2. Ensure the scripts-architected plugin is available

### Running Scripts

Execute individual scripts:

```bash
# Simple hello
architect scripts-hello

# Build the app
architect scripts-build-app

# Run tests
architect scripts-run-tests

# Deploy (with environment variables)
architect scripts-deploy

# List files in scripts directory
architect scripts-list-files

# Echo arguments
architect scripts-echo-args -- arg1 arg2 "arg with spaces"
```

### Running Full Workflow

Execute scripts as part of the workflow:

```bash
# Run all phases (INIT -> BUILD -> TEST -> PUBLISH)
# This will execute build-app, run-tests, and deploy in order
architect
```

## Expected Output

### scripts-hello
```
Hello from scripts-architected plugin!
```

### scripts-build-app
```
Building application...
Build complete!
```

### scripts-run-tests
```
Running tests...
All tests passed!
```

### scripts-deploy
```
Deploying to production environment in us-east-1 region
```

### scripts-list-files
```
/path/to/demo/scripts
total 12
drwxrwxr-x 2 user user 4096 Nov  2 14:45 .
drwxrwxr-x 3 user user 4096 Nov  2 14:45 ..
-rw-rw-r-- 1 user user   20 Nov  2 14:45 test.txt
```

## Customization

Feel free to modify the `architect.yml` to:
- Add your own custom scripts
- Change environment variables
- Attach scripts to different phases
- Modify working directories
- Update commands to match your workflow

## Learn More

See the main [scripts-architected README](../../README.md) for complete documentation.
