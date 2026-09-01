# 릴리스 빌드 가이드

## 서명 설정

실제 키와 비밀번호는 Git에 넣지 않는다. `keystore.properties.example`을
`keystore.properties`로 복사한 뒤 절대 경로와 네 값을 채운다. 저장소는 해당 파일과
`*.jks`, `*.keystore`를 무시한다.

```properties
storeFile=/absolute/path/to/fridge-rescue-upload.jks
storePassword=...
keyAlias=fridge-rescue-upload
keyPassword=...
```

Google Play 배포용 업로드 키는 별도 보안 저장소에 백업하고, QA 키를 배포에 재사용하지
않는다. 키가 설정되지 않아도 minified unsigned 산출물은 만들 수 있지만 설치·업로드할
수는 없다.

## 산출물

```bash
./gradlew clean testDebugUnitTest lintRelease assembleRelease bundleRelease
```

- APK: `app/build/outputs/apk/release/app-release.apk`
- AAB: `app/build/outputs/bundle/release/app-release.aab`
- R8 매핑: `app/build/outputs/mapping/release/mapping.txt`

서명 검증:

```bash
$ANDROID_SDK_ROOT/build-tools/36.0.0/apksigner verify --verbose \
  app/build/outputs/apk/release/app-release.apk
jarsigner -verify -verbose -certs app/build/outputs/bundle/release/app-release.aab
```

## 출시 전 체크

- `versionCode` 증가와 사용자용 `versionName` 확정
- Play App Signing 등록 및 업로드 인증서 SHA-256 보관
- Release APK를 API 26·최신 API에서 콜드 스타트·OCR·바코드·알림 스모크
- `mapping.txt`와 `seeds.txt`를 해당 버전 산출물과 함께 보관
- 개인정보처리방침 URL, 스토어 설명·스크린샷, 데이터 보안 양식 확인
