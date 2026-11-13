# Maple Sunday Notifier v1.2

개인용 메이플스토리 썬데이 메이플 공지 Discord Webhook 알림. GitHub Actions 예약 실행, Java 21 / Spring Boot 3.5.7, DB/웹 서버 불필요.

## 동작
- 메이플스토리 Open API `GET /maplestory/v1/notice-event`를 매 실행 시 조회.
- 제목에서 공백을 제거한 뒤 `썬데이메이플`이 포함된 공지만 선택.
- 기존 처리 기준점보다 최신인 공지 중 `date_event_end`가 아직 지나지 않은 가장 최신 공지 **1건**만 Webhook으로 보냄.
- 최초 실행에도 최근 공지가 유효하다면 최신 1건 전송. 유효하지 않다면 미발송 상태로 커서 초기화.
- 같은 ID의 제목/본문 수정은 무시. 스케줄 누락이나 장기 장애 복구 후에도 최신 유효 공지 1건만 발송.
- 성공 시 커서가 `state.json`에 기록되고 GitHub Actions가 커밋/푸시함.

## GitHub 설정
1. **Private** GitHub 저장소 새로 만들기. 이 프로젝트의 모든 파일을 기본 브랜치 `main`에 업로드.
2. [NEXON Open API](https://openapi.nexon.com/)에서 키 발급.
3. Discord 채널 설정 > 연동 > 웹후크에서 URL 복사.
4. GitHub 저장소 Settings > Secrets and variables > Actions > New repository secret에 `NEXON_API_KEY`, `DISCORD_WEBHOOK_URL` 등록.
5. Settings > Actions > General > Workflow permissions에서 `Read and write permissions` 허용 (워크플로는 `contents: write`도 명시).
6. Actions > Maple Sunday notification > Run workflow 로 최초 수동 실행. 이후 자동 예약 실행.
7. Actions 실행 로그와 `state.json` 커밋 여부 확인.

## 예약 실행 (KST)
금요일 09:07부터 18:57까지 10분 간격, 매일 21:13에 추가 실행. GitHub 예약은 지연되거나 누락될 수 있으며 10~20분 지연은 목표일 뿐 보장되지 않음.

## 로컬 실행
Java 21, Maven 3.9+ 설치 후 환경변수를 설정한다.

```bash
export NEXON_API_KEY='...'
export DISCORD_WEBHOOK_URL='https://discord.com/api/webhooks/...'
mvn test
mvn spring-boot:run
```

PowerShell에서는 `export` 대신 `$env:NEXON_API_KEY='...'` 형식을 사용.

## 상태 / 실패 시 동작
- `state.json`은 저장소에서 계속 추적. 삭제하면 처음 실행으로 돌아가 중복 가능.
- 읽을 수 없거나 손상된 JSON이면 알림을 전송하지 않고 실패.
- HTTP 429, 5xx, 연결 오류는 최대 3회 재시도. 나머지 4xx는 즉시 실패.
- Webhook에 `wait=true`를 붙여 실제 메시지 ID를 확인한 뒤 상태 저장.
- Discord 전송 성공 뒤 Git Push 실패 시 같은 공지를 중복 전송할 수 있음. 외부 Webhook과 Git 트랜잭션을 원자적으로 묶을 수 없기 때문.
- 종료시각 정보가 비어 있으면 유효하다고 가정하지 않고 건너뛰며 경고 로그 출력.
- 초기 상태나 유효기간 기준으로 가장 최신 1건을 선택하며 알림은 누락보다 중복 위험 최소화를 우선하지 않음.

## 구현 전제 및 검증 필요
NEXON 공지 API의 `date_event_end`가 실제 썬데이 혜택 종료 시각인지 실데이터를 대조해야 함. API 키가 제공되지 않아 실 API 통합 테스트는 이 패키지에서 수행하지 못함. 실제 반환 값의 UTC 오프셋과 이벤트 날짜 정확도를 최초 운영 시 확인할 것.
