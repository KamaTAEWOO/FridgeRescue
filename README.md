# 냉장고 구조대

Kotlin과 Jetpack Compose로 제작할 Android 포트폴리오 프로젝트입니다.

## 현재 상태

- Android 프로젝트와 Gradle 설정만 완료
- 앱 기능 구현은 시작하지 않음
- 패키지: `com.portfolio.fridgerescue`
- 최소 지원 버전: Android 8.0 (API 26)
- 컴파일/타깃 SDK: API 36
- JDK: 17

## 계획한 기술

- Jetpack Compose + Material 3
- ViewModel + StateFlow + UDF
- Room 기반 offline-first 데이터 계층
- Android Sharesheet로 이미지·PDF·텍스트 수신
- ML Kit Document Scanner, Text Recognition, Barcode Scanning
- WorkManager 기반 임박 알림
- DataStore 기반 사용자 설정

기능 구현 전에는 필요한 라이브러리를 추가하지 않습니다. 각 기능을 시작할 때 공식 문서와 기획서 기준으로 의존성을 도입합니다.

## 기획 문서

- [문서 인덱스](./docs/README.md)
- [제품 기획서](./docs/PRODUCT_SPEC.md)
- [사용자 흐름](./docs/USER_FLOW.md)
- [Android Compose 구현 계획](./docs/ANDROID_PLAN.md)
- [예외처리 기준](./docs/EDGE_CASES.md)
- [테스트 케이스](./docs/TEST_CASES.md)

제품 기획과 구현 기준을 먼저 확정하며, 기능 구현은 아직 시작하지 않았습니다.
