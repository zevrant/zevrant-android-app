pipeline {
    agent {
        label 'master'
    }
    stages {
        stage('Launch Build') {
            steps {
                script {
                    String repoName = env.JOB_NAME.split("/")[1].toLowerCase().replaceAll("\\h", "-")
                    build(
                            job: 'Android/Zevrant Android App/zevrant-android-app',
                            propagate: true,
                            wait: true,
                            parameters: [
                                    [$class: 'StringParameterValue', name: 'BRANCHNAME', value: env.BRANCH_NAME],
                                    [$class: 'StringParameterValue', name: 'REPOSITORY', value: repoName]
                            ]
                    )
                }
            }
        }
    }
}