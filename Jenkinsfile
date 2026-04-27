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
    // ─── 색상/이모지/라벨 ───
    def isSuccess = (result == 'SUCCESS')
    def color     = isSuccess ? '#2eb886' : '#e01e5a'  // 슬랙스러운 초록/빨강
    def envLabel  = env.DEPLOY_ENV.toUpperCase()
    def envEmoji  = env.DEPLOY_ENV == 'prod' ? '🚀' : '🧪'
    def resultEmoji = isSuccess ? '🟢' : '🔴'

    // ─── pretext (채널 알림 영역, 가장 위에 큼직하게) ───
    def pretext
    if (isSuccess) {
        pretext = "${resultEmoji}  **${envLabel}** 배포 성공"
    } else {
        // prod 실패는 강조
        if (env.DEPLOY_ENV == 'prod') {
            pretext = "🚨  **${envLabel}** 배포 실패 — 확인 필요"
        } else {
            pretext = "${resultEmoji}  **${envLabel}** 배포 실패"
        }
    }

    // ─── 트리거 정보 (간결하게) ───
    def cause = currentBuild.getBuildCauses()
    def triggerText = '알 수 없음'
    if (cause) {
        def first = cause[0]
        def klass = first._class ?: ''
        if (klass.contains('UserIdCause')) {
            triggerText = "👤 ${first.userName ?: '?'} (수동)"
        } else if (klass.contains('TimerTrigger')) {
            triggerText = '⏰ Cron'
        } else if (klass.contains('GitLab') || (first.shortDescription?.toString()?.contains('GitLab'))) {
            triggerText = "🦊 GitLab webhook"
        } else {
            triggerText = first.shortDescription?.toString() ?: '?'
        }
    }

    // ─── 변경된 서비스 ───
    def changedList = []
    if (env.CHANGED_BACKEND  == 'true') changedList << '⚙️ backend'
    if (env.CHANGED_AI       == 'true') changedList << '🤖 ai'
    if (env.CHANGED_FRONTEND == 'true') changedList << '🎨 frontend'
    def changedText = changedList.isEmpty() ? '_없음_' : changedList.join(' · ')

    // ─── 커밋 정보 ───
    def commitMsgFull = sh(script: 'git log -1 --pretty=%s || true', returnStdout: true).trim()
    def commitMsg     = commitMsgFull.take(80) + (commitMsgFull.length() > 80 ? '...' : '')
    def commitAuthor  = sh(script: 'git log -1 --pretty=%an || true', returnStdout: true).trim()
    def commitSha     = env.GIT_COMMIT_SHORT ?: 'unknown'
    def branch        = env.GIT_BRANCH?.replaceFirst('^origin/', '') ?: env.DEPLOY_ENV

    // ─── 빌드 시간 ───
    def duration  = currentBuild.durationString.replaceFirst(/ and counting$/, '')
    def buildTime = sh(script: "TZ=Asia/Seoul date '+%Y-%m-%d %H:%M'", returnStdout: true).trim()

    // ─── 빌드 URL ───
    def jenkinsUrl = env.BUILD_URL ?: "https://k14e101.p.ssafy.io/jenkins/job/${env.JOB_NAME}/${env.BUILD_NUMBER}/"
    def consoleUrl = "${jenkinsUrl}console"

    // ─── title (헤더) ───
    def title = "${envEmoji} Build #${env.BUILD_NUMBER} — ${branch}"

    // ─── fields (2단 정렬, KV 표) ───
    def fields = [
        [title: '👤 Author',   value: commitAuthor,  short: true],
        [title: '🔀 Trigger',  value: triggerText,   short: true],
        [title: '📦 Changed',  value: changedText,   short: true],
        [title: '⏱  Duration', value: duration,      short: true],
        [title: '#️⃣ Commit',   value: "`${commitSha}`",  short: true],
        [title: '🌿 Branch',   value: "`${branch}`", short: true]
    ]

    // ─── 메시지 본문 ───
    def text = "*${commitMsg}*\n"

    if (isSuccess) {
        if (env.DEPLOY_ENV == 'dev') {
            text += """
🌐 **접속 URL**
- [Frontend](https://k14e101.p.ssafy.io/dev/)
- [Swagger](https://k14e101.p.ssafy.io/dev/api/swagger-ui.html)
- [AI](https://k14e101.p.ssafy.io/dev/ai/)
"""
        } else {
            text += """
🌐 **접속 URL**
- [Frontend](https://k14e101.p.ssafy.io/)
- [AI](https://k14e101.p.ssafy.io/ai/)
"""
        }
    } else {
        // 실패 시 — 마지막 에러 한 줄 추출 시도
        def errorLine = sh(
            script: """
                BUILD_LOG=\$(curl -sS -k '${jenkinsUrl}consoleText' 2>/dev/null || true)
                echo "\$BUILD_LOG" | grep -E 'Caused by|ERROR|FAILURE|placeholder' | tail -3 || echo ''
            """,
            returnStdout: true
        ).trim()

        if (errorLine) {
            // 너무 길면 자름
            errorLine = errorLine.take(400)
            text += "\n🔍 **마지막 에러 라인**\n```\n${errorLine}\n```\n"
        }

        text += "\n[🔗 Console Output 보기](${consoleUrl})"
    }

    // ─── 페이로드 구성 ───
    def payload = [
        username: 'Jenkins Bot',
        icon_url: 'https://www.jenkins.io/images/logos/jenkins/jenkins.png',
        attachments: [[
            fallback: "[${envLabel}] Build #${env.BUILD_NUMBER} ${result}",
            color: color,
            pretext: pretext,
            title: title,
            title_link: jenkinsUrl,
            text: text,
            fields: fields,
            footer: "HappyNurse CI/CD • ${env.JOB_NAME}",
            footer_icon: 'https://www.jenkins.io/images/logos/jenkins/jenkins.png',
            ts: (System.currentTimeMillis() / 1000) as long
        ]]
    ]

    // ─── 전송 ───
    withCredentials([string(credentialsId: 'mattermost-webhook-url', variable: 'MM_URL')]) {
        writeFile file: '.mm-payload.json', text: groovy.json.JsonOutput.toJson(payload)
        sh '''
            curl -sS -X POST -H "Content-Type: application/json" \
                -d @.mm-payload.json \
                "$MM_URL" || echo "[WARN] Mattermost notification failed (non-fatal)"
            rm -f .mm-payload.json
        '''
    }
}


