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
