# Platform Test Stage
# This is controlled by platform engineers
# App engineers CANNOT modify this

.PHONY: run

run:
	@echo "=== Platform Test Stage ==="
	@echo "Running tests..."
	@if [ -f "pom.xml" ]; then \
		mvn test; \
	elif [ -f "build.gradle" ]; then \
		./gradlew test; \
	elif [ -f "package.json" ]; then \
		npm test; \
	else \
		echo "ERROR: No test configuration found"; \
		exit 1; \
	fi
	@echo "Tests completed successfully"
