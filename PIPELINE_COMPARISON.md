# Pipeline Comparison Guide

## Overview

You now have **3 pipeline options** in the shared library, each with different trade-offs:

## 1. mavenBuild (Original)

### Purpose
Maven-specific pipeline with optional Makefile hooks.

### Jenkins Requirements
- ✅ Maven installed
- ✅ Java installed
- ✅ Make installed (optional, for hooks)

### App Requirements
- Maven project (`pom.xml`)
- Optional: `Makefile` for hooks

### Jenkinsfile
```groovy
@Library('jenkins-shared-library') _

mavenBuild(
    gitUrl: 'https://github.com/org/app.git',
    gitBranch: 'main',
    mavenGoals: 'clean package',
    skipTests: true
)
```

### Hooks
Makefile targets: `before-build`, `after-build`, etc.

### Best For
- Maven-only organizations
- Teams familiar with Maven
- When you already have Maven/Java on Jenkins

---

## 2. makefilePipeline (Makefile-Based)

### Purpose
Multi-language pipeline using Makefiles for everything.

### Jenkins Requirements
- ✅ Maven installed
- ✅ Java installed
- ✅ Make installed
- ✅ Other tools as needed (Gradle, Node, etc.)

### App Requirements
- Build file (`pom.xml`, `build.gradle`, or `package.json`)
- Optional: `Makefile` for hooks

### Jenkinsfile
```groovy
@Library('jenkins-shared-library') _

makefilePipeline(
    gitUrl: 'https://github.com/org/app.git',
    gitBranch: 'main',
    mavenTool: 'Maven'
)
```

### Hooks
Makefile targets: `before-build`, `after-build`, etc.

### Platform Stages
Defined in `resources/stages/*/Makefile` in shared library.

### Best For
- Multi-language organizations
- Teams familiar with Make
- When you want platform-controlled Makefiles

---

## 3. containerPipeline (Container-Based) ⭐ **RECOMMENDED**

### Purpose
Modern container-based pipeline - NO tools needed on Jenkins!

### Jenkins Requirements
- ✅ Docker only!
- ❌ No Maven
- ❌ No Java
- ❌ No Make
- ❌ No other tools

### App Requirements
- Build file (`pom.xml`, `build.gradle`, or `package.json`)
- Optional: `pipeline-hooks.sh` for hooks

### Jenkinsfile
```groovy
@Library('jenkins-shared-library') _

containerPipeline(
    gitUrl: 'https://github.com/org/app.git',
    gitBranch: 'main'
)
```

### Hooks
Bash functions in `pipeline-hooks.sh`:
```bash
before_build() { ... }
after_build() { ... }
```

### Platform Stages
Defined in `vars/containerPipeline.groovy` as container commands.

### Best For
- **New projects** ⭐
- **Minimal Jenkins setup**
- **Maximum portability**
- **Consistent environments**
- **Any project type**

---

## Feature Comparison

| Feature | mavenBuild | makefilePipeline | containerPipeline |
|---------|------------|------------------|-------------------|
| **Tools on Jenkins** | Maven, Java | Make, Maven, Java | **Docker only** |
| **Setup complexity** | Medium | Medium | **Low** |
| **Portability** | Medium | Medium | **High** |
| **Consistency** | Medium | Medium | **High** |
| **Multi-language** | ❌ Maven only | ✅ Yes | ✅ Yes |
| **Hook format** | Makefile | Makefile | **Shell script** |
| **Platform updates** | Edit Groovy | Edit Makefiles | **Edit Groovy** |
| **Isolation** | ❌ Shared Jenkins | ❌ Shared Jenkins | **✅ Containers** |
| **Cache** | Jenkins workspace | Jenkins workspace | **Docker volume** |
| **Best for** | Maven projects | Multi-language | **Everything** |

## Detailed Comparison

### Jenkins Setup

#### mavenBuild
```bash
# On Jenkins server
apt-get install maven openjdk-17-jdk make
```

