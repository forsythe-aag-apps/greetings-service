#!/usr/bin/env groovy

@Library('github.com/ForsytheHostingSolutions/jenkins-pipeline-library@master') _

podTemplate(label: 'mypod', containers: [
    containerTemplate(
        name: 'maven', 
        image: 'maven:3.3.9-jdk-8-alpine', 
        envVars: [envVar(key: 'MAVEN_SETTINGS_PATH', value: '/root/.m2/settings.xml')], 
        ttyEnabled: true, 
        command: 'cat'),
    containerTemplate(image: 'docker', name: 'docker', command: 'cat', ttyEnabled: true),
    containerTemplate(name: 'kubectl', image: 'lachlanevenson/k8s-kubectl:v1.8.0', command: 'cat', ttyEnabled: true),
  ], volumes: [
    secretVolume(mountPath: '/root/.m2/', secretName: 'jenkins-maven-settings'),
    secretVolume(mountPath: '/home/jenkins/.docker', secretName: 'regsecret'),
    hostPathVolume(hostPath: '/var/run/docker.sock', mountPath: '/var/run/docker.sock'),
    persistentVolumeClaim(claimName: 'nfs', mountPath: '/root/.m2nrepo')
  ], imagePullSecrets: [ 'regsecret' ]) {

    node('mypod') {
        try {
            checkout scm
            def jobName = "${env.JOB_NAME}".tokenize('/').last() + 'test'
            def projectNamespace = "${env.JOB_NAME}".tokenize('/')[0]
            def ingressAddress = System.getenv("INGRESS_CONTROLLER_IP")
            def accessToken = ""

            withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'github-token', usernameVariable: 'USERNAME', passwordVariable: 'GITHUB_ACCESS_TOKEN']]) {
              accessToken = sh(returnStdout: true, script: 'echo $GITHUB_ACCESS_TOKEN').trim()
            }

            def pullRequest = false
            if (jobName.startsWith("PR-")) {
                pullRequest = true
            }

            if (!pullRequest) {
                container('kubectl') {
                    stage('Configure Kubernetes') {
                        createNamespace(projectNamespace)
                    }
                }
            }

            lock('maven-build') {
                container('maven') {
                    stage('Build a project') {
                        sh 'mvn clean install -DskipTests=true'
                    }

                    stage('Run tests') {
                        try {
                            sh 'mvn clean install test'
                        } finally {
                            junit 'target/surefire-reports/*.xml'
                        }
                    }

                    stage('SonarQube Analysis') {
                        if (!pullRequest) {
                            sonarQubeScanner(accessToken, 'forsythe-aag-apps/greetings-service', "http://sonarqube.${ingressAddress}.xip.io")
                        } else {
                            sonarQubePRScanner(accessToken, 'forsythe-aag-apps/greetings-service', "http://sonarqube.${ingressAddress}.xip.io")
                        }
                    }

                    if (!pullRequest) {
                        stage('Deploy project to Nexus') {
                            sh 'mvn -DskipTests=true package deploy'
                            archiveArtifacts artifacts: 'target/*.jar'
                        }
                    }
                }
            }

            if (!pullRequest) {
                container('docker') {
                    stage('Docker build') {
                        sh 'docker build -t greetings-service .'
                        sh 'docker tag greetings-service quay.io/zotovsa/greetings-service'
                        sh 'docker push quay.io/zotovsa/greetings-service'
                    }
                }

                container('kubectl') {
                    stage('Deploy MicroService') {
                       sh "kubectl delete deployment greetings-service -n ${projectNamespace} --ignore-not-found=true"
                       sh "kubectl delete service greetings-service -n ${projectNamespace} --ignore-not-found=true"
                       sh "kubectl delete -f ./deployment/prometheus-service-monitor.yml -n cicd-tools --ignore-not-found=true"

                       sh "sed -e 's/{{INGRESSIP}}/'${ingressAddress}'/g' ./deployment/ingress.yml > ./deployment/ingress2.yml"
                       sh "kubectl delete -f ./deployment/ingress2.yml -n ${projectNamespace} --ignore-not-found=true"
                       sh "kubectl create -f ./deployment/deployment.yml -n ${projectNamespace}"
                       sh "kubectl create -f ./deployment/service.yml -n ${projectNamespace}"
                       sh "kubectl create -f ./deployment/prometheus-service-monitor.yml -n cicd-tools"
                       sh "kubectl create -f ./deployment/ingress2.yml -n ${projectNamespace}"
                       waitForRunningState(projectNamespace)
                       print "Greetings Service can be accessed at: http://greetings-service.${ingressAddress}.xip.io"
                       rocketSend channel: 'general', message: "@here Greetings Service deployed successfully at http://greetings-service.${ingressAddress}.xip.io", rawMessage: true
                    }
                }

                container('kubectl') {
                    timeout(time: 3, unit: 'MINUTES') {
                        input message: "Deploy to Production?"
                    }
                }

                container('kubectl') {
                   sh "kubectl create namespace prod-${projectNamespace} || true"
                   sh "kubectl delete deployment greetings-service -n prod-${projectNamespace} --ignore-not-found=true"
                   sh "kubectl delete service greetings-service -n prod-${projectNamespace} --ignore-not-found=true"
                   sh "sed -e 's/{{INGRESSIP}}/'${ingressAddress}'/g' ./deployment/prod-ingress.yml > ./deployment/prod-ingress2.yml"
                   sh "kubectl delete -f ./deployment/prod-ingress2.yml -n prod-${projectNamespace} --ignore-not-found=true"
                   sh "kubectl create -f ./deployment/deployment.yml -n prod-${projectNamespace}"
                   sh "kubectl create -f ./deployment/service.yml -n prod-${projectNamespace}"
                   sh "kubectl create -f ./deployment/prod-ingress2.yml -n prod-${projectNamespace}"

                   waitForRunningState("prod-${projectNamespace}")
                   print "Greetings Service can be accessed at: http://prod-greetings-service.${ingressAddress}.xip.io"
                }
            }
        } catch (all) {
            currentBuild.result = 'FAILURE'
            rocketSend channel: 'general', message: "@here Greetings Service build failed", rawMessage: true
        }
    }
}
