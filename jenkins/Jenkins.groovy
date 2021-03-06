pipeline {

    environment {
        NEXUS_CREDENTIAL = "nexus"
        NEXUS_DOCKER_REGISTRY_URL = "localhost:7777"
        APP_NAME = "api-with-node"
    }

    agent any

    tools { nodejs "node" }

    options {
        ansiColor('xterm')
    }

    stages {

        stage("run pipeline steps") {
            steps {
                script {
                    infoString("Running Pipeline Steps")
                    runPipelineSteps()
                }
            }

        }


    }


    post {
        always {
            echo infoString("Done")
            cleanWs()
        }

        success {
            echo successString("Success")
        }

        failure {
            echo failureString("Failure")
        }
    }
}


def runPipelineSteps() {
    stage("Checkout SCM") {
            checkout(scm)
    }


    stage("build NodeJs") {
            echo infoString("Building NodeJS App")
            buildNodesJsApp()
    }

    stage("unit testing") {

        echo infoString("Testing NodeJS App")
        try {
            sh '''#!/bin/bash
                    set -x
                    npm start &
                    sleep 1
                    echo $! > .pidfile
                    set +x 
                    '''
            sh "npm test"
        } catch (Exception e) {
            throw e.getMessage()
        } finally {
            sh '''#!/bin/bash
                    set -x
                    kill $(cat .pidfile)
                    set +x
                    '''
        }

    }

    stage("build Docker image") {
        appVersion = getAppVersion()
        echo infoString("Building Docker Image")
        dockerImage = docker.build "${APP_NAME}:${appVersion}"

        docker.withRegistry("https://${NEXUS_DOCKER_REGISTRY_URL}", NEXUS_CREDENTIAL) {
            dockerImage.push("latest")
        }
    }
}

String getAppVersion() {
    def packageJson = readJSON file: 'package.json'
    appVersion = "${packageJson.version}"

    return appVersion
}


def buildNodesJsApp() {
    sh 'npm install'
    sh "chmod +x -R ${env.WORKSPACE}"
}

String infoString(String message) {
    return "\033[42m ${message} \033[0m"
}

String successString(String message) {
    return "\033[42m ${message} \033[0m"
}

String failureString(String message) {
    return "\033[41m ${message} \033[0m"
}