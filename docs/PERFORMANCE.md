# 성능 측정과 Baseline Profile

## 구성

- `benchmark` 모듈은 앱과 분리된 `com.android.test` 모듈이다.
- `BaselineProfileGenerator`는 콜드 스타트 후 재료함과 설정으로 이동하는 핵심 사용자 흐름을 기록한다.
- 생성 규칙은 앱 패키지로 한정하고 `app/src/main/generated/baselineProfiles`에 보관한다.
- `StartupBenchmark`는 R8과 리소스 축소가 적용된 비디버그 `benchmark` 빌드를 측정한다.
- 앱은 `profileable`만 허용하며 디버그 가능 상태로 측정하지 않는다.

## 2026-09-02 API 36 에뮬레이터 참고 측정

| 조건 | 콜드 스타트 중앙값 | P95 CPU 프레임 시간 |
|---|---:|---:|
| 컴파일 없음 | 286.7 ms | 116.7 ms |
| Baseline Profile 적용 | 268.2 ms | 68.5 ms |

콜드 스타트 중앙값은 약 6.5% 감소했다. 다만 에뮬레이터는 실제 사용자 기기의 CPU, 저장장치, 발열 특성을 재현하지 않으므로 이 결과는 회귀 확인용 참고값이며 포트폴리오의 실기기 성능 주장으로 사용하지 않는다.

## 실행 방법

API 33 이상 연결 기기에서 프로파일을 생성한다.

```bash
ANDROID_SERIAL=<serial> ./gradlew :app:generateBaselineProfile \
  -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules=BaselineProfile
```

실기기에서 벤치마크를 실행한다.

```bash
ANDROID_SERIAL=<serial> ./gradlew :benchmark:connectedBenchmarkReleaseAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules=Macrobenchmark
```

에뮬레이터에서 파이프라인만 확인할 때는 아래 오류 억제 옵션을 추가할 수 있지만, 결과를 실기기 수치로 취급하면 안 된다.

```bash
-Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.suppressErrors=EMULATOR
```

상세 JSON과 Perfetto trace는 `benchmark/build/outputs/connected_android_test_additional_output` 아래에 생성된다.
