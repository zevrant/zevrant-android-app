pipeline {
    agent {
        label 'master'
    }
    stages {
        stage('Launch Build') {
            steps {
                script {
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