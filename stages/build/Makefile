# Platform Build Stage
# This is controlled by platform engineers
# App engineers CANNOT modify this

.PHONY: run

run:
	@echo "=== Platform Build Stage ==="
	@echo "Building application..."
	@if [ -f "pom.xml" ]; then \
		mvn clean compile; \
	elif [ -f "build.gradle" ]; then \
		./gradlew clean build; \
	elif [ -f "package.json" ]; then \
		npm install && npm run build; \
	else \
		echo "ERROR: No build file found (pom.xml, build.gradle, package.json)"; \
		exit 1; \
	fi
	@echo "Build completed successfully"
