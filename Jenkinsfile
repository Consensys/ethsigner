#!/usr/bin/env groovy

import hudson.model.Result
import hudson.model.Run
import jenkins.model.CauseOfInterruption.UserInterruption

if (env.BRANCH_NAME == "master") {
    properties([
        buildDiscarder(
            logRotator(
                daysToKeepStr: '90'
            )
        )
    ])
} else {
    properties([
        buildDiscarder(
            logRotator(
                numToKeepStr: '10'
            )
        )
    ])
}

def docker_image_dind = 'docker:18.06.0-ce-dind'
def docker_image = 'docker:18.06.0-ce'
def build_image = 'pegasyseng/pantheon-build:0.0.5-jdk11'
def registry = 'https://registry.hub.docker.com'
def userAccount = 'dockerhub-pegasysengci'
def imageRepos = 'pegasyseng'
def imageTag = 'develop'

def abortPreviousBuilds() {
    Run previousBuild = currentBuild.rawBuild.getPreviousBuildInProgress()

    while (previousBuild != null) {
        if (previousBuild.isInProgress()) {
            def executor = previousBuild.getExecutor()
            if (executor != null) {
                echo ">> Aborting older build #${previousBuild.number}"
                executor.interrupt(Result.ABORTED, new UserInterruption(
                    "Aborted by newer build #${currentBuild.number}"
                ))
            }
        }

        previousBuild = previousBuild.getPreviousBuildInProgress()
    }
}

if (env.BRANCH_NAME != "master") {
    abortPreviousBuilds()
}

try {
    node {
        checkout scm
        docker.image('docker:18.06.3-ce-dind').withRun('--privileged -v /volumes/jenkins-slave-workspace:/var/jenkins-slave-workspace') { d ->
            docker.image('pegasyseng/pantheon-build:0.0.5-jdk11').inside("-e DOCKER_HOST=tcp://docker:2375 --link ${d.id}:docker") {
                try {
                    stage('Build') {
                        sh './gradlew --no-daemon --parallel build'
                    }
                    stage('Test') {
                        sh './gradlew --no-daemon --parallel test'
                    }
                    stage('Integration Test') {
                        sh './gradlew --no-daemon --parallel integrationTest'
                    }
                    stage('Acceptance Test') {
                        sh './gradlew --no-daemon --parallel acceptanceTest'
                    }
                    stage('Docker') {
                        def image = imageRepos + '/ethsigner:' + imageTag
                        def docker_folder = 'docker'
                        def version_property_file = 'gradle.properties'
                        def reports_folder = docker_folder + '/reports'
                        def dockerfile = docker_folder + '/Dockerfile'

                        sh "docker run --rm -i hadolint/hadolint < ${dockerfile}"
                    }
                } finally {
                    archiveArtifacts '**/build/reports/**'
                    archiveArtifacts '**/build/test-results/**'

                    junit allowEmptyResults: true, testResults: '**/build/test-results/**/*.xml'
                }
            }
        }
    }
} catch (e) {
    currentBuild.result = 'FAILURE'
} finally {
    // If we're on master and it failed, notify slack
    if (env.BRANCH_NAME == "master") {
        def currentResult = currentBuild.result ?: 'SUCCESS'
        def channel = '#priv-pegasys-prod-dev'
        if (currentResult == 'SUCCESS') {
            def previousResult = currentBuild.previousBuild?.result
            if (previousResult != null && (previousResult == 'FAILURE' || previousResult == 'UNSTABLE')) {
                slackSend(
                    color: 'good',
                    message: "EthSigner branch ${env.BRANCH_NAME} build is back to HEALTHY.\nBuild Number: #${env.BUILD_NUMBER}\n${env.BUILD_URL}",
                    channel: channel
                )
            }
        } else if (currentBuild.result == 'FAILURE') {
            slackSend(
                color: 'danger',
                message: "EthSigner branch ${env.BRANCH_NAME} build is FAILING.\nBuild Number: #${env.BUILD_NUMBER}\n${env.BUILD_URL}",
                channel: channel
            )
        } else if (currentBuild.result == 'UNSTABLE') {
            slackSend(
                color: 'warning',
                message: "EthSigner branch ${env.BRANCH_NAME} build is UNSTABLE.\nBuild Number: #${env.BUILD_NUMBER}\n${env.BUILD_URL}",
                channel: channel
            )
        }
    }
}
