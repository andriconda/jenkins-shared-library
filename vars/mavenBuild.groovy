#!/usr/bin/env groovy

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
            stage('Checkout') {
                steps {
                    script {
                        echo "Checking out ${gitBranch} from ${gitUrl}"
                        git branch: gitBranch, url: gitUrl
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
            
            stage('Pre-Build Hook') {
                when {
                    expression { 
                        return fileExists('Makefile')
                    }
                }
                steps {
                    script {
                        echo "Checking for pre-build target in Makefile"
                        def result = sh(script: 'make -n pre-build 2>/dev/null', returnStatus: true)
                        if (result == 0) {
                            echo "Running pre-build hook from Makefile"
                            sh 'make pre-build'
                        } else {
                            echo "No pre-build target found in Makefile, skipping"
                        }
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
            
            stage('Post-Build Hook') {
                when {
                    expression { 
                        return fileExists('Makefile')
                    }
                }
                steps {
                    script {
                        echo "Checking for post-build target in Makefile"
                        def result = sh(script: 'make -n post-build 2>/dev/null', returnStatus: true)
                        if (result == 0) {
                            echo "Running post-build hook from Makefile"
                            sh 'make post-build'
                        } else {
                            echo "No post-build target found in Makefile, skipping"
                        }
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
