#!/usr/bin/env groovy

/**
 * Container-Based Pipeline
 * 
 * MANDATORY STAGES: Run in FIXED standard containers (platform controlled)
 * OPTIONAL HOOKS: Can specify custom containers (app controlled)
 * 
 * NO TOOLS REQUIRED ON JENKINS!
 * Everything runs in containers with all tools pre-installed.
 * 
 * Platform engineers control: 
 *   - Container images for mandatory stages (FIXED)
 *   - Stage scripts in resources/container-stages/
 * 
 * App engineers control: 
 *   - Optional before/after hooks
 *   - Container images for hooks only
 */

// Helper to run platform stage from script file
def runPlatformStage(String stageName, String containerImage, String cacheVolume) {
    // Load script from shared library resources
    def stageScript = libraryResource "container-stages/${stageName.toLowerCase()}.sh"
    
    // Write to temporary file
    writeFile file: ".stage-${stageName}.sh", text: stageScript
    sh "chmod +x .stage-${stageName}.sh"
    
    // Execute in container
    docker.image(containerImage).inside("-v ${cacheVolume}:/root/.m2") {
        sh "./.stage-${stageName}.sh"
    }
    
    // Cleanup
    sh "rm -f .stage-${stageName}.sh"
}

// Helper to run custom app stage
def runCustomStage(String stageName, Map stageConfig, String defaultImage, String cacheVolume) {
    def container = stageConfig.container ?: defaultImage
    def script = stageConfig.script
    def scriptFile = stageConfig.scriptFile
    def continueOnFailure = stageConfig.continueOnFailure ?: false
    
    if (scriptFile) {
        // Run script file from app repo
        docker.image(container).inside("-v ${cacheVolume}:/root/.m2") {
            sh "chmod +x ${scriptFile}"
            
            def exitCode = sh(script: "./${scriptFile}", returnStatus: true)
            if (exitCode != 0) {
                if (continueOnFailure) {
                    echo "WARNING: Custom stage '${stageName}' failed with exit code ${exitCode} (continueOnFailure=true)"
                    currentBuild.result = 'UNSTABLE'
                } else {
                    error("Custom stage '${stageName}' failed with exit code ${exitCode}")
                }
            }
        }
    } else if (script) {
        // Run inline script
        docker.image(container).inside("-v ${cacheVolume}:/root/.m2") {
            def exitCode = sh(script: script, returnStatus: true)
            if (exitCode != 0) {
                if (continueOnFailure) {
                    echo "WARNING: Custom stage '${stageName}' failed with exit code ${exitCode} (continueOnFailure=true)"
                    currentBuild.result = 'UNSTABLE'
                } else {
                    error("Custom stage '${stageName}' failed with exit code ${exitCode}")
                }
            }
        }
    } else {
        echo "WARNING: Custom stage '${stageName}' has no script or scriptFile defined"
    }
}

// Helper to create custom stages dynamically
def createCustomStages(String afterStage, Map customStages, String defaultImage, String cacheVolume) {
    def stages = [:]
    customStages.findAll { it.value.after == afterStage }.each { stageName, stageConfig ->
        stages[stageName] = {
            echo "=== Custom Stage: ${stageName} (App) ==="
            runCustomStage(stageName, stageConfig, defaultImage, cacheVolume)
        }
    }
    return stages
}

def call(Map config = [:]) {
    // Required parameters
    def gitUrl = config.gitUrl ?: error("gitUrl is required")
    def gitBranch = config.gitBranch ?: 'main'
    
    // FIXED container images for mandatory stages (platform controlled)
    def PLATFORM_BUILD_IMAGE = 'maven:3.9-eclipse-temurin-17'
    def PLATFORM_TEST_IMAGE = 'maven:3.9-eclipse-temurin-17'
    def PLATFORM_SECURITY_IMAGE = 'maven:3.9-eclipse-temurin-17'
    def PLATFORM_PACKAGE_IMAGE = 'maven:3.9-eclipse-temurin-17'
    def PLATFORM_DOCKER_IMAGE = 'docker:24-cli'
    
    // Optional: Custom stages (app controlled)
    def customStages = config.customStages ?: [:]
    
    // Maven cache volume
    def mavenCache = config.mavenCache ?: 'maven-repo'
    
    pipeline {
        agent any
        
        options {
            skipDefaultCheckout()
        }
        
        stages {
            stage('Setup') {
                steps {
                    script {
                        echo "=== Container-Based Pipeline Setup ==="
                        echo "All stages run in containers - no tools needed on Jenkins!"
                        
                        // Clean workspace to avoid stale git state
                        cleanWs()
                        
                        // Checkout application code
                        echo "Checking out ${gitBranch} from ${gitUrl}"
                        checkout([
                            $class: 'GitSCM',
                            branches: [[name: "*/${gitBranch}"]],
                            userRemoteConfigs: [[url: gitUrl]]
                        ])
                    }
                }
            }
            
            stage('Build') {
                steps {
                    script {
                        echo "=== Build Stage (Platform Mandatory) ==="
                        runPlatformStage('Build', PLATFORM_BUILD_IMAGE, mavenCache)
                    }
                }
            }
            
            stage('Custom: After Build') {
                when {
                    expression { customStages.any { it.value.after == 'build' } }
                }
                steps {
                    script {
                        def buildCustomStages = createCustomStages('build', customStages, PLATFORM_BUILD_IMAGE, mavenCache)
                        buildCustomStages['failFast'] = true
                        parallel buildCustomStages
                    }
                }
            }
            
            stage('Test') {
                steps {
                    script {
                        echo "=== Test Stage (Platform Mandatory) ==="
                        runPlatformStage('Test', PLATFORM_TEST_IMAGE, mavenCache)
                    }
                }
            }
            
            stage('Custom: After Test') {
                when {
                    expression { customStages.any { it.value.after == 'test' } }
                }
                steps {
                    script {
                        def testCustomStages = createCustomStages('test', customStages, PLATFORM_TEST_IMAGE, mavenCache)
                        testCustomStages['failFast'] = true
                        parallel testCustomStages
                    }
                }
            }
            
            stage('Security Scan') {
                steps {
                    script {
                        echo "=== Security Scan Stage (Platform Mandatory) ==="
                        runPlatformStage('Security', PLATFORM_SECURITY_IMAGE, mavenCache)
                    }
                }
            }
            
            stage('Custom: After Security') {
                when {
                    expression { customStages.any { it.value.after == 'security' } }
                }
                steps {
                    script {
                        def securityCustomStages = createCustomStages('security', customStages, PLATFORM_SECURITY_IMAGE, mavenCache)
                        securityCustomStages['failFast'] = true
                        parallel securityCustomStages
                    }
                }
            }
            
            stage('Package') {
                steps {
                    script {
                        echo "=== Package Stage (Platform Mandatory) ==="
                        runPlatformStage('Package', PLATFORM_PACKAGE_IMAGE, mavenCache)
                    }
                }
            }
            
            stage('Custom: After Package') {
                when {
                    expression { customStages.any { it.value.after == 'package' } }
                }
                steps {
                    script {
                        def packageCustomStages = createCustomStages('package', customStages, PLATFORM_PACKAGE_IMAGE, mavenCache)
                        packageCustomStages['failFast'] = true
                        parallel packageCustomStages
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
