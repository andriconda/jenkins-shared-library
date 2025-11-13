#!/usr/bin/env groovy

// Helper function to run Makefile hooks
def runMakefileHook(String hookName, boolean makeAvailable) {
    // Check if the target exists in Makefile
    def hasTarget = sh(
        script: "grep -q '^${hookName}:' Makefile",
        returnStatus: true
    ) == 0
    
    if (hasTarget) {
        echo "Running ${hookName} hook from Makefile"
        sh "make ${hookName}"
    }
}

def call(Map config = [:]) {
    def gitUrl = config.gitUrl ?: ''
    def gitBranch = config.gitBranch ?: 'main'
    def mavenGoals = config.mavenGoals ?: 'clean package'
    def skipTests = config.skipTests ?: true
    def mavenTool = config.mavenTool ?: 'Maven'
    def cleanCache = config.cleanCache ?: true
    def customStages = config.customStages ?: [:] // Map of stage name to make target
    
    pipeline {
        agent any
        
        tools {
            maven "${mavenTool}"
        }
        
        stages {
            stage('Setup Hooks') {
                steps {
                    script {
                        // Check once if Makefile exists and make is available
                        env.MAKEFILE_EXISTS = fileExists('Makefile') ? 'true' : 'false'
                        
                        if (env.MAKEFILE_EXISTS == 'true') {
                            env.MAKE_AVAILABLE = sh(
                                script: 'command -v make >/dev/null 2>&1',
                                returnStatus: true
                            ) == 0 ? 'true' : 'false'
                            
                            if (env.MAKE_AVAILABLE == 'false') {
                                echo "WARNING: 'make' command not found. Makefile hooks will be skipped."
                                echo "To use Makefile hooks, install make in your Jenkins environment."
                            } else {
                                echo "Makefile detected. Hooks will be executed if targets exist."
                            }
                        }
                    }
                }
            }
            
            stage('Before Checkout') {
                when {
                    expression { env.MAKEFILE_EXISTS == 'true' && env.MAKE_AVAILABLE == 'true' }
                }
                steps {
                    script {
                        runMakefileHook('before-checkout', true)
                    }
                }
            }
            
            stage('Checkout') {
                steps {
                    script {
                        echo "Checking out ${gitBranch} from ${gitUrl}"
                        git branch: gitBranch, url: gitUrl
                    }
                }
            }
            
            stage('After Checkout') {
                when {
                    expression { env.MAKEFILE_EXISTS == 'true' && env.MAKE_AVAILABLE == 'true' }
                }
                steps {
                    script {
                        runMakefileHook('after-checkout', true)
                    }
                }
            }
            
            stage('Before Clean Cache') {
                when {
                    expression { cleanCache == true && env.MAKEFILE_EXISTS == 'true' && env.MAKE_AVAILABLE == 'true' }
                }
                steps {
                    script {
                        runMakefileHook('before-clean', true)
                    }
                }
            }
            
            stage('Clean Cache') {
                when {
                    expression { cleanCache == true }
                }
                steps {
                    script {
                        echo "Cleaning build artifacts and Maven cache"
                        sh '''
                            # Remove target directory
                            if [ -d "target" ]; then
                                echo "Removing target directory..."
                                rm -rf target
                            fi
                            
                            # Clean Maven local repository cache for this project
                            if [ -d "$HOME/.m2/repository" ]; then
                                echo "Cleaning Maven cache..."
                                # This removes cached artifacts to force fresh downloads
                                mvn dependency:purge-local-repository -DreResolve=false || true
                            fi
                        '''
                    }
                }
            }
            
            stage('After Clean Cache') {
                when {
                    expression { cleanCache == true && env.MAKEFILE_EXISTS == 'true' && env.MAKE_AVAILABLE == 'true' }
                }
                steps {
                    script {
                        runMakefileHook('after-clean', true)
                    }
                }
            }
            
            stage('Before Build') {
                when {
                    expression { env.MAKEFILE_EXISTS == 'true' && env.MAKE_AVAILABLE == 'true' }
                }
                steps {
                    script {
                        runMakefileHook('before-build', true)
                    }
                }
            }
            
            stage('Build') {
                steps {
                    script {
                        def mvnCommand = "mvn -B"
                        if (skipTests) {
                            mvnCommand += " -DskipTests"
                        }
                        mvnCommand += " ${mavenGoals}"
                        
                        echo "Executing: ${mvnCommand}"
                        sh mvnCommand
                    }
                }
            }
            
            stage('After Build') {
                when {
                    expression { env.MAKEFILE_EXISTS == 'true' && env.MAKE_AVAILABLE == 'true' }
                }
                steps {
                    script {
                        runMakefileHook('after-build', true)
                    }
                }
            }
            
            stage('Before Archive') {
                when {
                    expression { env.MAKEFILE_EXISTS == 'true' && env.MAKE_AVAILABLE == 'true' }
                }
                steps {
                    script {
                        runMakefileHook('before-archive', true)
                    }
                }
            }
            
            stage('Archive') {
                steps {
                    script {
                        echo "Archiving artifacts"
                        archiveArtifacts artifacts: '**/target/*.jar', allowEmptyArchive: true
                    }
                }
            }
            
            stage('After Archive') {
                when {
                    expression { env.MAKEFILE_EXISTS == 'true' && env.MAKE_AVAILABLE == 'true' }
                }
                steps {
                    script {
                        runMakefileHook('after-archive', true)
                    }
                }
            }
        }
        
        post {
            success {
                echo "Build completed successfully!"
            }
            failure {
                echo "Build failed!"
            }
            always {
                cleanWs()
            }
        }
    }
}
