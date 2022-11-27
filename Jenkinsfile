def image_tag = ""

pipeline {
    agent any
    environment {
       CI = 'false'
       DISCORD_WEBHOOK = credentials('discord_webhook')
    }
    
    stages {
        stage('Discord notify'){
            when {
                anyOf{
                    branch 'master'
                    branch 'development'
                    branch pattern: "PR-\\d+", comparator: "REGEXP"
                }
            }
            agent any
            steps {
                discordSend (
                    description: "Job started", 
                    footer: "ETA ~10min", 
                    link: env.BUILD_URL, 
                    result: "UNSTABLE", // so we get yellow color in discord, 
                    title: JOB_NAME, 
                    webhookURL: DISCORD_WEBHOOK
                )
            }
            post{
                unsuccessful {
                    discordSend (
                        description: "Hey ${env.CHANGE_AUTHOR}, job is not successful on branch ${env.GIT_BRANCH}", 
                        footer: currentBuild.currentResult, 
                        link: env.BUILD_URL, 
                        result: currentBuild.currentResult, 
                        title: JOB_NAME, 
                        webhookURL: DISCORD_WEBHOOK
                    )
                }
            }
        }
        stage('Run tests') {
            when {
                anyOf{
                    branch 'master'
                    branch 'development'
                    branch pattern: "PR-\\d+", comparator: "REGEXP"
                }
            }
            agent {
                // Equivalent to "docker build -f Dockerfile.build --build-arg version=1.0.2 ./build/
                dockerfile {
                    filename './docker/Dockerfile.test'
                    //dir 'build'
                    args '-v /root/.m2:/root/.m2'
                }
            }
            steps {
                sh 'mvn -f jenkins_pom.xml test'
            }
            post{
                unsuccessful {
                    discordSend (
                        description: "Hey ${env.CHANGE_AUTHOR}, job is not successful on branch ${env.GIT_BRANCH}", 
                        footer: currentBuild.currentResult, 
                        link: env.BUILD_URL, 
                        result: currentBuild.currentResult, 
                        title: JOB_NAME, 
                        webhookURL: DISCORD_WEBHOOK
                    )
                }
            }
        }
        stage('Docker build PR') {
            when {
                anyOf{
                    branch pattern: "PR-\\d+", comparator: "REGEXP"
                }
            }
            agent any
            environment {
		        DOCKERHUB_CREDENTIALS = credentials('dockerhub_access')
	        }
            steps {
                sh "docker build --file ./docker/Dockerfile.deploy --tag codebenders/codedefenders:${env.GIT_COMMIT} ."
                sh "docker push codebenders/codedefenders:${env.GIT_COMMIT}"
                /*script{
                    image_tag = "${env.GIT_COMMIT}"
                }*/
            }
            post{
                success{
                    discordSend (
                        description: "Hey ${env.CHANGE_AUTHOR}, job is successful on branch ${env.GIT_BRANCH} :D", 
                        footer: "Your image: codebenders/codedefenders:${env.GIT_COMMIT}", 
                        link: env.BUILD_URL, 
                        result: currentBuild.currentResult, 
                        title: JOB_NAME, 
                        webhookURL: DISCORD_WEBHOOK
                    )
                }
                unsuccessful {
                    discordSend (
                        description: "Hey ${env.CHANGE_AUTHOR}, job is not successful on branch ${env.GIT_BRANCH} :(", 
                        footer: currentBuild.currentResult, 
                        link: env.BUILD_URL, 
                        result: currentBuild.currentResult, 
                        title: JOB_NAME, 
                        webhookURL: DISCORD_WEBHOOK
                    )
                }
            }
        }
        stage('Docker build dev') {
            when {
                anyOf{
                    branch 'development'
                }
            }
            agent any
            environment {
		        DOCKERHUB_CREDENTIALS = credentials('dockerhub_access')
	        }
            steps {
                sh "docker build --file ./docker/Dockerfile.deploy --tag codebenders/codedefenders:${env.GIT_COMMIT} ."
                sh "docker tag codebenders/codedefenders:${env.GIT_COMMIT} codebenders/codedefenders:dev"
                
                sh "docker push codebenders/codedefenders:${env.GIT_COMMIT}"
                sh "docker push codebenders/codedefenders:dev"
                /*script{
                    image_tag = "${env.GIT_COMMIT}"
                }*/
            }
            post{
                success{
                    discordSend (
                        description: "Hey team, job is successful on branch ${env.GIT_BRANCH} :D", 
                        footer: "New development image: codebenders/codedefenders:dev, also codebenders/codedefenders:${env.GIT_COMMIT}", 
                        link: env.BUILD_URL, 
                        result: currentBuild.currentResult, 
                        title: JOB_NAME, 
                        webhookURL: DISCORD_WEBHOOK
                    )
                }
                unsuccessful {
                    discordSend (
                        description: "Hey team, job is not successful on branch ${env.GIT_BRANCH} :(", 
                        footer: currentBuild.currentResult, 
                        link: env.BUILD_URL, 
                        result: currentBuild.currentResult, 
                        title: JOB_NAME, 
                        webhookURL: DISCORD_WEBHOOK
                    )
                }
            }
        }
        stage('Docker build release'){
            when {
                anyOf{
                    branch 'master'
                }
            }
            agent any
            environment {
		        DOCKERHUB_CREDENTIALS = credentials('dockerhub_access')
	        }
            steps {
                sh "docker build --file ./docker/Dockerfile.deploy --tag codebenders/codedefenders:${env.GIT_COMMIT} ."
                sh "docker tag codebenders/codedefenders:${env.GIT_COMMIT} codebenders/codedefenders:latest"

                sh "docker push codebenders/codedefenders:${env.GIT_COMMIT}"
                sh 'docker push codebenders/codedefenders:latest'
            }
            post{
                success {
                    discordSend (
                        description: "Hey team, job is successful on branch ${env.GIT_BRANCH} :D", 
                        footer: "Latest release image: codebenders/codedefenders:latest, also codebenders/codedefenders:${env.GIT_COMMIT}", 
                        link: env.BUILD_URL, 
                        result: currentBuild.currentResult, 
                        title: JOB_NAME, 
                        webhookURL: DISCORD_WEBHOOK
                    )
                }
                unsuccessful {
                    discordSend (
                        description: "Hey team, job is not successful on branch ${env.GIT_BRANCH} :(", 
                        footer: currentBuild.currentResult, 
                        link: env.BUILD_URL, 
                        result: currentBuild.currentResult, 
                        title: JOB_NAME, 
                        webhookURL: DISCORD_WEBHOOK
                    )
                }
            }
        }
    }
}
