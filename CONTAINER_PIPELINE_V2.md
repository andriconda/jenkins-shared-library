# Container Pipeline V2 - Refactored

## What Changed

### ✅ **1. Fixed Container Images for Mandatory Stages**

**Before (V1):**
```groovy
containerPipeline(
    buildImage: 'maven:3.9',  // App could override
    testImage: 'maven:3.9'
)
```

**After (V2):**
```groovy
// Platform defines FIXED images in the pipeline code
def PLATFORM_BUILD_IMAGE = 'maven:3.9-eclipse-temurin-17'  // FIXED
def PLATFORM_TEST_IMAGE = 'maven:3.9-eclipse-temurin-17'   // FIXED
```

**App engineers CANNOT override mandatory stage containers!**

### ✅ **2. Custom Containers Only for Hooks**

**App engineers can specify containers for their hooks:**
```groovy
containerPipeline(
    gitUrl: '...',
    hookContainers: [
        before_build: 'maven:3.9-eclipse-temurin-21',  // Custom for hook
        after_build: 'alpine:latest'                    // Custom for hook
    ]
)
```

### ✅ **3. Stage Scripts in Separate Files**

**Before (V1):**
```groovy
// Inline in Groovy
stage('Build') {
    docker.image(buildImage).inside() {
        sh '''
            if [ -f "pom.xml" ]; then
                mvn clean compile
            fi
        '''
    }
}
```

**After (V2):**
```groovy
// Load from file
stage('Build') {
    runPlatformStage('Build', PLATFORM_BUILD_IMAGE, mavenCache)
}

// Helper function
def runPlatformStage(String stageName, String containerImage, String cacheVolume) {
    def stageScript = libraryResource "container-stages/${stageName.toLowerCase()}.sh"
    // Execute script in container
}
```

**Platform engineers edit:** `resources/container-stages/build.sh`

## Architecture

```
jenkins-shared-library/
├── resources/
│   └── container-stages/
│       ├── build.sh        ← Platform stage scripts
│       ├── test.sh
│       ├── security.sh
│       └── package.sh
└── vars/
    └── containerPipeline.groovy  ← Fixed container images

app-repo/
├── Jenkinsfile.container   ← Simple config
└── pipeline-hooks.sh       ← Optional hooks
```

## Platform Control

### Fixed Container Images (in containerPipeline.groovy):
```groovy
def PLATFORM_BUILD_IMAGE = 'maven:3.9-eclipse-temurin-17'
def PLATFORM_TEST_IMAGE = 'maven:3.9-eclipse-temurin-17'
def PLATFORM_SECURITY_IMAGE = 'maven:3.9-eclipse-temurin-17'
def PLATFORM_PACKAGE_IMAGE = 'maven:3.9-eclipse-temurin-17'
def PLATFORM_DOCKER_IMAGE = 'docker:24-cli'
```

### Stage Scripts (in resources/container-stages/):
```bash
# build.sh
#!/bin/bash
set -e
echo "=== Platform Build Stage ==="
if [ -f "pom.xml" ]; then
    mvn clean compile
elif [ -f "build.gradle" ]; then
    ./gradlew clean build
fi
```

## App Control

### Jenkinsfile (minimal):
```groovy
@Library('jenkins-shared-library') _

containerPipeline(
    gitUrl: 'https://github.com/org/app.git',
    gitBranch: 'main'
)
```

### Optional Hook Containers:
```groovy
containerPipeline(
    gitUrl: '...',
    hookContainers: [
        before_build: 'node:20',      // Run before_build in Node container
        after_test: 'python:3.11'     // Run after_test in Python container
    ]
)
```

### Optional Hooks (pipeline-hooks.sh):
```bash
#!/bin/bash

before_build() {
    echo "Custom pre-build"
}

after_build() {
    echo "Custom post-build"
}
```

## Benefits

### For Platform Engineers:
✅ **Full control** - Fixed containers, can't be overridden  
✅ **Easy updates** - Edit script files, not Groovy  
✅ **Consistent** - All apps use same containers  
✅ **Maintainable** - Scripts in separate files  

### For App Engineers:
✅ **Simple** - Just git URL, no container config  
✅ **Flexible hooks** - Can use custom containers for hooks  
✅ **No Groovy** - Only bash scripts  
✅ **Clear boundaries** - Can't modify mandatory stages  

## Platform Updates

### To Update Container Image:
**Edit `vars/containerPipeline.groovy`:**
```groovy
def PLATFORM_BUILD_IMAGE = 'maven:3.9-eclipse-temurin-21'  // Java 17 → 21
```

### To Update Stage Logic:
**Edit `resources/container-stages/build.sh`:**
```bash
#!/bin/bash
set -e
if [ -f "pom.xml" ]; then
    mvn clean compile -Pnew-profile  # Add new flag
fi
```

### Commit and Push:
```bash
git add vars/containerPipeline.groovy resources/container-stages/
git commit -m "Update to Java 21"
git push
```

**All apps get the update automatically!**

## Comparison: V1 vs V2

| Feature | V1 | V2 |
|---------|----|----|
| **Mandatory stage containers** | Configurable | **FIXED** |
| **Hook containers** | Same as mandatory | **Configurable** |
| **Stage logic** | Inline in Groovy | **Separate .sh files** |
| **Platform control** | Medium | **High** |
| **App flexibility** | High | **Appropriate** |
| **Maintainability** | Medium | **High** |

## Migration from V1 to V2

No changes needed for apps! The Jenkinsfile stays the same:

```groovy
// Works with both V1 and V2
containerPipeline(
    gitUrl: '...',
    gitBranch: 'main'
)
```

If apps were overriding container images, they need to remove that:

```groovy
// V1 (remove these)
containerPipeline(
    buildImage: 'maven:3.9',  // ← Remove
    testImage: 'maven:3.9'    // ← Remove
)

// V2 (clean)
containerPipeline(
    gitUrl: '...'
)
```

## Example: Custom Hook Container

```groovy
// Jenkinsfile
containerPipeline(
    gitUrl: 'https://github.com/org/app.git',
    hookContainers: [
        after_build: 'node:20-alpine'  // Use Node for after_build hook
    ]
)
```

```bash
# pipeline-hooks.sh
after_build() {
    # This runs in node:20-alpine container
    echo "Running Node.js script..."
    node scripts/post-build.js
}
```

## Summary

**V2 gives platform engineers full control while still allowing app engineers flexibility where it matters (hooks).**

✅ **Platform controls:** Container images, stage logic  
✅ **Apps control:** Hooks, hook containers  
✅ **Clear separation:** No confusion about what can be changed  
✅ **Easy maintenance:** Scripts in files, not inline  

This is the **ideal platform engineering model**! 🚀
