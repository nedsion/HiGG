# HiGG

[![Views](https://hits.sh/github.com/nedsion/HiGG.svg?label=views&color=7c3aed)](https://hits.sh/github.com/nedsion/HiGG/)
[![Downloads](.github/badges/downloads.svg)](../../releases)

**[English](#english) · [Tiếng Việt](#tiếng-việt)**

Call Google Gemini by saying **"Hey Google" / "OK Google"** on vivo phones running the China ROM (OriginOS), where Google's own hotword can't work — no root, no ADB, no Shizuku, no Accessibility service.

Gọi Google Gemini bằng câu **"Hey Google" / "OK Google"** trên điện thoại vivo bản nội địa (OriginOS), nơi "Ok Google" thật của Google không chạy được — không cần root, ADB, Shizuku hay Trợ năng.

---

## English

### Why
On China-ROM vivo phones the Google app is installed as a normal app, so Android denies it the hotword permission (`CAPTURE_AUDIO_HOTWORD`) and the low-power hotword chip, which only vivo's own Jovi can use. "Hey Google" therefore never works, and Jovi's "Hi Jovi" can't be redirected to Gemini.

HiGG listens for the wake word itself, entirely on the phone, and opens the **Gemini overlay** when it hears it.

### Features
- **"Hey Google" / "OK Google" → Gemini overlay**, also with the screen off or locked (the screen turns on; unlock with face or fingerprint and Gemini appears).
- **Voice lock**: record 6 samples and only your voice opens Gemini — the TV and people nearby are ignored.
- **False-trigger filtering**: the wake word must start the sentence, and every candidate is re-checked by a full speech model before Gemini opens.
- **Battery saving**: the recognizer pauses while the room is quiet.
- **vivo shortcut keys**: set *Quick launch* / *Shortcut button → Open app → HiGG* and the key opens Gemini directly; or long-press the HiGG icon → *Open Gemini*.
- Vietnamese / English interface, light and dark themes.
- Works alongside banking apps: no Accessibility service, nothing needs Developer options after install.

### Requirements
- vivo phone on OriginOS (tested: OriginOS 6, Android 16, model V2359A). Other Android 10+ phones may work but aren't tested.
- The **Google** app and **Gemini** app from Play Store, signed in.
- About 200 MB of free storage (speech models are unpacked on first start).

### Install
1. Download `HiGG-v0.3.0.apk` from [Releases](../../releases) and open it. OriginOS warns about apps from outside its store — tick *I understand* and continue.
2. Open HiGG and complete the **Setup** list:
   - **Microphone & notifications** — tap *Allow microphone*; listening starts right away.
   - **Display over other apps** — needed to open Gemini while HiGG runs in the background.
   - **Unrestricted battery** — so OriginOS doesn't stop HiGG.
   - **Keep permissions** — turn off *Pause app activity if unused*, otherwise Android may revoke the microphone after a few months.
   - **Auto-start** — allow HiGG in vivo's *Auto-start* list so it resumes after a reboot or update.
3. Recommended: **Set up voice lock** and read "Hey Google" or "OK Google" 6 times in a quiet place.
4. Optional: turn off "Hi Jovi" so both assistants don't compete for the microphone.

After a reboot, Android 14+ doesn't allow microphone apps to start on their own; HiGG shows a notification — tap it once to resume.

### How it works
```
mic ─▶ silence gate ─▶ Vosk (restricted grammar) ─▶ wake word at start of sentence?
                                                     │
                     no voice lock: full-model recheck ┤ voice lock: speaker match
                                                     ▼
                     transparent 1 px overlay ─▶ VOICE_COMMAND ─▶ Gemini overlay
```
- **Recognition:** [Vosk](https://alphacephei.com/vosk/) small English model with a restricted vocabulary that includes look-alike words ("go ahead", "good", "girl"…) so they aren't forced into "google".
- **Recheck:** a candidate is accepted only if the unconstrained Vosk model also hears "ok/okay/hey google" (top-5 alternatives).
- **Voice lock:** Vosk speaker x-vectors. The threshold is computed from your own samples (floor 0.6), and an inconsistent sample is dropped and re-recorded.
- **Opening Gemini:** `VOICE_COMMAND` sent to the Google app opens the floating Gemini panel. Android 15+ only allows a background app with *Display over other apps* to start an activity while it shows an overlay window, so HiGG adds a fully transparent 1×1 px window for 0.6 s.
- **Staying alive on OriginOS:** the microphone stays open (OriginOS freezes background apps that stop recording); while Gemini uses the mic, HiGG receives silence and waits.

### Privacy
All recognition runs on the phone. HiGG has no internet permission and never records or uploads audio. The microphone indicator stays on while HiGG listens — that's Android showing an app using the mic.

### Limitations
- Always-on listening uses the CPU (about 8% of one core measured on the test phone, less in quiet rooms). A dedicated hotword chip like Jovi's is only available to system apps.
- Very noisy places reduce accuracy; repeat the wake word or use a shortcut key.
- The voice lock is a convenience filter, not a security feature: a recording of your voice could pass.
- Recognition is tuned for English pronunciation of "Google".

### Build from source
Requirements: JDK 17, Android SDK 35.
```bash
./gradlew assembleDebug          # debug build
./gradlew assembleRelease        # release build (needs keystore.properties, see below)
```
For a signed release, create `keystore.properties` in the project root (never commit it):
```properties
storeFile=/path/to/higg-release.jks
storePassword=…
keyAlias=higg
keyPassword=…
```

**Silent test harness (debug builds only).** Push 16 kHz mono WAV files to the app and run them through the exact on-device pipeline instead of the microphone:
```bash
tar -C my-wavs -cf - . | adb exec-in run-as com.higg sh -c "mkdir -p files/test/set1 && tar -C files/test/set1 -xf -"
adb shell am broadcast -n com.higg/.TestFeedReceiver -a com.higg.TEST_FEED --es set set1
adb shell run-as com.higg cat files/test/result-set1.txt
```
Other modes: `--es mode enroll` (create a voice profile from the files), `--es mode launch` (open Gemini as if the wake word was heard). Debug builds also write a log: `adb shell run-as com.higg cat files/debug.log`.

### Project layout
| Path | Purpose |
|---|---|
| `HotwordService.kt` | Foreground service: microphone loop, pause while Gemini listens |
| `HotwordPipeline.kt` | Decides whether a sentence opens Gemini (shared by service and test harness) |
| `HotwordDetector.kt` | Vosk recognizers, grammars, recheck |
| `SpeechGate.kt` | Skips silence to save CPU |
| `VoiceProfile.kt`, `EnrollActivity.kt` | Voice lock |
| `Gemini.kt` | Opening the Gemini overlay from the background / lock screen |
| `I18n.kt` | All Vietnamese and English text |
| `ui/` | Jetpack Compose screens and design system |
| `src/debug/` | Silent test harness (not in release builds) |

### Credits
- [Vosk](https://github.com/alphacep/vosk-api) speech recognition and the `vosk-model-small-en-us-0.15` / `vosk-model-spk-0.4` models — Apache 2.0 (see the [Vosk models page](https://alphacephei.com/vosk/models))
- [JNA](https://github.com/java-native-access/jna) — Apache 2.0 / LGPL 2.1
- [ZXing](https://github.com/zxing/zxing) — Apache 2.0
- [Inter](https://github.com/rsms/inter) font — SIL Open Font License 1.1
- UI direction made with the [ui-ux-pro-max](https://github.com/nextlevelbuilder/ui-ux-pro-max-skill) skill

HiGG is not affiliated with Google or vivo. "Google" and "Gemini" are trademarks of Google LLC.

License: not decided yet — all rights reserved for now.

### Support
If HiGG helps you, you can support the author from the app (*Support HiGG*):
- Bank transfer (Vietnam): **Timo by Ban Viet Bank · 9037041009728 · NGUYEN HUU NAM** · note "Ung Ho HiGG"
- PayPal: **starnam23@gmail.com**

---

## Tiếng Việt

### Vì sao có HiGG
Trên máy vivo bản nội địa, app Google chỉ là app thường nên Android không cấp quyền nghe từ khóa (`CAPTURE_AUDIO_HOTWORD`) và không cho dùng chip nghe tiết kiệm pin — chỉ Jovi của vivo dùng được. Vì vậy "Hey Google" không bao giờ chạy, và cũng không thể chuyển "Hi Jovi" sang Gemini.

HiGG tự nghe từ khóa ngay trên máy và mở **bảng nổi Gemini** khi nghe thấy.

### Tính năng
- **"Hey Google" / "OK Google" → bảng nổi Gemini**, cả khi tắt màn hình hay đang khóa (màn hình tự sáng; mở khóa bằng khuôn mặt hoặc vân tay là Gemini hiện ra).
- **Khóa giọng nói**: đọc mẫu 6 lần, chỉ giọng bạn mở được Gemini — tiếng TV và người xung quanh bị bỏ qua.
- **Lọc bật nhầm**: từ khóa phải đứng đầu câu, và mỗi lần nghi là từ khóa đều được mô hình nhận diện đầy đủ nghe lại trước khi mở Gemini.
- **Tiết kiệm pin**: tạm dừng nhận diện khi phòng yên tĩnh.
- **Phím tắt vivo**: đặt *Khởi chạy nhanh* / *Nút lối tắt → Mở ứng dụng → HiGG* là bấm phím mở thẳng Gemini; hoặc nhấn giữ biểu tượng HiGG → *Mở Gemini*.
- Giao diện tiếng Việt / tiếng Anh, sáng và tối.
- Dùng chung được với app ngân hàng: không dùng Trợ năng, không cần bật Tùy chọn nhà phát triển sau khi cài.

### Yêu cầu
- Điện thoại vivo chạy OriginOS (đã thử: OriginOS 6, Android 16, máy V2359A). Máy Android 10+ khác có thể chạy nhưng chưa thử.
- App **Google** và **Gemini** từ Play Store, đã đăng nhập.
- Còn trống khoảng 200 MB (mô hình giọng nói được giải nén lần đầu mở).

### Cài đặt
1. Tải `HiGG-v0.3.0.apk` ở mục [Releases](../../releases) rồi mở. OriginOS sẽ cảnh báo app ngoài cửa hàng — tích *Tôi đã hiểu* rồi tiếp tục cài.
2. Mở HiGG và làm hết danh sách **Thiết lập**:
   - **Micro và thông báo** — bấm *Cấp quyền micro*, app bắt đầu nghe ngay.
   - **Hiển thị trên ứng dụng khác** — để mở được Gemini khi HiGG chạy nền.
   - **Không giới hạn pin** — để OriginOS không tắt HiGG.
   - **Không tạm dừng khi ít dùng** — tắt *Tạm dừng hoạt động nếu không dùng*, nếu không Android có thể thu hồi quyền micro sau vài tháng.
   - **Tự động khởi động** — bật HiGG trong danh sách *Tự động khởi động* của vivo để app tự chạy lại sau khi khởi động máy hoặc cập nhật.
3. Nên làm: **Đăng ký giọng nói**, đọc "Hey Google" hoặc "OK Google" 6 lần ở nơi yên tĩnh.
4. Tùy chọn: tắt "Hi Jovi" để hai trợ lý không giành micro.

Sau khi khởi động lại máy, Android 14+ không cho app dùng micro tự chạy; HiGG sẽ hiện thông báo — chạm một lần để nghe lại.

### Cách hoạt động
```
micro ─▶ bỏ qua im lặng ─▶ Vosk (từ vựng giới hạn) ─▶ từ khóa ở đầu câu?
                                                       │
           chưa khóa giọng: nghe lại bằng mô hình đầy đủ ┤ đã khóa giọng: so giọng chủ máy
                                                       ▼
           cửa sổ trong suốt 1 px ─▶ VOICE_COMMAND ─▶ bảng nổi Gemini
```
- **Nhận diện:** mô hình tiếng Anh nhỏ của [Vosk](https://alphacephei.com/vosk/), từ vựng giới hạn có thêm các từ dễ nhầm ("go ahead", "good", "girl"…) để chúng không bị ép thành "google".
- **Nghe lại:** chỉ nhận khi mô hình Vosk không giới hạn từ vựng cũng nghe ra "ok/okay/hey google" (trong 5 cách nghe khả dĩ nhất).
- **Khóa giọng:** dấu vân giọng (x-vector) của Vosk. Ngưỡng tính từ chính các mẫu của bạn (tối thiểu 0,6); mẫu đọc lệch bị bỏ và xin đọc lại.
- **Mở Gemini:** gửi `VOICE_COMMAND` tới app Google để mở bảng nổi Gemini. Từ Android 15, app chạy nền có quyền *Hiển thị trên ứng dụng khác* chỉ được mở màn hình khi đang có cửa sổ nổi, nên HiGG thêm một cửa sổ trong suốt hoàn toàn 1×1 px trong 0,6 giây.
- **Sống được trên OriginOS:** micro luôn mở (OriginOS đóng băng app chạy nền khi ngừng thu âm); lúc Gemini dùng micro, HiGG chỉ nhận im lặng và chờ.

### Quyền riêng tư
Mọi nhận diện chạy trên máy. HiGG không có quyền truy cập Internet, không ghi âm và không gửi âm thanh đi đâu. Biểu tượng micro luôn sáng khi HiGG đang nghe — đó là Android báo có app dùng micro.

### Giới hạn
- Nghe liên tục tốn CPU (đo trên máy thử khoảng 8% của một nhân, phòng yên tĩnh thì ít hơn). Chip nghe tiết kiệm pin như của Jovi chỉ dành cho app hệ thống.
- Nơi rất ồn sẽ nhận kém hơn; hãy nói lại hoặc dùng phím tắt.
- Khóa giọng là bộ lọc cho tiện, không phải bảo mật: bản ghi âm giọng bạn vẫn có thể lọt qua.
- Nhận diện được chỉnh theo cách phát âm tiếng Anh của "Google".

### Build từ mã nguồn
Cần JDK 17 và Android SDK 35.
```bash
./gradlew assembleDebug          # bản debug
./gradlew assembleRelease        # bản phát hành (cần keystore.properties, xem dưới)
```
Muốn ký bản phát hành, tạo `keystore.properties` ở thư mục gốc (không bao giờ commit file này):
```properties
storeFile=/đường/dẫn/higg-release.jks
storePassword=…
keyAlias=higg
keyPassword=…
```

**Công cụ thử im lặng (chỉ có trong bản debug).** Đẩy file WAV 16 kHz mono vào app để chạy qua đúng chuỗi xử lý trên máy thay cho micro:
```bash
tar -C my-wavs -cf - . | adb exec-in run-as com.higg sh -c "mkdir -p files/test/set1 && tar -C files/test/set1 -xf -"
adb shell am broadcast -n com.higg/.TestFeedReceiver -a com.higg.TEST_FEED --es set set1
adb shell run-as com.higg cat files/test/result-set1.txt
```
Chế độ khác: `--es mode enroll` (tạo hồ sơ giọng từ các file), `--es mode launch` (mở Gemini như khi nghe thấy từ khóa). Bản debug còn ghi log: `adb shell run-as com.higg cat files/debug.log`.

### Cấu trúc dự án
| Đường dẫn | Vai trò |
|---|---|
| `HotwordService.kt` | Dịch vụ nền: vòng lặp micro, tạm dừng khi Gemini đang nghe |
| `HotwordPipeline.kt` | Quyết định một câu có mở Gemini không (dùng chung cho dịch vụ và công cụ thử) |
| `HotwordDetector.kt` | Bộ nhận diện Vosk, từ vựng, nghe lại |
| `SpeechGate.kt` | Bỏ qua im lặng để tiết kiệm CPU |
| `VoiceProfile.kt`, `EnrollActivity.kt` | Khóa giọng nói |
| `Gemini.kt` | Mở bảng nổi Gemini từ nền / màn hình khóa |
| `I18n.kt` | Toàn bộ chữ tiếng Việt và tiếng Anh |
| `ui/` | Màn hình Jetpack Compose và hệ thống thiết kế |
| `src/debug/` | Công cụ thử im lặng (không có trong bản phát hành) |

### Ghi công
- Nhận diện giọng nói [Vosk](https://github.com/alphacep/vosk-api) và mô hình `vosk-model-small-en-us-0.15` / `vosk-model-spk-0.4` — Apache 2.0 (xem [trang mô hình Vosk](https://alphacephei.com/vosk/models))
- [JNA](https://github.com/java-native-access/jna) — Apache 2.0 / LGPL 2.1
- [ZXing](https://github.com/zxing/zxing) — Apache 2.0
- Phông [Inter](https://github.com/rsms/inter) — SIL Open Font License 1.1
- Hướng thiết kế giao diện làm bằng skill [ui-ux-pro-max](https://github.com/nextlevelbuilder/ui-ux-pro-max-skill)

HiGG không liên quan tới Google hay vivo. "Google" và "Gemini" là nhãn hiệu của Google LLC.

Giấy phép: chưa quyết định — tạm thời giữ mọi quyền.

### Ủng hộ
Nếu HiGG hữu ích, bạn có thể ủng hộ tác giả ngay trong app (*Ủng hộ HiGG*):
- Chuyển khoản: **Timo by Ban Viet Bank · 9037041009728 · NGUYEN HUU NAM** · nội dung "Ung Ho HiGG"
- PayPal: **starnam23@gmail.com**
