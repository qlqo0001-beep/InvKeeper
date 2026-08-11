# InvKeeper v1.4.2 릴리즈 노트

> 출시일: 2026-08-11
> 주요 변경: **무덤 시스템 성능 개선(TPS/MSPT 안정화) 및 세부 버그 수정**

---

## ⚡ 성능 개선

### 무덤 관련 디스크 저장을 비동기로 전환 (TPS/MSPT 흔들림 해결)
- **증상**: 플레이어가 사망(무덤 생성)하거나, 무덤을 열어 아이템을 회수하거나, 도굴을 시작/취소하거나, 어드민이 히스토리를 열람할 때 서버 TPS/MSPT가 순간적으로 흔들리는 현상이 확인됨.
- **원인**: 위 동작들이 모두 YAML 파일을 **메인 스레드에서 동기적으로** 읽고 쓰고 있었음. 특히 히스토리 조회(`/invkeeper grave history <player>`)는 서버 전체 히스토리 디렉터리를 스캔하며 파일마다 파싱하는 작업이 메인 스레드를 그대로 막고 있었음.
- **수정**: 무덤(`GraveStorage`), 무덤 히스토리(`GraveHistoryStorage`), 도굴 세션(`LootSessionStorage`) 저장/삭제 로직을 전부 비동기로 전환. 아이템 등 게임 상태는 메인 스레드에서 안전하게 스냅샷(복제)한 뒤, 실제 디스크 I/O(파일 쓰기/읽기/삭제, 히스토리 정리 스캔)만 별도 스레드에서 수행하도록 변경. 히스토리 GUI 조회도 비동기 조회 후 결과만 메인 스레드로 되돌려 GUI를 여는 구조로 전환.
- 플러그인 종료(`/stop` 등) 시에는 데이터 유실을 막기 위해 무덤 저장만 예외적으로 기존과 동일한 동기(블로킹) 저장을 유지.

---

## 🐛 버그 수정

### 홀로그램 중복(겹침) 표시
- 서버 재시작 후 무덤 위 홀로그램 텍스트가 겹쳐 보이는 문제 수정. 새 홀로그램을 스폰하기 전, 같은 위치에 남아있던(재시작 전) 엔티티를 실제 월드에서 조회해 자동으로 정리하도록 변경.
- 추가 수정: 조회 시점에 해당 청크가 아직 로드되지 않아 잔여 엔티티를 못 찾고 지나치는 경우가 있어(청크가 로드되지 않으면 그 안의 엔티티는 조회되지 않음), 조회 전에 청크를 먼저 로드하도록 순서를 바로잡음. 이제 서버 재시작만으로 별도 명령 없이 자동으로 겹침이 해소됨.
- `/invkeeper grave reload` 명령도 설정 리로드와 함께 모든 활성 무덤의 홀로그램을 즉시 재동기화하도록 확장(서버를 다시 내리지 않고도 기존에 이미 겹쳐 있던 홀로그램을 정리 가능).

### 도굴 취소 시 무덤 주인 메시지 누락
- 도굴 중인 무덤을 주인이 열어 도굴을 막았을 때, 도굴자에게만 알림이 가고 주인 본인에게는 아무 메시지도 없던 문제 수정. 이제 주인에게도 "무덤 도굴을 막았습니다!" 메시지가 전송됨. `messages.yml`의 `grave-loot-blocked-owner` 키로 문구 수정 가능.

### 아이템 로어 {duration} 미표기
- 도굴 아이템(`items.yml`의 `kind: GRAVE_LOOT_TOOL`, 예: `grave_loot_vanilla`) 로어에서 `{duration}` placeholder가 치환되지 않고 글자 그대로 표시되던 문제 수정. 이제 설정된 `cast-time-seconds` 값(초)으로 정상 치환됨.

---

## 📝 문서
- `config.yml`의 `grave:` 섹션 전체에 상세한 한글 주석 추가(각 옵션의 동작·기본값·placeholder 설명). 코드와 대조하는 과정에서 실제로는 아직 코드에 연결되지 않은 설정(`container.type: CUSTOM_BLOCK`, `title-format`/`death-time-format`, `protection.*`)을 확인해 주석에 명시.

---

## 🧱 영향을 받는 파일
- `GraveStorage`, `GraveHistoryStorage`, `LootSessionStorage`, `GraveHistoryGui`, `GraveManager`
- `GraveHologramManager`, `GraveAdminHandler`, `GraveInteractListener`, `VanillaProtectionItems`
- `config.yml`, `messages.yml`, `pom.xml`, `plugin.yml`
