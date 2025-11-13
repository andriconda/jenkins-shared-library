# Platform Package Stage
# This is controlled by platform engineers
# App engineers CANNOT modify this

.PHONY: run

run:
	@echo "=== Platform Package Stage ==="
	@echo "Packaging application..."
	@if [ -f "pom.xml" ]; then \
		mvn package -DskipTests; \
	elif [ -f "build.gradle" ]; then \
		./gradlew assemble; \
	elif [ -f "package.json" ]; then \
		npm pack; \
	else \
		echo "ERROR: No package configuration found"; \
		exit 1; \
	fi
	@echo "Packaging completed successfully"
