# Platform Mandatory Stages

This directory contains Makefiles for mandatory pipeline stages.

## Structure

```
resources/stages/
├── build/Makefile      # Build stage
├── test/Makefile       # Test stage
├── security/Makefile   # Security scan stage
└── package/Makefile    # Package stage
```

These files are loaded via `libraryResource` in the pipeline.

## Purpose

These Makefiles define the **mandatory stages** that ALL applications must go through.

- **Platform engineers** maintain these files
- **App engineers** CANNOT modify these
- Changes here affect ALL applications using `makefilePipeline`

## Each Makefile Structure

Every stage Makefile has a single target: `run`

```makefile
.PHONY: run

run:
	@echo "=== Stage Name ==="
	# Stage logic here
```

## Supported Project Types

Each stage Makefile supports multiple project types:

- **Maven** (pom.xml)
- **Gradle** (build.gradle)
- **Node.js** (package.json)

## Modifying Stages

### To update a stage:

1. Edit the appropriate Makefile
2. Test locally if possible
3. Commit and push to shared library repo
4. All apps will use new version on next pipeline run

### Example: Update Build Stage

```bash
cd jenkins-shared-library/resources/stages/build
vi Makefile

# Make your changes
git add Makefile
git commit -m "Update build stage to use Java 17"
git push origin main
```

## Stage Descriptions

### build/Makefile
- **Purpose:** Compile/build the application
- **Maven:** `mvn clean compile`
- **Gradle:** `./gradlew clean build`
- **Node:** `npm install && npm run build`

### test/Makefile
- **Purpose:** Run unit and integration tests
- **Maven:** `mvn test`
- **Gradle:** `./gradlew test`
- **Node:** `npm test`

### security/Makefile
- **Purpose:** Security vulnerability scanning
- **Maven:** `mvn dependency-check:check`
- **Gradle:** `./gradlew dependencyCheckAnalyze`
- **Node:** `npm audit`

### package/Makefile
- **Purpose:** Package application for deployment
- **Maven:** `mvn package -DskipTests`
- **Gradle:** `./gradlew assemble`
- **Node:** `npm pack`

## Best Practices

1. **Keep stages simple** - Each stage should do one thing
2. **Support multiple project types** - Use conditionals for Maven/Gradle/Node
3. **Fail fast** - Exit with error code if stage fails
4. **Log clearly** - Echo what the stage is doing
5. **Test changes** - Test on a sample project before rolling out

## Adding New Stages

To add a new mandatory stage:

1. Create new directory: `mkdir resources/stages/newstage`
2. Create Makefile: `touch resources/stages/newstage/Makefile`
3. Add `run` target with stage logic
4. Update `vars/makefilePipeline.groovy` to call new stage with `runPlatformStage('Newstage')`
5. Document in this README

## Questions?

Contact the platform engineering team.
