
String branchName = (BRANCH_NAME.startsWith('PR-')) ? CHANGE_BRANCH : BRANCH_NAME

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
                                    [$class: 'StringParameterValue', name: 'BRANCH_NAME', value: branchName],
                            ]
                    )
                }
            }
        }
    }
}
