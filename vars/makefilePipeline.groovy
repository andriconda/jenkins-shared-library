#!/usr/bin/env groovy

/**
 * Makefile-Based Pipeline
 * 
 * MANDATORY STAGES: Use Makefiles from shared library repo (platform controlled)
 * OPTIONAL HOOKS: Use Makefiles from app repo (before-* and after-* targets)
 * 
 * Platform engineers control: Makefile in shared library
 * App engineers control: before-build, after-build, etc. in app repo Makefile
 */

// Helper to run platform stage
def runPlatformStage(String stageName, String stageScript) {
    writeFile file: ".platform-${stageName}.mk", text: """
.PHONY: run
run:
\t@echo "=== Platform ${stageName} Stage ==="
${stageScript}
\t@echo "${stageName} completed successfully"
"""
    sh "make -f .platform-${stageName}.mk run"
    sh "rm -f .platform-${stageName}.mk"
}

def call(Map config = [:]) {
    // Required parameters
    def gitUrl = config.gitUrl ?: error("gitUrl is required")
    def gitBranch = config.gitBranch ?: 'main'
    
    // Optional: Maven tool configuration
    def mavenTool = config.mavenTool ?: 'Maven'
    
    pipeline {
        agent any
        
        tools {
            maven "${mavenTool}"
        }
        
        stages {
            stage('Setup') {
                steps {
                    script {
                        echo "=== Makefile-Based Pipeline Setup ==="
                        
                        // Checkout application code
                        echo "Checking out ${gitBranch} from ${gitUrl}"
                        git branch: gitBranch, url: gitUrl
                        
                        // Check if make is available
                        env.MAKE_AVAILABLE = sh(
                            script: 'command -v make >/dev/null 2>&1',
                            returnStatus: true
                        ) == 0 ? 'true' : 'false'
                        
                        if (env.MAKE_AVAILABLE != 'true') {
                            error("ERROR: 'make' command not found. Please install make in Jenkins environment.")
                        }
                        
                        // Check if app has Makefile for hooks
                        env.APP_MAKEFILE_EXISTS = fileExists('Makefile') ? 'true' : 'false'
                        
                        if (env.APP_MAKEFILE_EXISTS == 'true') {
                            echo "✓ App Makefile found - before/after hooks available"
                        } else {
                            echo "ℹ No app Makefile - no before/after hooks"
                        }
                    }
                }
            }
            
            stage('Before Build') {
                when {
                    expression { 
                        env.APP_MAKEFILE_EXISTS == 'true' &&
                        sh(script: 'grep -q "^before-build:" Makefile', returnStatus: true) == 0
                    }
                }
                steps {
                    script {
                        echo "=== Before Build Hook (App) ==="
                        sh 'make before-build'
                    }
                }
            }
            
            stage('Build') {
                steps {
                    script {
                        def buildScript = '''
\t@echo "Building application..."
\t@if [ -f "pom.xml" ]; then \\
\t\tmvn clean compile; \\
\telif [ -f "build.gradle" ]; then \\
\t\t./gradlew clean build; \\
\telif [ -f "package.json" ]; then \\
\t\tnpm install && npm run build; \\
\telse \\
\t\techo "ERROR: No build file found (pom.xml, build.gradle, package.json)"; \\
\t\texit 1; \\
\tfi'''
                        runPlatformStage('Build', buildScript)
                    }
                }
            }
            
            stage('After Build') {
                when {
                    expression { 
                        env.APP_MAKEFILE_EXISTS == 'true' &&
                        sh(script: 'grep -q "^after-build:" Makefile', returnStatus: true) == 0
                    }
                }
                steps {
                    script {
                        echo "=== After Build Hook (App) ==="
                        sh 'make after-build'
                    }
                }
            }
            
            stage('Before Test') {
                when {
                    expression { 
                        env.APP_MAKEFILE_EXISTS == 'true' &&
                        sh(script: 'grep -q "^before-test:" Makefile', returnStatus: true) == 0
                    }
                }
                steps {
                    script {
                        echo "=== Before Test Hook (App) ==="
                        sh 'make before-test'
                    }
                }
            }
            
            stage('Test') {
                steps {
                    script {
                        def testScript = '''
\t@echo "Running tests..."
\t@if [ -f "pom.xml" ]; then \\
\t\tmvn test; \\
\telif [ -f "build.gradle" ]; then \\
\t\t./gradlew test; \\
\telif [ -f "package.json" ]; then \\
\t\tnpm test; \\
\telse \\
\t\techo "ERROR: No test configuration found"; \\
\t\texit 1; \\
\tfi'''
                        runPlatformStage('Test', testScript)
                    }
                }
            }
            
            stage('After Test') {
                when {
                    expression { 
                        env.APP_MAKEFILE_EXISTS == 'true' &&
                        sh(script: 'grep -q "^after-test:" Makefile', returnStatus: true) == 0
                    }
                }
                steps {
                    script {
                        echo "=== After Test Hook (App) ==="
                        sh 'make after-test'
                    }
                }
            }
            
            stage('Before Security') {
                when {
                    expression { 
                        env.APP_MAKEFILE_EXISTS == 'true' &&
                        sh(script: 'grep -q "^before-security:" Makefile', returnStatus: true) == 0
                    }
                }
                steps {
                    script {
                        echo "=== Before Security Hook (App) ==="
                        sh 'make before-security'
                    }
                }
            }
            
            stage('Security Scan') {
                steps {
                    script {
                        def securityScript = '''
\t@echo "Running security scans..."
\t@if [ -f "pom.xml" ]; then \\
\t\tmvn dependency-check:check || echo "Warning: Security scan had issues"; \\
\t\tmvn dependency:tree > dependency-tree.txt; \\
\telif [ -f "build.gradle" ]; then \\
\t\t./gradlew dependencyCheckAnalyze || echo "Warning: Security scan had issues"; \\
\telif [ -f "package.json" ]; then \\
\t\tnpm audit || echo "Warning: Security scan had issues"; \\
\telse \\
\t\techo "Warning: No security scan available for this project type"; \\
\tfi'''
                        runPlatformStage('Security', securityScript)
                    }
                }
            }
            
            stage('After Security') {
                when {
                    expression { 
                        env.APP_MAKEFILE_EXISTS == 'true' &&
                        sh(script: 'grep -q "^after-security:" Makefile', returnStatus: true) == 0
                    }
                }
                steps {
                    script {
                        echo "=== After Security Hook (App) ==="
                        sh 'make after-security'
                    }
                }
            }
            
            stage('Before Package') {
                when {
                    expression { 
                        env.APP_MAKEFILE_EXISTS == 'true' &&
                        sh(script: 'grep -q "^before-package:" Makefile', returnStatus: true) == 0
                    }
                }
                steps {
                    script {
                        echo "=== Before Package Hook (App) ==="
                        sh 'make before-package'
                    }
                }
            }
            
            stage('Package') {
                steps {
                    script {
                        def packageScript = '''
\t@echo "Packaging application..."
\t@if [ -f "pom.xml" ]; then \\
\t\tmvn package -DskipTests; \\
\telif [ -f "build.gradle" ]; then \\
\t\t./gradlew assemble; \\
\telif [ -f "package.json" ]; then \\
\t\tnpm pack; \\
\telse \\
\t\techo "ERROR: No package configuration found"; \\
\t\texit 1; \\
\tfi'''
                        runPlatformStage('Package', packageScript)
                    }
                }
            }
            
            stage('After Package') {
                when {
                    expression { 
                        env.APP_MAKEFILE_EXISTS == 'true' &&
                        sh(script: 'grep -q "^after-package:" Makefile', returnStatus: true) == 0
                    }
                }
                steps {
                    script {
                        echo "=== After Package Hook (App) ==="
                        sh 'make after-package'
                    }
                }
            }
            
            stage('Archive') {
                steps {
                    script {
                        echo "=== Archiving Artifacts ==="
                        // Archive common artifact patterns
                        if (fileExists('target')) {
                            archiveArtifacts artifacts: '**/target/*.jar', allowEmptyArchive: true
                            archiveArtifacts artifacts: '**/target/*.war', allowEmptyArchive: true
                        }
                        if (fileExists('build/libs')) {
                            archiveArtifacts artifacts: 'build/libs/*.jar', allowEmptyArchive: true
                        }
                        if (fileExists('dist')) {
                            archiveArtifacts artifacts: 'dist/**/*', allowEmptyArchive: true
                        }
                    }
                }
            }
        }
        
        post {
            success {
                script {
                    echo "✓ Pipeline completed successfully!"
                }
            }
            failure {
                script {
                    echo "✗ Pipeline failed!"
                }
            }
            always {
                script {
                    cleanWs()
                }
            }
        }
    }
}
