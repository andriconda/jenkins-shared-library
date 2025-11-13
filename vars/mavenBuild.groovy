#!/usr/bin/env groovy

def call(Map config = [:]) {
    def gitUrl = config.gitUrl ?: ''
    def gitBranch = config.gitBranch ?: 'main'
    def mavenGoals = config.mavenGoals ?: 'clean package'
    def skipTests = config.skipTests ?: true
    def mavenTool = config.mavenTool ?: 'Maven'
    def cleanCache = config.cleanCache ?: true
    
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
