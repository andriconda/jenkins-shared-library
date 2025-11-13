# Jenkins Shared Library

A collection of reusable Jenkins pipeline functions for common CI/CD tasks.

## Available Functions

### mavenBuild

Builds Maven-based projects with configurable options.

**Usage:**

```groovy
@Library('jenkins-shared-library') _

mavenBuild(
    gitUrl: 'https://github.com/your-org/your-repo.git',
    gitBranch: 'main',
    mavenGoals: 'clean package',
    skipTests: true,
    mavenTool: 'Maven',
    cleanCache: true
)
```

**Parameters:**

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `gitUrl` | String | (required) | Git repository URL |
| `gitBranch` | String | `'main'` | Git branch to checkout |
| `mavenGoals` | String | `'clean package'` | Maven goals to execute |
| `skipTests` | Boolean | `true` | Skip tests during build |
| `mavenTool` | String | `'Maven'` | Name of Maven tool configured in Jenkins |
| `cleanCache` | Boolean | `true` | Clean build artifacts and Maven cache before build |

## Setup in Jenkins

1. Go to **Manage Jenkins** → **System**
2. Scroll to **Global Pipeline Libraries**
3. Click **Add** and configure:
   - **Name**: `jenkins-shared-library`
   - **Default version**: `main`
   - **Retrieval method**: Modern SCM → Git
   - **Project Repository**: `https://github.com/andriconda/jenkins-shared-library.git`
4. Save

## Features

- ✅ Automatic cache cleaning before builds
- ✅ Configurable Maven goals
- ✅ Test skipping option
- ✅ Artifact archiving
- ✅ Workspace cleanup
- ✅ Build status notifications
- ✅ **Custom stage injection via Makefile hooks**

## Custom Stages via Makefile

Projects can inject custom stages into the pipeline by creating a `Makefile` with specific targets:

### Pre-Build Hook

Runs before Maven build:

```makefile
.PHONY: pre-build

pre-build:
	@echo "Running validation..."
	@java -version
	@mvn -version
```

### Post-Build Hook

Runs after Maven build:

```makefile
.PHONY: post-build

post-build:
	@echo "Running additional tests..."
	@make test
	@make security-scan
```

### Benefits

- **Standardized**: All projects use the same Makefile interface
- **Flexible**: Each project defines its own custom logic
- **Optional**: Hooks only run if targets exist
- **No Jenkinsfile changes**: Customize without modifying pipeline code

## Contributing

To add new shared library functions:
1. Create a new `.groovy` file in the `vars/` directory
2. Add corresponding `.txt` documentation file
3. Update this README

## Directory Structure

```
jenkins-shared-library/
├── README.md
├── vars/
│   ├── mavenBuild.groovy   # Maven build pipeline
│   └── mavenBuild.txt      # Documentation
└── resources/              # Shared resources (optional)
```
