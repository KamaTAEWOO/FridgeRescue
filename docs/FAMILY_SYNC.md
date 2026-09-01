# 가족 공유와 서버 동기화

## 범위

가족 공유는 선택 기능이다. 연결하지 않으면 기존처럼 Room만 사용하는 로컬 우선 앱으로 동작한다. 연결하면 식재료 스냅샷만 사용자가 지정한 서버와 동기화하며 영수증 원문, 공유 파일, 행동 이력과 알림 설정은 전송하지 않는다.

## 구성

```text
Android 설정 화면
  ├─ 익명 계정 생성 → 개인 가족 공간 + 6자리 초대 코드
  ├─ 초대 코드 참가 → 기존 가족 공간으로 이동
  └─ 지금 동기화
          ↓ Bearer token / JSON
Ktor server
  ├─ 계정·가족·초대 코드
  ├─ 가족별 식재료 스냅샷
  ├─ updatedAt + accountId 결정적 충돌 해결
  └─ JSON 파일 원자적 저장
```

같은 재료 ID가 충돌하면 `updatedAtEpochMillis`가 최신인 값을 사용한다. 시각이 같고 내용이 다를 때만 account ID를 최종 tie-breaker로 사용해 모든 기기가 같은 결과에 수렴하도록 한다. 사용자가 `지금 동기화`를 누르기 전의 로컬 작업은 그대로 유지된다.

## 로컬 실행

```bash
./gradlew :server:run
```

- 기본 포트: `8080`
- 기본 저장 파일: `data/family-store.json`
- 포트 변경: `PORT=9090 ./gradlew :server:run`
- 저장 위치 변경: `FRIDGE_RESCUE_DATA_FILE=/safe/path/family.json ./gradlew :server:run`
- Android 에뮬레이터 Debug 기본 주소: `http://10.0.2.2:8080`

Debug 빌드만 로컬 HTTP 연결을 허용한다. Release 빌드는 cleartext를 차단하므로 배포 서버는 HTTPS여야 한다. Bearer token은 HTTPS/TLS 연결에서만 운영해야 한다.

## API

| Method | Path | 인증 | 역할 |
|---|---|---|---|
| `GET` | `/health` | 없음 | 상태 확인 |
| `POST` | `/v1/accounts` | 없음 | 익명 계정과 가족 공간 생성 |
| `POST` | `/v1/families/join` | Bearer | 초대 코드로 가족 참가 |
| `POST` | `/v1/sync` | Bearer | 로컬 항목 제출 후 가족 스냅샷 수신 |

## 현재 제약

- 사용자가 누르는 명시적 동기화만 제공하며 자동 백그라운드 재시도는 아직 하지 않는다.
- 토큰 철회, 계정 복구, 가족 탈퇴·소유권 이전을 위한 운영자 API는 제공하지 않는다.
- JSON 파일 저장은 단일 서버 인스턴스용이다. 수평 확장 시 트랜잭션 DB와 비밀 관리 시스템으로 교체해야 한다.
- 현재 token은 앱 전용 DataStore와 서버 상태 파일에 저장한다. 운영 환경에서는 Android Keystore 기반 암호화와 서버 비밀 저장소를 적용해야 한다.
