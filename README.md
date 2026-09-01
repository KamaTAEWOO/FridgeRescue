# 냉장고 구조대

매번 재료를 직접 등록하지 않아도 되는 로컬 우선 Android 냉장고 앱입니다. 온라인 주문내역의 텍스트·이미지·PDF를 공유하거나 종이 영수증을 촬영하면, 온디바이스 OCR과 규칙 기반 선별로 버리기 쉬운 식재료만 한 번에 담습니다.

## 구현된 핵심 경험

- Android Sharesheet의 텍스트·이미지·PDF 수신과 프로세스 종료 후 초안 복구
- ML Kit 한국어 온디바이스 OCR, Google Code Scanner 일반·GS1 바코드, PDF 페이지 렌더링, 영수증 카메라·Photo Picker·직접 입력 대체 경로
- 식품·비식품 선별, 수량 추출, 같은 주문 재공유 중복 경고, 선택 품목 일괄 저장
- Room 기반 냉장고, 표시 날짜와 앱 예상 소비일 구분, 임박도·개봉·고정 우선 구조 큐
- 먹음·아직 있음·일부 사용·버림 이력, 선택 폐기 사유, 멱등 처리와 실행 취소
- WorkManager D-3·D-1·당일 요약 알림, 알림 직접 행동, 권한 거부 시 홈 배지, 조용한 시간
- 구조·폐기 리포트와 반복 폐기 장보기 힌트
- 시스템 다크 모드, 폰·넓은 화면 적응형 Compose UI, 로컬 저장 개인정보 안내

## 기술 구성

- Kotlin 2.3, Jetpack Compose + Material 3, UDF + ViewModel + StateFlow
- Room 2.8, WorkManager 2.11, Preferences DataStore 1.2
- ML Kit Text Recognition v2 한국어 번들 모델, Google Code Scanner 16.1, `PdfRenderer`, Photo Picker, FileProvider
- Android 8.0(API 26) 이상, compile/target SDK 36, JDK 17

```text
외부 공유·카메라·Photo Picker
        ↓
앱 전용 캐시 → 온디바이스 OCR/PDF → 규칙 기반 후보 선별
        ↓                              ↓
   24시간 정리                   일괄 확인·Room 저장
                                       ↓
                           구조 큐·행동 이력·알림·리포트
```

## 검증

- 자동 테스트 90개: JVM 47개, Room·Intent·Compose 계측 43개
- API 36 에뮬레이터에서 전체 테스트, Lint, Debug APK 빌드 통과
- 실제 생성 영수증 이미지에서 `두부 2개`, `시금치 1봉` OCR·선별 확인
- GitHub Actions에서 단위 테스트, Android Lint, APK 빌드 수행

```bash
./gradlew testDebugUnitTest lintDebug connectedDebugAndroidTest assembleDebug
```

## 문서

- [구현 현황](./docs/IMPLEMENTATION_STATUS.md)
- [제품 기획서](./docs/PRODUCT_SPEC.md)
- [사용자 흐름](./docs/USER_FLOW.md)
- [Android Compose 구현 계획](./docs/ANDROID_PLAN.md)
- [예외처리 기준](./docs/EDGE_CASES.md)
- [테스트 케이스](./docs/TEST_CASES.md)

서버·가족 공유는 로컬 기능 완성 이후 범위입니다.
