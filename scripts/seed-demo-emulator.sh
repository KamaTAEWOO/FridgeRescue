#!/usr/bin/env bash
set -euo pipefail

project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
adb_bin="${ADB_BIN:-adb}"
device_serial="${ANDROID_SERIAL:-}"

if [[ -z "$device_serial" ]]; then
    device_serial="$($adb_bin devices | awk 'NR > 1 && $2 == "device" { print $1; exit }')"
fi
if [[ -z "$device_serial" ]]; then
    echo "실행 중인 Android 에뮬레이터를 찾지 못했습니다." >&2
    exit 1
fi

cd "$project_root"
./gradlew :app:assembleDebug

if ! "$adb_bin" -s "$device_serial" install -r -t app/build/outputs/apk/debug/app-debug.apk; then
    echo "기존 앱과 Debug 서명이 다릅니다. 테스트 기기라면 앱 제거 후 다시 실행하세요:" >&2
    echo "  $adb_bin -s $device_serial uninstall com.portfolio.fridgerescue" >&2
    exit 1
fi

gallery_dir="/sdcard/Pictures/FridgeRescueDemo"
"$adb_bin" -s "$device_serial" shell mkdir -p "$gallery_dir"
for image_path in "$project_root"/demo-data/gallery/*.png; do
    image_name="$(basename "$image_path")"
    "$adb_bin" -s "$device_serial" push "$image_path" "$gallery_dir/$image_name"
    "$adb_bin" -s "$device_serial" shell am broadcast \
        -a android.intent.action.MEDIA_SCANNER_SCAN_FILE \
        -d "file://$gallery_dir/$image_name" >/dev/null
done

seed_result="$($adb_bin -s "$device_serial" shell am broadcast \
    -a com.portfolio.fridgerescue.action.SEED_DEMO_DATA \
    -n com.portfolio.fridgerescue/.debug.DemoDataSeedReceiver)"
if [[ "$seed_result" != *"foodItems=18,events=7"* ]]; then
    echo "$seed_result" >&2
    echo "데모 데이터 주입에 실패했습니다." >&2
    exit 1
fi

"$adb_bin" -s "$device_serial" shell am force-stop com.portfolio.fridgerescue
"$adb_bin" -s "$device_serial" shell am start \
    -n com.portfolio.fridgerescue/.MainActivity >/dev/null

echo "완료: $device_serial"
echo "- 앱 데이터: 식재료 18개, 이력 7개"
echo "- 사진첩: $gallery_dir 이미지 3개"