#### makefilePipeline
```bash
# On Jenkins server
apt-get install maven openjdk-17-jdk make
# Plus any other tools (gradle, node, etc.)
```

#### containerPipeline ⭐
```bash
# On Jenkins server
apt-get install docker.io
# That's it!
```

### App Files

#### mavenBuild
```
app/
├── Jenkinsfile
├── Makefile (optional)
├── pom.xml
└── src/
```

#### makefilePipeline
```
app/
├── Jenkinsfile
├── Makefile (optional)
├── pom.xml
└── src/
```

#### containerPipeline ⭐
```
app/
├── Jenkinsfile
├── pipeline-hooks.sh (optional)
├── pom.xml
└── src/
```

### Hook Examples

#### mavenBuild & makefilePipeline
```makefile
# Makefile
.PHONY: before-build after-build

before-build:
	@echo "Pre-build tasks"
	@java -version

after-build:
	@echo "Post-build tasks"
	@ls -lh target/
```

#### containerPipeline ⭐
```bash
# pipeline-hooks.sh
#!/bin/bash

before_build() {
    echo "Pre-build tasks"
    java -version
}

after_build() {
    echo "Post-build tasks"
    ls -lh target/
}
```

### Platform Updates

#### mavenBuild
Edit `vars/mavenBuild.groovy`:
```groovy
sh "mvn ${mavenGoals}"  // Change Maven command
```

#### makefilePipeline
Edit `resources/stages/build/Makefile`:
```makefile
run:
	mvn clean compile  # Change build command
```

#### containerPipeline ⭐
Edit `vars/containerPipeline.groovy`:
```groovy
docker.image(buildImage).inside(...) {
    sh 'mvn clean compile'  // Change build command
}
```

## Migration Path

### Current State → Target State

```
mavenBuild
    ↓
makefilePipeline (if multi-language needed)
    ↓
containerPipeline ⭐ (recommended end state)
```

### Migration Steps

#### From mavenBuild to containerPipeline:

1. **Create new files:**
   ```bash
   cp Jenkinsfile.container Jenkinsfile
   ```

2. **Convert Makefile to pipeline-hooks.sh:**
   ```bash
   # Makefile
   before-build:
       @echo "Hello"
   
   # pipeline-hooks.sh
   before_build() {
       echo "Hello"
   }
   ```

3. **Test and commit:**
   ```bash
   git add Jenkinsfile pipeline-hooks.sh
   git commit -m "Migrate to container-based pipeline"
   git push
   ```

4. **Verify:**
   - Run pipeline in Jenkins
   - Check all stages complete
   - Verify hooks run correctly

## Recommendation

### For New Projects: containerPipeline ⭐

**Why:**
- ✅ Minimal Jenkins setup (Docker only)
- ✅ Maximum portability
- ✅ Consistent environments
- ✅ Easy to maintain
- ✅ Future-proof

### For Existing Projects:

**If you have Maven/Java on Jenkins:**
- Keep `mavenBuild` (works fine)
- Or migrate to `containerPipeline` for benefits

**If you need multi-language:**
- Use `makefilePipeline` (if you want Makefiles)
- Or use `containerPipeline` (more portable)

### For Platform Teams:

**Start with:** `containerPipeline`
- Easiest to support
- Minimal Jenkins requirements
- Most flexible

**Maintain:** All three for backward compatibility
- Teams can migrate at their own pace
- No forced migrations

## Summary

| Pipeline | Use When | Avoid When |
|----------|----------|------------|
| **mavenBuild** | Maven-only org, tools already installed | Multi-language, minimal Jenkins |
| **makefilePipeline** | Multi-language, like Makefiles | Want minimal Jenkins setup |
| **containerPipeline** ⭐ | **New projects, minimal setup, max portability** | **Docker not available** |

## Next Steps

1. **Choose your pipeline** based on requirements
2. **Create Jenkinsfile** from appropriate template
3. **Add hooks** if needed (Makefile or pipeline-hooks.sh)
4. **Test** in Jenkins
5. **Document** your choice for team

All three pipelines coexist in the shared library - use what works best for your team! 🚀
