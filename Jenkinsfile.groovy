pipeline {
    agent {
        label 'master'
    }
    stages {
        stage('Launch Build') {
            steps {
                script {
                    sh 'printenv'
                    build(
                            job: 'Android/Zevrant Android App/zevrant-android-app',
                            propagate: true,
                            wait: true,
                            parameters: [
                                    [$class: 'StringParameterValue', name: 'BRANCHNAME', value: env.BRANCH_NAME],
                            ]
                    )
                }
            }
        }
    }
}