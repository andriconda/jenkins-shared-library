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
    
    if (scriptFile) {
        // Run script file from app repo
        docker.image(container).inside("-v ${cacheVolume}:/root/.m2") {
            sh "chmod +x ${scriptFile}"
            sh "./${scriptFile}"
        }
    } else if (script) {
        // Run inline script
        docker.image(container).inside("-v ${cacheVolume}:/root/.m2") {
            sh script
        }
    } else {
        echo "WARNING: Custom stage '${stageName}' has no script or scriptFile defined"
    }
}

// Helper to execute custom stages after a specific stage
def executeCustomStages(String afterStage, Map customStages, String defaultImage, String cacheVolume) {
    customStages.findAll { it.value.after == afterStage }.each { stageName, stageConfig ->
        stage(stageName) {
            steps {
                script {
                    echo "=== Custom Stage: ${stageName} (App) ==="
                    runCustomStage(stageName, stageConfig, defaultImage, cacheVolume)
                }
            }
        }
    }
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
        
        stages {
            stage('Setup') {
                steps {
                    script {
                        echo "=== Container-Based Pipeline Setup ==="
                        echo "All stages run in containers - no tools needed on Jenkins!"
                        
                        // Checkout application code
                        echo "Checking out ${gitBranch} from ${gitUrl}"
                        git branch: gitBranch, url: gitUrl
                    }
                }
            }
            
            stage('Build') {
                steps {
                    script {
                        echo "=== Build Stage (Platform Mandatory) ==="
                        runPlatformStage('Build', PLATFORM_BUILD_IMAGE, mavenCache)
                        
                        // Execute custom stages after Build (runtime decision)
                        executeCustomStages('build', customStages, PLATFORM_BUILD_IMAGE, mavenCache)
                    }
                }
            }
            
            stage('Test') {
                steps {
                    script {
                        echo "=== Test Stage (Platform Mandatory) ==="
                        runPlatformStage('Test', PLATFORM_TEST_IMAGE, mavenCache)
                        
                        // Execute custom stages after Test (runtime decision)
                        executeCustomStages('test', customStages, PLATFORM_TEST_IMAGE, mavenCache)
                    }
                }
            }
            
            stage('Security Scan') {
                steps {
                    script {
                        echo "=== Security Scan Stage (Platform Mandatory) ==="
                        runPlatformStage('Security', PLATFORM_SECURITY_IMAGE, mavenCache)
                        
                        // Execute custom stages after Security (runtime decision)
                        executeCustomStages('security', customStages, PLATFORM_SECURITY_IMAGE, mavenCache)
                    }
                }
            }
            
            stage('Package') {
                steps {
                    script {
                        echo "=== Package Stage (Platform Mandatory) ==="
                        runPlatformStage('Package', PLATFORM_PACKAGE_IMAGE, mavenCache)
                        
                        // Execute custom stages after Package (runtime decision)
                        executeCustomStages('package', customStages, PLATFORM_PACKAGE_IMAGE, mavenCache)
                    }
                }
            }
            
            stage('Docker Build') {
                when {
                    expression { fileExists('Dockerfile') }
                }
                steps {
                    script {
                        echo "=== Docker Build Stage ==="
                        docker.image(PLATFORM_DOCKER_IMAGE).inside("-v /var/run/docker.sock:/var/run/docker.sock") {
                            sh '''
                                echo "Building Docker image..."
                                docker build -t ${JOB_NAME}:${BUILD_NUMBER} .
                                docker tag ${JOB_NAME}:${BUILD_NUMBER} ${JOB_NAME}:latest
                                echo "Docker image built: ${JOB_NAME}:${BUILD_NUMBER}"
                            '''
                        }
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
