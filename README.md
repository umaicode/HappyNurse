<div align="center">

<!-- 로고/배너 이미지 영역 -->
<!-- 여기에 프로젝트 로고 또는 배너 이미지를 넣어주세요 -->
<img src="./image/banner/해피너스_최종배너.png" alt="HappyNurse 로고" width="500"/>

# HappyNurse

### 🏆 SSAFY 자율 프로젝트 우수상 수상

### 기록은 HappyNurse가, 케어는 간호사가

### 간호사를 위한 업무 자동화 AI 에이전트 비서

<br/>

![Kotlin](https://img.shields.io/badge/Kotlin-2.2.20-7F52FF?logo=kotlin&logoColor=white)
![Next.js](https://img.shields.io/badge/Next.js-16-000000?logo=nextdotjs&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3-6DB33F?logo=springboot&logoColor=white)
![Java](https://img.shields.io/badge/Java-17-007396?logo=openjdk&logoColor=white)
![FastAPI](https://img.shields.io/badge/FastAPI-Python%203.12-009688?logo=fastapi&logoColor=white)

![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-4169E1?logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?logo=redis&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?logo=docker&logoColor=white)
![Jenkins](https://img.shields.io/badge/Jenkins-D24939?logo=jenkins&logoColor=white)
![Nginx](https://img.shields.io/badge/Nginx-009639?logo=nginx&logoColor=white)

<br/>

#### SSAFY 자율 프로젝트 · 6인 팀 · 8주 진행

#### 개발 기간 : 2026.04.14 ~ 2026.05.22

#### 플랫폼: Web & Android App & Watch App

</div>

<br/>

## 📑 목차

<div align="center">

<a href="#1-프로젝트-소개">🏥 프로젝트 소개</a> &nbsp;·&nbsp;
<a href="#2-기획-배경">💡 기획 배경</a> &nbsp;·&nbsp;
<a href="#3-주요-기능">✨ 주요 기능</a> &nbsp;·&nbsp;
<a href="#4-팀원-소개">👥 팀원 소개</a> &nbsp;·&nbsp;
<a href="#5-기술-스택">🛠 기술 스택</a>
<br/>
<a href="#6-시스템-아키텍처">🏗 시스템 아키텍처</a> &nbsp;·&nbsp;
<a href="#7-erd">🗄 ERD</a> &nbsp;·&nbsp;
<a href="#8-프로젝트-구조">📂 프로젝트 구조</a> &nbsp;·&nbsp;
<a href="#9-영상-포트폴리오">🎬 영상 포트폴리오</a>

</div>

<br/>

<a id="1-프로젝트-소개"></a>

## 🏥 1. 프로젝트 소개

**HappyNurse(해피너스)**는 병동 간호사의 반복적인 기록 업무를 자동화하여, 간호사가 본질적인 환자 케어에 집중할 수 있도록 돕는 **AI 업무 자동화 비서**입니다.

NFC 태깅으로 환자와 약물을 즉시 인식하고, 음성(STT)으로 간호 기록을 작성하며, AI가 인수인계 내용을 자동 요약합니다. 모바일 앱·스마트워치·웹 관리 콘솔이 유기적으로 연동되어, 침상에서의 실시간 기록부터 데스크탑에서의 검토·확정까지 하나의 흐름으로 이어집니다.

<!-- 프로젝트 대표 GIF 또는 메인 화면 이미지 영역 -->
<!-- 여기에 프로젝트 전체를 보여주는 대표 GIF를 넣어주세요 -->
<div align="center">
<img src="./image/슬라이드25.PNG"  height="420" />
</div>

<br/>

<a id="2-기획-배경"></a>

## 💡 2. 기획 배경

간호사는 환자 케어보다 기록·행정 업무에 많은 시간을 소모합니다. 투약 기록, 간호일지 작성, 인수인계 정리 등 반복적인 문서 작업은 간호사의 피로도를 높이고 환자와의 접점을 줄입니다.

HappyNurse는 다음 문제를 해결하고자 합니다.

- **기록 부담 경감** - 음성 한 번으로 간호일지를 작성하고, AI가 의료 용어를 교정합니다.
- **투약 안전성 강화** - NFC 환자 팔찌·약물 태그를 검증하여 오투약을 방지합니다.
- **인수인계 누락 방지** - AI가 근무 중 발생한 특이사항을 자동 요약해 인수인계 품질을 높입니다.
- **실시간 알림** - 수액 잔량, 환자 호출, 처방 변경 등을 워치·앱으로 즉시 전달합니다.

<br/>

<a id="3-주요-기능"></a>

## ✨ 3. 주요 기능

### 환자 정보 조회 & 의사 오더 확인

<p align="left">병동·호수별 환자 리스트를 조회하고, 환자별 간호일지와 의사 오더(투약·수액·처치·검사·영상)를 타임라인으로 확인합니다.</p>

<!-- 기능 시연 GIF 영역 -->
<div align="center">

<table>
  <tr>
    <th>Web</th>
    <th>App</th>
  </tr>
  <tr>
    <td align="center"><img src="./image/간호일지.png" width="640"/></td>
    <td align="center"><img src="./image/앱간호일지.gif" width="150"/></td>
  </tr>
</table>

</div>

<br/>

### AI 인수인계 자동 요약

<p align="left">근무 중 발생한 V/S 이상, 미완료 업무, 신규 처방 등을 AI가 자동으로 요약하여 인수인계 카드로 제공합니다. 주의사항과 인수자 체크리스트를 함께 정리합니다.</p>

<!-- 기능 시연 GIF 영역 -->
<div align="center">

<table>
  <tr>
    <th>Web</th>
    <th>App</th>
  </tr>
  <tr>
    <td align="center"><img src="./image/인수인계.png" width="750"/></td>
    <td align="center"><img src="./image/앱인수인계.gif" width="180"/></td>
  </tr>
</table>

</div>

<br/>

### 음성(STT) 간호일지 자동화 (App)

<p align="left">앱에서 마이크 버튼으로 간호 기록을 음성으로 녹음하면 CLOVA Speech STT가 텍스트로 변환합니다. 형태소 분석(Kiwi)과 의료 용어 교정 사전이 오인식된 용어를 자동 보정하며, 변환된 기록은 '확정 전' 상태로 저장되어 웹에서 검토·확정합니다.</p>

<!-- 기능 시연 GIF 영역 -->
<div align="center">

<table>
  <tr>
    <th>음성 녹음 · STT 변환</th>
    <th>간호일지에 확정 전으로 등록</th>
  </tr>
  <tr>
    <td align="center"><img src="./image/간호일지stt.gif" width="260"/></td>
    <td align="center"><img src="./image/stt_app.jpg" width="260"/></td>
  </tr>
</table>

</div>

<br/>

### 음성(STT) 타이머 설정 (Watch)

<p align="left">양손이 자유롭지 못한 간호 업무 중에도 손목 제스처(더블 스냅)로 녹음을 시작해 음성으로 타이머를 설정합니다. 'N분 뒤', '오전/오후 H시 M분' 등 시간 표현을 파싱해 예약 시각을 인식하고, 사용자가 확인·조정한 뒤 알림을 등록합니다. 설정한 시각이 되면 워치 알림과 TTS 음성으로 안내합니다.</p>

<!-- 기능 시연 GIF 영역 -->
<div align="center">

<table>
  <tr>
    <th>손목 제스처</th>
    <th>음성 타이머 설정</th>
    <th>타이머 알림</th>
  </tr>
  <tr>
    <td align="center"><img src="./image/워치제스처.gif" width="220"/></td>
    <td align="center"><img src="./image/워치stt_원형.png" width="220"/></td>
    <td align="center"><img src="./image/stt_watch.png" width="220"/></td>
  </tr>
</table>

</div>

<br/>

### NFC 기반 환자·약물 인식

<p align="left">간호사 앱에서 환자 팔찌를 NFC로 태깅해 환자 신원을 즉시 확인하고, 약물 NFC 태그로 투약 내역을 검증·기록합니다.</p>

<!-- 기능 시연 GIF 영역 -->
<div align="center">

<table>
  <tr>
    <th>간호사 앱 환자 확인</th>
    <th>약물 NFC 태깅 확인</th>
  </tr>
  <tr>
    <td align="center"><img src="./image/환자팔찌태깅앱.jpg" width="260"/></td>
    <td align="center"><img src="./image/약물태깅.gif" width="260"/></td>
  </tr>
</table>

</div>

<br/>

### 환자 웹앱 — 증상 요청 & AI 긴급도 분류

<p align="left">환자가 자신의 NFC 팔찌를 태깅하면 환자 전용 웹앱이 열립니다. 이름·생년월일로 본인 인증 후 통증·화장실·드레싱·수액·체위 변경·호흡 불편 등 빠른 증상 버튼이나 음성·텍스트로 간호사에게 도움을 요청합니다. AI가 환자의 진료과·수술·진단·재원 경과(POD) 맥락과 증상을 함께 분석해 긴급도(긴급·높음·보통·낮음)를 분류하고, '긴급' 요청은 담당 간호사의 앱·워치로 즉시 알림을 전달합니다.</p>

<!-- 기능 시연 GIF 영역 -->
<div align="center">

<table>
  <tr>
    <th>환자 NFC → 웹앱 진입</th>
    <th>증상 · 도움 요청</th>
    <th>긴급 알림 (Watch)</th>
  </tr>
  <tr>
    <td align="center"><img src="./image/환자팔찌.gif" width="240"/></td>
    <td align="center"><img src="./image/환자도움요청.gif" width="200"/></td>
    <td align="center"><img src="./image/긴급알림_원형.png" width="220"/></td>
  </tr>
</table>

</div>

<br/>

### 수액 타이머

<p align="left">간호 알람과 수액 타이머를 통합 관리합니다. 수액 주입 속도를 입력하면 종료 예정 시각을 자동 계산하고, 잔량 임계치 도달 시 앱·워치로 알림을 보냅니다.</p>

<!-- 기능 시연 GIF 영역 -->
<div align="center">

<table>
  <tr>
    <th>수액 타이머 설정</th>
    <th>실시간 잔량 모니터링</th>
  </tr>
  <tr>
    <td align="center"><img src="./image/수액타이머설정.gif" width="220"/></td>
    <td align="center"><img src="./image/수액실시간.gif" width="220"/></td>
  </tr>
</table>

</div>

<br/>

### 실시간 알림

<p align="left">환자 호출, 처방 변경, 투약 시각 등 주요 이벤트를 웹·앱·워치로 즉시 전달합니다. FCM 푸시와 SSE 스트리밍을 결합해 알림 지연을 최소화합니다.</p>

<!-- 기능 시연 GIF 영역 -->
<div align="center">

<table>
  <tr>
    <th>Web</th>
    <th>App</th>
    <th>Watch</th>
  </tr>
  <tr>
    <td align="center"><img src="./image/웹알림.gif" width="226"/></td>
    <td align="center"><img src="./image/실시간알림.gif" width="194"/></td>
    <td align="center"><img src="./image/긴급알림_원형.png" width="220"/></td>
  </tr>
</table>

</div>

<br/>

<a id="4-팀원-소개"></a>

## 👥 4. 팀원 소개

<div align="center">

<table align="center">
    <tr>
        <td width="33%" align="center">
            <img src="./image/members/uniform/김가민.png" width="160" height="213" />
            <br/> 김가민 (Leader) <br/>(Infra & AI)
        </td>
        <td width="33%" align="center">
            <img src="./image/members/uniform/김소연.png" width="160" height="213" />
            <br/> 김소연 <br/>(Backend & Database)
        </td>
        <td width="33%" align="center">
            <img src="./image/members/uniform/문현지.png" width="160" height="213" />
            <br/> 문현지 <br/>(Frontend & Design)
        </td>
    </tr>
    <tr>
        <td width="33%" valign="top">
            <sub>
                - AI CLOVA STT 파이프라인 (노이즈 제거, 용어 매핑)<br/>
                - STT 타이머 알림 (BE + FCM)<br/>
                - Jenkins CI/CD 파이프라인 구축<br/>
                - Blue-Green 무중단 배포<br/>
                - Mattermost 배포 알림
            </sub>
        </td>
        <td width="33%" valign="top">
            <sub>
                - 앱 로그인 API 및 리프레시 토큰<br/>
                - IV 수액 타이머 서비스 (종료 알림 스케줄러)<br/>
                - NFC 환자 팔찌/약물 투약 시스템<br/>
                - 인수인계 체크리스트 API <br/>
                - EMR 시스템 기반 ERD 설계
            </sub>
        </td>
        <td width="33%" valign="top">
            <sub>
                - 웹·앱·워치 전반 UI/UX 디자인 및 프론트엔드 구현<br/>
                - 간호사 앱 전체 화면 (NFC 환자·약물 인증, 간호일지 STT, IV 수액 타이머 등)<br/>
                - 워치 앱 전체 구현 (손목 제스처, 간호기록 STT, 환자 긴급 풀스크린 알람)<br/>
                - FCM 푸시 알림 · 앱 SSE 실시간 연동<br/>
                - 환자용 웹앱 구현
            </sub>
        </td>
    </tr>
    <tr>
        <td width="33%" align="center">
            <img src="./image/members/uniform/박승찬.png" width="160" height="213" />
            <br/> 박승찬 <br/>(Backend & AI)
        </td>
        <td width="33%" align="center">
            <img src="./image/members/uniform/이승연.png" width="160" height="213" />
            <br/> 이승연 <br/>(Backend & Design)
        </td>
        <td width="33%" align="center">
            <img src="./image/members/uniform/최현웅.png" width="160" height="213" />
            <br/> 최현웅 <br/>(Frontend & AI)
        </td>
    </tr>
    <tr>
        <td width="33%" valign="top">
            <sub>
                - SSE 실시간 알림 시스템 전체 설계·구현<br/>
                - NFC 환자 웹앱 본인확인 API<br/>
                - IV 수액 SSE 연동<br/>
                - AI 인수인계 PASS-BAR 리포트 개발
            </sub>
        </td>
        <td width="33%" valign="top">
            <sub>
                - Spring Security + JWT 인증 시스템 설계<br/>
                - Redis 리프레시 토큰 구현<br/>
                - 로그인/로그아웃/회원가입 API<br/>
                - AI 환자 증상 LLM 중요도 분류 서비스
            </sub>
        </td>
        <td width="33%" valign="top">
            <sub>
                - 웹 대시보드 EMR UI · 비즈니스 로직 전반<br/>
                - 웹 AI 인수인계 PASS-BAR 리포트 화면 · 통합 요약<br/>
                - 웹 SSE 실시간 구독<br/>
                - NFC 토큰 환자 본인확인 웹앱<br/>&nbsp;&nbsp;+ 환자 음성 STT 통합 (AI + BE + FE)<br/>
                - 환자 호출 비속어 필터링<br/>&nbsp;&nbsp;(AI 엔드포인트 + BE 정제 흐름 + FE 연동)<br/>
                - 앱 NFC 환자·약물 인식 · 음성 메모 STT<br/>&nbsp;&nbsp;· 수액 타이머/속도 계산 · 약물 등록 FE 구현
            </sub>
        </td>
    </tr>
</table>

</div>

<br/>

<a id="5-기술-스택"></a>

## 🛠️ 5. 기술 스택

<h3 align="center">Frontend - 앱 / 워치</h3>

<div align="center">

![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)<br>
![Hilt](https://img.shields.io/badge/Hilt-2196F3?style=for-the-badge&logo=android&logoColor=white)
![Retrofit](https://img.shields.io/badge/Retrofit-48B983?style=for-the-badge&logo=square&logoColor=white)
![Wear OS](https://img.shields.io/badge/Wear%20OS-4285F4?style=for-the-badge&logo=wearos&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)

<br>

|     구분     |              기술               |
| :----------: | :-----------------------------: |
|     언어     |  Kotlin 2.2.20 (compileSdk 35)  |
|      UI      |  Jetpack Compose (Material 3)   |
| 의존성 주입  |              Hilt               |
|    비동기    |        Kotlin Coroutines        |
|   네트워크   |        Retrofit, OkHttp         |
|    직렬화    |      Kotlinx Serialization      |
|  화면 전환   |       Navigation Compose        |
|  로컬 저장   |            DataStore            |
| 이미지 로딩  |              Coil               |
|  워치 연동   |       Wear OS Data Layer        |
|   디바이스   |  NFC, Firebase Cloud Messaging  |

</div>

<br/>

<h3 align="center">Frontend - 웹</h3>

<div align="center">

![Next.js](https://img.shields.io/badge/Next.js-000000?style=for-the-badge&logo=nextdotjs&logoColor=white)
![React](https://img.shields.io/badge/React-61DAFB?style=for-the-badge&logo=react&logoColor=black)
![TypeScript](https://img.shields.io/badge/TypeScript-3178C6?style=for-the-badge&logo=typescript&logoColor=white)<br>
![TanStack Query](https://img.shields.io/badge/TanStack%20Query-FF4154?style=for-the-badge&logo=reactquery&logoColor=white)
![Zustand](https://img.shields.io/badge/Zustand-433E38?style=for-the-badge&logo=react&logoColor=white)
![Tailwind CSS](https://img.shields.io/badge/Tailwind%20CSS-06B6D4?style=for-the-badge&logo=tailwindcss&logoColor=white)<br>
![Axios](https://img.shields.io/badge/Axios-5A29E4?style=for-the-badge&logo=axios&logoColor=white)
![Framer Motion](https://img.shields.io/badge/Framer%20Motion-0055FF?style=for-the-badge&logo=framer&logoColor=white)

<br>

|     구분     |                     기술                      |
| :----------: | :-------------------------------------------: |
|  프레임워크  |             Next.js 16, React 19              |
|     언어     |                 TypeScript 5                  |
|  상태 관리   |            TanStack Query, Zustand            |
|    스타일    |      Tailwind CSS 4, shadcn/ui, Radix UI      |
|      폼      |                React Hook Form                |
|   네트워크   |                     Axios                     |
|    시각화    |                   Recharts                    |
|     기타     | Framer Motion, lucide-react, date-fns, sonner |

</div>

<br/>

<h3 align="center">Backend</h3>

<div align="center">

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Java](https://img.shields.io/badge/Java-007396?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring%20Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)<br>
![JPA](https://img.shields.io/badge/Spring%20Data%20JPA-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white)<br>
![Swagger](https://img.shields.io/badge/Swagger-85EA2D?style=for-the-badge&logo=swagger&logoColor=black)
![Firebase](https://img.shields.io/badge/Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)

<br>

|      구분      |              기술              |
| :------------: | :----------------------------: |
|   프레임워크   |         Spring Boot 3          |
|      언어      |            Java 17             |
|  데이터 접근   |        Spring Data JPA         |
|      캐시      |             Redis              |
|   인증/인가    |      Spring Security, JWT      |
|    API 문서    | SpringDoc OpenAPI (Swagger UI) |
|      푸시      |    Firebase Admin SDK (FCM)    |
| 분산 스케줄 락 |            ShedLock            |
|    모니터링    |      Spring Boot Actuator      |

</div>

<br/>

<h3 align="center">AI</h3>

<div align="center">

![FastAPI](https://img.shields.io/badge/FastAPI-009688?style=for-the-badge&logo=fastapi&logoColor=white)
![Python](https://img.shields.io/badge/Python-3670A0?style=for-the-badge&logo=python&logoColor=white)
![Uvicorn](https://img.shields.io/badge/Uvicorn-499848?style=for-the-badge&logo=gunicorn&logoColor=white)<br>
![Claude](https://img.shields.io/badge/Claude-D97757?style=for-the-badge&logo=anthropic&logoColor=white)
![CLOVA](https://img.shields.io/badge/CLOVA%20Speech-03C75A?style=for-the-badge&logo=naver&logoColor=white)
![SQLAlchemy](https://img.shields.io/badge/SQLAlchemy-D71F00?style=for-the-badge&logo=sqlalchemy&logoColor=white)

<br>

|      구분       |                                 기술                                |
| :-------------: | :-----------------------------------------------------------------: |
|   프레임워크    |           FastAPI, Uvicorn (Python 3.12)                            |
| 음성 인식 (STT) |                         Naver CLOVA Speech                          |
|   형태소 분석   |                                Kiwi                                 |
| 의료 용어 교정  |              RapidFuzz (퍼지 매칭 기반 용어 사전 매핑)              |
|       LLM       | Claude Haiku (발화 분류 · 욕설 필터), Claude Sonnet (인수인계 요약)  |
|    스트리밍     |                         SSE (sse-starlette)                         |
|     DB 연동     |                   SQLAlchemy, asyncpg / psycopg2                    |

</div>

<br/>

<h3 align="center">Database & Infrastructure</h3>

<div align="center">

![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white)<br>
![Jenkins](https://img.shields.io/badge/Jenkins-D24939?style=for-the-badge&logo=jenkins&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![Nginx](https://img.shields.io/badge/Nginx-009639?style=for-the-badge&logo=nginx&logoColor=white)<br>
![GitLab](https://img.shields.io/badge/GitLab-FC6D26?style=for-the-badge&logo=gitlab&logoColor=white)
![Mattermost](https://img.shields.io/badge/Mattermost-0058CC?style=for-the-badge&logo=mattermost&logoColor=white)

<br>

**Database**

|  구분  |      기술       |
| :----: | :-------------: |
| RDBMS  |  PostgreSQL 17  |
|  캐시  |      Redis      |

**Infrastructure**

|      구분      |                      기술                       |
| :------------: | :---------------------------------------------: |
|     CI/CD      |     Jenkins (Build → Deploy → Image Prune)      |
|    컨테이너    |             Docker, Docker Compose              |
|   배포 전략    |  Blue-Green 무중단 배포 (dev / prod 환경 분리)  |
| 리버스 프록시  |              Nginx (upstream 전환)              |
|   형상 관리    |                     GitLab                      |
|      알림      |       Mattermost (빌드 / 배포 결과 통지)        |

</div>

<br/>

<a id="6-시스템-아키텍처"></a>

## 🏗️ 6. 시스템 아키텍처
<div align="center">

<img src="./image/시스템아키텍처.png"/>

</div>

<br/>

<a id="7-erd"></a>

## 🗄️ 7. ERD

<div align="center">
    <img src="./image/erd.png"/>
</div>

<br/>

<a id="8-프로젝트-구조"></a>

## 📁 8. 프로젝트 구조

```
S14P31E101/
├── ai/             # FastAPI AI 서버 (Python 3.12)
├── backend/        # Spring Boot API 서버 (Java 17)
├── frontend/       # 모바일 앱 · 워치(Kotlin) · 웹(Next.js)
├── infra/          # 배포 스크립트 (Blue-Green, Nginx)
└── Jenkinsfile     # CI/CD 파이프라인 정의
```

<details>
<summary><b>AI</b></summary>

```
ai/
├── Dockerfile
├── requirements.txt
└── nursing_ai/
    ├── app/
    │   ├── main.py              # FastAPI 엔트리포인트
    │   ├── routers/             # API 엔드포인트
    │   │   ├── stt.py           # 간호사 음성 STT
    │   │   ├── stt_patient.py   # 환자 음성 STT
    │   │   ├── correction.py    # 의료 용어 교정
    │   │   ├── classification.py# 발화 분류 (우선순위·카테고리)
    │   │   ├── filter.py        # 욕설·부적절 발화 필터
    │   │   └── handover.py      # 인수인계 요약
    │   ├── services/            # 핵심 비즈니스 로직
    │   │   ├── nursing_stt/     # CLOVA STT · 노이즈 캔슬링 · 형태소 · 용어 매핑
    │   │   ├── classification/  # Claude 기반 분류 · 욕설 필터
    │   │   ├── handover/        # 인수인계 요약 (LLM · 임상 규칙 · 사전)
    │   │   └── audio_storage.py # 음성 파일 저장
    │   ├── database/            # DB 연결 (sync · async)
    │   └── middleware/          # JWT 인증
    └── tools/                   # 분석·디버깅 도구
```

</details>

<details>
<summary><b>Backend</b></summary>

```
backend/src/main/
├── java/com/ssafy/happynurse/
│   ├── domain/                  # 도메인별 패키지
│   │   ├── auth/                # 인증 (JWT · Redis 세션)
│   │   ├── patient/             # 환자
│   │   ├── nurse/               # 간호사 · 알림(FCM)
│   │   ├── nurseSTT/            # 간호 STT 기록
│   │   ├── doctor/              # 의사 오더
│   │   ├── handover/            # 인수인계
│   │   ├── nfc/                 # NFC 태그 (환자 팔찌·약물)
│   │   ├── device/              # 디바이스 등록
│   │   ├── reminder/            # 리마인더 · 스케줄러
│   │   ├── watch/               # 워치 연동
│   │   ├── his/                 # 병원정보시스템(HIS) 연동
│   │   ├── webapp/              # 웹앱 전용 API
│   │   └── common/              # 공통 엔티티
│   └── global/                  # config · security · exception · response
└── resources/                   # application.yml · firebase · classification
```

</details>

<details>
<summary><b>Frontend</b></summary>

```
frontend/
├── mobile/                      # Kotlin (Android)
│   ├── app/                     # 모바일 앱 (간호사용)
│   │   └── src/main/java/com/happynurse/
│   │       ├── data/            # remote(API·FCM·SSE) · nfc · audio · wearable · repository
│   │       ├── domain/          # 도메인 모델
│   │       ├── di/              # Hilt 모듈
│   │       └── presentation/    # Compose 화면 · 컴포넌트 · 네비게이션 · 테마
│   │           └── screens/     # login · patients · patientdetail · nfc · logentry
│   │                            #   drugentry · ivtimer · timer · handoff · mypage
│   └── wear/                    # Wear OS 스마트워치 앱
│       └── src/main/java/com/happynurse/wear/
│           ├── data/            # remote · wearable · audio · eventbus · repository
│           ├── domain/ · di/ · alarm/ · gesture/
│           └── presentation/    # screens(home · alarm · stt · record · detail · pager)
└── web/                         # Next.js 16 웹 관리 콘솔
    └── src/
        ├── app/                 # App Router ((auth)/login · (web)/dashboard·handover · patient)
        ├── features/            # 기능별 모듈 (auth · dashboard · handover · patient)
        │                        #   각 모듈: api · components · hooks · stores · types
        ├── components/          # 공통 UI (common · layout · patient · ui)
        └── lib/                 # 유틸 · 설정
```

</details>

<br/>

<a id="9-영상-포트폴리오"></a>

## 🎬 9. 영상 포트폴리오

<div align="center">

<a href="https://youtu.be/uzQpA21Kzg4">
  <img src="https://img.youtube.com/vi/uzQpA21Kzg4/maxresdefault.jpg" alt="HappyNurse 영상 포트폴리오" width="500"/>
</a>

<br/>

▶️ <a href="https://youtu.be/uzQpA21Kzg4"><b>HappyNurse 영상 포트폴리오 보러가기 (YouTube)</b></a>

</div>
