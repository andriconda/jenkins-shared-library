#!/usr/bin/env groovy

/**
 * Standard Pipeline for Platform Engineering
 * 
 * Platform engineers define the stages here.
 * App engineers can only customize via:
 * 1. Makefile targets (build, test, security-scan, package, deploy, etc.)
 * 2. Container images for each stage
 * 3. Simple configuration parameters
 */

// Execute a stage using Makefile or container
def executeStage(String stageName, String makeTarget, Map config) {
    def container = config.containers?.get(stageName)
    def containerArgs = config.containerArgs?.get(stageName) ?: ''
    
    if (container) {
        // Run in specified container
        echo "Running ${stageName} in container: ${container}"
        docker.image(container).inside(containerArgs) {
            if (env.MAKE_AVAILABLE == 'true') {
                def hasTarget = sh(
                    script: "grep -q '^${makeTarget}:' Makefile",
                    returnStatus: true
                ) == 0
                
                if (hasTarget) {
                    sh "make ${makeTarget}"
                } else {
                    echo "No '${makeTarget}' target in Makefile, skipping"
                }
            }
        }
    } else if (env.MAKE_AVAILABLE == 'true') {
        // Run via Makefile on host
        def hasTarget = sh(
            script: "grep -q '^${makeTarget}:' Makefile",
            returnStatus: true
        ) == 0
        
        if (hasTarget) {
            echo "Running ${stageName} via Makefile target: ${makeTarget}"
            sh "make ${makeTarget}"
        } else {
            echo "No '${makeTarget}' target in Makefile, skipping ${stageName}"
        }
    } else {
        echo "WARNING: Neither container nor make available for ${stageName}, skipping"
    }
}

def call(Map config = [:]) {
    // Required parameters
    def gitUrl = config.gitUrl ?: error("gitUrl is required")
    def gitBranch = config.gitBranch ?: 'main'
    
    // Optional: containers for each stage
    def containers = config.containers ?: [:]
    def containerArgs = config.containerArgs ?: [:]
    
    pipeline {
        agent any
        
        stages {
            // PLATFORM STAGE 1: Setup
            stage('Setup') {
                steps {
                    script {
                        echo "=== Platform Pipeline Setup ==="
                        
                        // Check Makefile and make availability
                        env.MAKEFILE_EXISTS = fileExists('Makefile') ? 'true' : 'false'
                        env.MAKE_AVAILABLE = 'false'
                        
                        if (env.MAKEFILE_EXISTS == 'true') {
                            env.MAKE_AVAILABLE = sh(
                                script: 'command -v make >/dev/null 2>&1',
                                returnStatus: true
                            ) == 0 ? 'true' : 'false'
                            
                            if (env.MAKE_AVAILABLE == 'true') {
                                echo "✓ Makefile detected and make is available"
                            } else {
                                echo "⚠ Makefile found but make command not available"
                            }
                        } else {
                            echo "⚠ No Makefile found - stages will use containers only"
                        }
                        
                        // Checkout code
                        echo "Checking out ${gitBranch} from ${gitUrl}"
                        git branch: gitBranch, url: gitUrl
                        
                        // Run post-checkout hook if exists
                        if (env.MAKE_AVAILABLE == 'true') {
                            def hasCheckout = sh(
                                script: "grep -q '^post-checkout:' Makefile",
                                returnStatus: true
                            ) == 0
                            if (hasCheckout) {
                                sh 'make post-checkout'
                            }
                        }
                    }
                }
            }
            
            // PLATFORM STAGE 2: Build
            stage('Build') {
                steps {
                    script {
                        echo "=== Build Stage ==="
                        executeStage('Build', 'build', config)
                    }
                }
            }
            
            // PLATFORM STAGE 3: Test
            stage('Test') {
                steps {
                    script {
                        echo "=== Test Stage ==="
                        executeStage('Test', 'test', config)
                    }
                }
            }
            
            // PLATFORM STAGE 4: Security Scan
            stage('Security Scan') {
                steps {
                    script {
                        echo "=== Security Scan Stage ==="
                        executeStage('Security', 'security-scan', config)
                    }
                }
            }
            
            // PLATFORM STAGE 5: Package
            stage('Package') {
                steps {
                    script {
                        echo "=== Package Stage ==="
                        executeStage('Package', 'package', config)
                        
                        // Archive artifacts
                        if (fileExists('target')) {
                            archiveArtifacts artifacts: '**/target/*.jar', allowEmptyArchive: true
                        }
                        if (fileExists('dist')) {
                            archiveArtifacts artifacts: 'dist/**/*', allowEmptyArchive: true
                        }
                    }
                }
            }
            
            // PLATFORM STAGE 6: Deploy (optional)
            stage('Deploy') {
                when {
                    expression { 
                        env.MAKE_AVAILABLE == 'true' && 
                        sh(script: "grep -q '^deploy:' Makefile", returnStatus: true) == 0 
                    }
                }
                steps {
                    script {
                        echo "=== Deploy Stage ==="
                        executeStage('Deploy', 'deploy', config)
                    }
                }
            }
        }
        
        post {
            success {
                script {
                    echo "✓ Pipeline completed successfully!"
                    if (env.MAKE_AVAILABLE == 'true') {
                        def hasSuccess = sh(
                            script: "grep -q '^on-success:' Makefile",
                            returnStatus: true
                        ) == 0
                        if (hasSuccess) {
                            sh 'make on-success'
                        }
                    }
                }
            }
            failure {
                script {
                    echo "✗ Pipeline failed!"
                    if (env.MAKE_AVAILABLE == 'true') {
                        def hasFailure = sh(
                            script: "grep -q '^on-failure:' Makefile",
                            returnStatus: true
                        ) == 0
                        if (hasFailure) {
                            sh 'make on-failure'
                        }
                    }
                }
            }
            always {
                script {
                    if (env.MAKE_AVAILABLE == 'true') {
                        def hasCleanup = sh(
                            script: "grep -q '^cleanup:' Makefile",
                            returnStatus: true
                        ) == 0
                        if (hasCleanup) {
                            sh 'make cleanup'
                        }
                    }
                    cleanWs()
                }
            }
        }
    }
}
