pipeline {
    agent any

    options {
        timestamps()                          // 로그 줄마다 시간 표시
        buildDiscarder(logRotator(numToKeepStr: '20'))  // 빌드 기록 20개만 유지
        timeout(time: 30, unit: 'MINUTES')    // 30분 넘으면 강제 종료
        disableConcurrentBuilds()             // 같은 잡 동시 실행 방지
    }

    triggers {
        // 1시간마다 polling 기반 빌드 (개발 초기용)
        // H = Hash. 정확히 매시 0분이 아니라, 잡마다 분산된 시각에 실행됨 → 동시 부하 방지
        cron('H * * * *')
    }

    environment {
        // 잡 이름으로 환경 분기 (dev-deploy → dev, prod-deploy → prod)
        DEPLOY_ENV = "${env.JOB_NAME == 'prod-deploy' ? 'prod' : 'dev'}"
        // 빌드 시작 시각 (한국 시간)
        BUILD_TIME = sh(script: "TZ=Asia/Seoul date '+%Y-%m-%d %H:%M:%S'", returnStdout: true).trim()
    }

    stages {
        stage('Init') {
            steps {
                script {
                    echo "============================================"
                    echo " Build #${env.BUILD_NUMBER}"
                    echo " Branch       : ${env.BRANCH_NAME ?: 'N/A'}"
                    echo " Deploy Env   : ${env.DEPLOY_ENV}"
                    echo " Build Time   : ${env.BUILD_TIME} (KST)"
                    echo " Commit       : ${env.GIT_COMMIT ?: 'unknown'}"
                    echo " Prev Success : ${env.GIT_PREVIOUS_SUCCESSFUL_COMMIT ?: 'none (first build)'}"
                    echo "============================================"
                }
            }
        }

        stage('Detect Changes') {
            steps {
                script {
                    def prev = env.GIT_PREVIOUS_SUCCESSFUL_COMMIT
                    def curr = env.GIT_COMMIT

                    if (!prev) {
                        // 첫 빌드 또는 이전 성공 빌드 없음 → 전부 빌드
                        echo "▶ No previous successful build. Building ALL services."
                        env.CHANGED_BACKEND = 'true'
                        env.CHANGED_AI = 'true'
                        env.CHANGED_FRONTEND = 'true'
                    } else {
                        // 이전 성공 빌드와 현재 사이의 변경 파일 목록
                        def changedFiles = sh(
                            script: "git diff --name-only ${prev} ${curr} || true",
                            returnStdout: true
                        ).trim()

                        echo "▶ Changed files since last success:\n${changedFiles ?: '(none)'}"

                        env.CHANGED_BACKEND  = changedFiles.split('\n').any { it.startsWith('backend/') }  ? 'true' : 'false'
                        env.CHANGED_AI       = changedFiles.split('\n').any { it.startsWith('ai/') }       ? 'true' : 'false'
                        env.CHANGED_FRONTEND = changedFiles.split('\n').any { it.startsWith('frontend/') } ? 'true' : 'false'

                        // 인프라 파일(Jenkinsfile, infra/) 변경 시 → 안전하게 전부 빌드
                        def infraChanged = changedFiles.split('\n').any { it == 'Jenkinsfile' || it.startsWith('infra/') }
                        if (infraChanged) {
                            echo "▶ Jenkinsfile or infra/ changed. Rebuilding ALL services."
                            env.CHANGED_BACKEND = 'true'
                            env.CHANGED_AI = 'true'
                            env.CHANGED_FRONTEND = 'true'
                        }
                    }

                    echo "▶ Build plan:"
                    echo "   backend  : ${env.CHANGED_BACKEND}"
                    echo "   ai       : ${env.CHANGED_AI}"
                    echo "   frontend : ${env.CHANGED_FRONTEND}"
                }
            }
        }

        stage('Build & Deploy Services') {
            parallel {
                stage('Backend (Spring Boot)') {
                    when { environment name: 'CHANGED_BACKEND', value: 'true' }
                    steps {
                        echo "🔧 [BACKEND] Build & Deploy to ${env.DEPLOY_ENV}"
                        echo "   (Step 7-8에서 실제 docker build / blue-green 배포로 교체 예정)"
                        sh 'ls -la backend/ || echo "backend/ folder not found"'
                    }
                }

                stage('AI (FastAPI)') {
                    when { environment name: 'CHANGED_AI', value: 'true' }
                    steps {
                        echo "🤖 [AI] Build & Deploy to ${env.DEPLOY_ENV}"
                        echo "   (Step 7-8에서 실제 docker build / blue-green 배포로 교체 예정)"
                        sh 'ls -la ai/ || echo "ai/ folder not found"'
                    }
                }

                stage('Frontend (Next.js)') {
                    when { environment name: 'CHANGED_FRONTEND', value: 'true' }
                    steps {
                        echo "🌐 [FRONTEND] Build & Deploy to ${env.DEPLOY_ENV}"
                        echo "   (Step 7-8에서 실제 docker build / blue-green 배포로 교체 예정)"
                        sh 'ls -la frontend/ || echo "frontend/ folder not found"'
                    }
                }
            }
        }
    }

    post {
        success {
            echo "✅ Build #${env.BUILD_NUMBER} succeeded for ${env.DEPLOY_ENV}"
        }
        failure {
            echo "❌ Build #${env.BUILD_NUMBER} FAILED for ${env.DEPLOY_ENV}"
        }
        always {
            echo "Pipeline finished at ${env.BUILD_TIME}"
        }
    }
}