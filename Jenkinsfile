pipeline {
    agent any

    options {
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '20'))
        timeout(time: 30, unit: 'MINUTES')
        disableConcurrentBuilds()
    }

    // triggers {
    //     // 1시간마다 polling (개발 초기용)
    //     cron('H * * * *')
    // }

    environment {
        DEPLOY_ENV = "${env.JOB_NAME == 'prod-deploy' ? 'prod' : 'dev'}"
        BUILD_TIME = sh(script: "TZ=Asia/Seoul date '+%Y-%m-%d %H:%M:%S'", returnStdout: true).trim()
        INFRA_DIR = "/home/deploy/infra"
    }

    stages {
        stage('Init') {
            steps {
                script {
                    // 짧은 커밋 SHA (8자리)
                    env.GIT_COMMIT_SHORT = env.GIT_COMMIT ? env.GIT_COMMIT.substring(0, 8) : 'unknown'

                    echo "============================================"
                    echo " Build #${env.BUILD_NUMBER}"
                    echo " Job Name     : ${env.JOB_NAME}"
                    echo " Deploy Env   : ${env.DEPLOY_ENV}"
                    echo " Build Time   : ${env.BUILD_TIME} (KST)"
                    echo " Commit       : ${env.GIT_COMMIT_SHORT}"
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
                        echo ">>> No previous successful build. Building ALL services."
                        env.CHANGED_BACKEND = 'true'
                        env.CHANGED_AI = 'true'
                        env.CHANGED_FRONTEND = 'true'
                    } else {
                        def changedFiles = sh(
                            script: "git diff --name-only ${prev} ${curr} || true",
                            returnStdout: true
                        ).trim()

                        echo ">>> Changed files since last success:\n${changedFiles ?: '(none)'}"

                        def lines = changedFiles ? changedFiles.split('\n') : []

                        env.CHANGED_BACKEND  = lines.any { it.startsWith('backend/') }      ? 'true' : 'false'
                        env.CHANGED_AI       = lines.any { it.startsWith('ai/') }           ? 'true' : 'false'
                        env.CHANGED_FRONTEND = lines.any { it.startsWith('frontend/web/') } ? 'true' : 'false'

                        // 인프라 관련 파일 변경 시 전부 재빌드
                        def infraChanged = lines.any {
                            it == 'Jenkinsfile' ||
                            it.startsWith('infra/')
                        }
                        if (infraChanged) {
                            echo ">>> Jenkinsfile or infra/ changed. Rebuilding ALL services."
                            env.CHANGED_BACKEND = 'true'
                            env.CHANGED_AI = 'true'
                            env.CHANGED_FRONTEND = 'true'
                        }
                    }

                    echo ">>> Build plan:"
                    echo "    backend  : ${env.CHANGED_BACKEND}"
                    echo "    ai       : ${env.CHANGED_AI}"
                    echo "    frontend : ${env.CHANGED_FRONTEND}"
                }
            }
        }

        stage('Build Images') {
            parallel {
                stage('Build: Backend') {
                    when { environment name: 'CHANGED_BACKEND', value: 'true' }
                    steps {
                        script {
                            def tag     = "${env.DEPLOY_ENV}-${env.GIT_COMMIT_SHORT}"
                            def latest  = "${env.DEPLOY_ENV}-latest"

                            echo ">>> [BACKEND] Building happynurse-backend:${tag}"
                            sh """
                                cd backend
                                docker build \
                                    -t happynurse-backend:${tag} \
                                    -t happynurse-backend:${latest} \
                                    .
                            """
                        }
                    }
                }

                stage('Build: AI') {
                    when { environment name: 'CHANGED_AI', value: 'true' }
                    steps {
                        script {
                            def tag     = "${env.DEPLOY_ENV}-${env.GIT_COMMIT_SHORT}"
                            def latest  = "${env.DEPLOY_ENV}-latest"

                            echo ">>> [AI] Building happynurse-ai:${tag}"
                            sh """
                                cd ai
                                docker build \
                                    -t happynurse-ai:${tag} \
                                    -t happynurse-ai:${latest} \
                                    .
                            """
                        }
                    }
                }

                stage('Build: Frontend') {
                    when { environment name: 'CHANGED_FRONTEND', value: 'true' }
                    steps {
                        script {
                            def tag     = "${env.DEPLOY_ENV}-${env.GIT_COMMIT_SHORT}"
                            def latest  = "${env.DEPLOY_ENV}-latest"
                            // dev는 /dev prefix, prod는 root에서 서비스
                            def basePath = env.DEPLOY_ENV == 'dev' ? '/dev' : ''

                            echo ">>> [FRONTEND] Building happynurse-frontend:${tag} (basePath='${basePath}')"
                            sh """
                                cd frontend/web
                                docker build \\
                                    --build-arg NEXT_PUBLIC_BASE_PATH=${basePath} \\
                                    -t happynurse-frontend:${tag} \\
                                    -t happynurse-frontend:${latest} \\
                                    .
                            """
                        }
                    }
                }
            }
        }

        stage('Deploy') {
            steps {
                script {
                    // 배포 스크립트에 실행권한 부여 (Windows 체크아웃 대비)
                    sh "chmod +x infra/scripts/deploy.sh"

                    // 순차 배포 (Nginx reload 충돌 방지)
                    if (env.CHANGED_BACKEND == 'true') {
                        echo ">>> Deploying backend-${env.DEPLOY_ENV}"
                        sh "./infra/scripts/deploy.sh ${env.DEPLOY_ENV} backend"
                    }
                    if (env.CHANGED_AI == 'true') {
                        echo ">>> Deploying ai-${env.DEPLOY_ENV}"
                        sh "./infra/scripts/deploy.sh ${env.DEPLOY_ENV} ai"
                    }
                    if (env.CHANGED_FRONTEND == 'true') {
                        echo ">>> Deploying frontend-${env.DEPLOY_ENV}"
                        sh "./infra/scripts/deploy.sh ${env.DEPLOY_ENV} frontend"
                    }
                }
            }
        }

        stage('Cleanup') {
            steps {
                script {
                    // 이전 버전 이미지 정리 (디스크 공간 확보)
                    // 같은 서비스/env의 :latest 아닌 태그 중 오래된 것 제거 (최근 3개만 유지)
                    echo ">>> Pruning old images"
                    sh '''
                        for svc in backend ai frontend; do
                            docker images "happynurse-${svc}" --format "{{.Repository}}:{{.Tag}}\\t{{.CreatedAt}}" | \
                                grep -v ":${DEPLOY_ENV}-latest$" | \
                                grep "${DEPLOY_ENV}-" | \
                                sort -rk2 | \
                                awk 'NR>3 {print $1}' | \
                                xargs -r docker rmi -f || true
                        done

                        # dangling 이미지 정리
                        docker image prune -f || true
                    '''
                }
            }
        }
    }

    post {
        success {
            script {
                notifyMattermost('SUCCESS')
            }
            echo "[SUCCESS] Build #${env.BUILD_NUMBER} deployed to ${env.DEPLOY_ENV}"
        }
        failure {
            script {
                notifyMattermost('FAILURE')
            }
            echo "[FAILURE] Build #${env.BUILD_NUMBER} FAILED for ${env.DEPLOY_ENV}"
            echo "Check deploy.sh output above for rollback status."
        }
        always {
            echo "Pipeline finished at ${env.BUILD_TIME}"
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Mattermost 알림 함수
// ──────────────────────────────────────────────────────────────
def notifyMattermost(String result) {
    // 결과별 색상 + 이모지 (Mattermost는 attachment color로 좌측 막대 색깔)
    def color    = (result == 'SUCCESS') ? '#36a64f' : '#d00000'
    def emoji    = (result == 'SUCCESS') ? '🟢'      : '🔴'
    def envLabel = env.DEPLOY_ENV.toUpperCase()

    // 트리거 정보
    def cause = currentBuild.getBuildCauses()
    def triggerText = '알 수 없음'
    if (cause) {
        def first = cause[0]
        if (first._class?.contains('UserIdCause')) {
            triggerText = "수동 빌드 (${first.userName ?: '?'})"
        } else if (first._class?.contains('TimerTrigger')) {
            triggerText = '⏰ Cron (1시간 자동)'
        } else if (first._class?.contains('GitLabWebHookCause') || first.shortDescription?.toString()?.contains('GitLab')) {
            triggerText = "GitLab webhook (${first.shortDescription ?: 'push/MR'})"
        } else {
            triggerText = first.shortDescription?.toString() ?: '?'
        }
    }

    // 변경된 서비스
    def changedServices = []
    if (env.CHANGED_BACKEND  == 'true') changedServices << 'backend'
    if (env.CHANGED_AI       == 'true') changedServices << 'ai'
    if (env.CHANGED_FRONTEND == 'true') changedServices << 'frontend'
    def changedText = changedServices.isEmpty() ? '없음' : changedServices.join(', ')

    // 커밋 정보
    def commitMsg    = sh(script: 'git log -1 --pretty=%B || true', returnStdout: true).trim().take(120)
    def commitAuthor = sh(script: 'git log -1 --pretty=%an || true', returnStdout: true).trim()

    // 빌드 시간
    def duration = currentBuild.durationString.replaceFirst(/ and counting$/, '')

    // 빌드 페이지 링크
    def jenkinsUrl = env.BUILD_URL ?: "https://k14e101.p.ssafy.io/jenkins/job/${env.JOB_NAME}/${env.BUILD_NUMBER}/"

    // 환경별 접속 URL
    def serviceUrls = ''
    if (result == 'SUCCESS') {
        if (env.DEPLOY_ENV == 'dev') {
            serviceUrls = """
*🌐 접속 URL*
- Frontend: https://k14e101.p.ssafy.io/dev/
- Swagger:  https://k14e101.p.ssafy.io/dev/api/swagger-ui.html
- AI:       https://k14e101.p.ssafy.io/dev/ai/
"""
        } else {
            serviceUrls = """
*🌐 접속 URL*
- Frontend: https://k14e101.p.ssafy.io/
- AI:       https://k14e101.p.ssafy.io/ai/
"""
        }
    }

    // 실패 시 콘솔 링크 추가
    def failureFooter = ''
    if (result == 'FAILURE') {
        failureFooter = "\n\n*🔍 실패 분석*: <${jenkinsUrl}console|Console Output에서 확인>"
    }

    // 메시지 본문
    def text = """
${emoji} **[${envLabel}] Build #${env.BUILD_NUMBER} ${result}**

*Commit*:    `${env.GIT_COMMIT_SHORT}` — ${commitMsg}
*Author*:    ${commitAuthor}
*Triggered*: ${triggerText}
*Changed*:   ${changedText}
*Duration*:  ${duration}
${serviceUrls}${failureFooter}

[🔗 Build Details](${jenkinsUrl})
"""

    // Mattermost로 전송
    withCredentials([string(credentialsId: 'mattermost-webhook-url', variable: 'MM_URL')]) {
        // JSON escape를 위해 텍스트를 파일에 쓰고 curl로 보냄
        writeFile file: '.mm-payload.json', text: groovy.json.JsonOutput.toJson([
            username: 'jenkins-bot',
            attachments: [[
                color: color,
                text: text
            ]]
        ])
        sh '''
            curl -sS -X POST -H "Content-Type: application/json" \
                -d @.mm-payload.json \
                "$MM_URL" || echo "[WARN] Mattermost notification failed (non-fatal)"
            rm -f .mm-payload.json
        '''
    }
}


