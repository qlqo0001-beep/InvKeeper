# InvKeeper

> Paper 1.21+ 기반 인벤토리 보호 및 영혼각인(소울바인드) 플러그인

플레이어 사망 시 인벤토리와 경험치 손실을 아이템과 권한/월드 설정으로 세밀하게 제어하고, 아이템에 소유자를 각인하여 거래나 도난을 방지할 수 있습니다. MMOItems 플러그인과의 연동을 지원합니다.

---

## 주요 기능

### 1. 인벤토리 보호권
- **소모형 보호권 (CONSUMABLE_PROTECTION)**: 사망 시 자동으로 1개 소모되어 인벤토리와 경험치를 완전히 보호합니다.
- **시간형 보호권 (TIMED_PROTECTION)**: 우클릭으로 일정 시간 동안 보호 상태를 활성화합니다. 시간 만료 또는 서버 재시작 시 자동으로 해제됩니다.

### 2. 영혼각인 (Soulbind)
- 아이템에 소유자 UUID와 만료 시간을 PDC(PersistentDataContainer)에 저장합니다.
- 소유자만 픽업/이동 가능하며, 타인은 픽업이 차단되고 Shift-click 시 강제 드랍됩니다.
- 호퍼/드로퍼/광산수레 등 자동 이동도 차단합니다.
- 만료 시간이 지나면 자동으로 각인이 제거됩니다.

### 3. 각인 도구
- **영혼각인기 (SOULBIND_TOOL)**: 아이템 위에 드래그-드랍하여 해당 스택 전체에 각인을 적용합니다. `soulbind-apply-duration`으로 적용할 각인의 지속 시간을 설정할 수 있습니다.
- **각인 해제기 (SOULBIND_UNBIND_TOOL)**: 각인된 아이템의 각인을 해제합니다.

### 4. 월드별 / 권한별 드랍율
- `config.yml`의 `rules.world`로 월드별 인벤토리/경험치 드랍 퍼센트를 설정합니다.
- `rules.permissions`로 권한별 우선순위(priority)와 드랍율을 설정할 수 있습니다. 우선순위가 높은 규칙이 우선 적용됩니다.

### 5. MMOItems 연동
- MMOItems로 등록된 아이템도 보호권 및 각인 도구로 사용할 수 있습니다.
- `LiveMMOItem` 및 `NBTItem` API를 리플렉션으로 지원하여 다양한 MMOItems 버전과 호환됩니다.

---

## 명령어

| 명령어 | 설명 | 권한 |
|--------|------|------|
| `/invkeeper status` | 현재 적용 중인 드랍율과 시간제 보호 상태를 확인합니다 | `invkeeper.status` |
| `/invkeeper reload` | 설정 파일을 다시 불러옵니다 | `invkeeper.admin` |
| `/invkeeper give <플레이어> <아이템> [개수]` | 보호 아이템을 지급합니다 | `invkeeper.admin` |
| `/invkeeper soulbind inspect <플레이어>` | 대상 플레이어의 각인된 아이템 목록을 확인합니다 | `invkeeper.admin` |
| `/invkeeper soulbind unbind <플레이어> [슬롯\|all]` | 특정 슬롯이나 전체 인벤토리의 각인을 강제 해제합니다 | `invkeeper.admin` |

**별칭**: `/ik` (예: `/ik status`)

---

## 권한

| 권한 | 설명 |
|------|------|
| `invkeeper.admin` | 모든 관리자 명령어 사용 및 각인 우회 |
| `invkeeper.soulbind.bypass` | 다른 플레이어의 각인된 아이템도 자유롭게 다룰 수 있음 |
| `invkeeper.drop.donor` | 후원자 등급 사망 드랍 규칙 적용 (우선순위 10) |
| `invkeeper.drop.vip` | VIP 등급 사망 드랍 규칙 적용 (우선순위 50) |
| `invkeeper.drop.admin` | 관리자 등급 사망 드랍 규칙 적용 (우선순위 100) |

---

## 설정 파일

플러그인 최초 로드 시 `plugins/InvKeeper/` 폴더에 다음 파일들이 생성됩니다.

### config.yml — 기본 설정

```yaml
# keepInventory 게임룰 강제 고정 옵션
# true로 두면 플러그인 로드/월드 로드 시마다 모든 월드의 keepInventory를 false로 강제 설정합니다.
# 인벤토리 복제 버그 방지를 위해 기본값 true 권장합니다.
force-keep-inventory-false: true

# 영혼각인(소울바인드) 만료 검사를 몇 개의 배치로 나눠서 수행할지 설정합니다.
# 서버는 매초 접속자 전원을 이 값만큼의 그룹으로 나눠, 매초 한 그룹씩만 순환 검사합니다.
# (예: 값이 5면, 100명이 있을 때 매초 약 20명씩만 검사하고 5초에 걸쳐 전원을 한 바퀴 돕니다.)
#
# - 값을 올리면: 한 틱에 검사하는 인원이 줄어 서버 부하(순간 렉)는 낮아지지만,
#   개별 아이템의 각인 만료가 화면에 반영되기까지 걸리는 시간(최대 값 초)이 길어집니다.
# - 값을 내리면: 반영은 빨라지지만 한 틱당 부하가 커집니다.
# 각인 만료 반영이 몇 초 늦어도 게임플레이에는 지장이 없으므로, 인원이 많을수록
# 값을 올려 부하를 분산시키는 것을 권장합니다.
#
# 권장값 (동시 접속자 수 기준):
#   ~20명    : 1   (매초 전원 검사해도 부담 없음)
#   ~50명    : 3
#   ~100명   : 5   (기본값)
#   ~200명   : 8~10
#   ~500명   : 15~20
#
# 최소값은 1입니다. 1보다 작게 설정하면 자동으로 1로 보정됩니다.
soulbind-scan-batches: 5

# 타임존 설정
# 각인 만료 시간 표시에 사용되는 시간대입니다.
# 사용 가능한 타임존 목록: https://en.wikipedia.org/wiki/List_of_tz_database_time_zones
# (TZ database name 컬럼 참고)
# 기본값: Asia/Seoul
timezone: "Asia/Seoul"

rules:
  world:
    # 0~100 사이의 값을 사용하세요. 0은 완전 보호, 100은 모든 아이템/경험치를 드랍합니다.
    default:        { inventory-drop-percent: 0,   exp-drop-percent: 0 }
    world:          { inventory-drop-percent: 50,  exp-drop-percent: 50 }
    world_nether:   { inventory-drop-percent: 70,  exp-drop-percent: 70 }
    world_the_end:  { inventory-drop-percent: 100, exp-drop-percent: 100 }

  permissions:
    # priority가 높은 규칙이 우선 적용됩니다.
    - permission: "invkeeper.drop.donor"
      priority: 10
      inventory-drop-percent: 30
      exp-drop-percent: 30
    - permission: "invkeeper.drop.vip"
      priority: 50
      inventory-drop-percent: 10
      exp-drop-percent: 10
    - permission: "invkeeper.drop.admin"
      priority: 100
      inventory-drop-percent: 0
      exp-drop-percent: 0
```

### items.yml — 아이템 설정

```yaml
items:
  # 소모형 보호권 (바닐라)
  consumable_vanilla:
    kind: CONSUMABLE_PROTECTION
    use-type: vanilla
    vanilla-material: PAPER
    vanilla-name: "&b인벤토리 보호권 &7(소모용)"
    vanilla-lore:
      - "&7소지만 하고 있으면"
      - "&7사망 시 &f자동으로 소모&7되어"
      - "&7인벤토리와 경험치를 &a완전히 보호&7합니다."
      - ""
      - "&e[자동 발동]"
    soulbind:
      enabled: true
      duration-minutes: 0  # 0 = 영구 각인

  # 소모형 보호권 (MMOItems)
  consumable_mmo:
    kind: CONSUMABLE_PROTECTION
    use-type: mmoitems
    mmoitems-type: "consumable"
    mmoitems-id: "인벤토리보호권_소모형"
    soulbind:
      enabled: true
      duration-minutes: 0

  # 시간형 보호권 (바닐라)
  timed_vanilla:
    kind: TIMED_PROTECTION
    use-type: vanilla
    duration-minutes: 30
    vanilla-material: PAPER
    vanilla-name: "&b인벤토리 보호권 &7(시간형)"
    vanilla-lore:
      - "&7우클릭하여 사용하면"
      - "&e{duration}분간&7 사망해도"
      - "&7인벤토리와 경험치를 &a보호&7합니다."
      - ""
      - "&e[우클릭 사용]"
    soulbind:
      enabled: true
      duration-minutes: 0

  # 시간형 보호권 (MMOItems)
  timed_mmo:
    kind: TIMED_PROTECTION
    use-type: mmoitems
    duration-minutes: 30
    mmoitems-type: "consumable"
    mmoitems-id: "인벤토리보호권_시간형"
    soulbind:
      enabled: true
      duration-minutes: 0

  # 영혼각인기 (바닐라)
  soulbind_tool_vanilla:
    kind: SOULBIND_TOOL
    use-type: vanilla
    # 이 도구로 다른 아이템에 적용할 각인의 지속 시간 (분)
    soulbind-apply-duration: 60
    vanilla-material: amethyst_shard
    vanilla-name: "&d영혼 각인기"
    vanilla-lore:
      - "&7다른 아이템 위에 드래그-드랍하여"
      - "&7해당 스택 전체에 영혼각인을 적용합니다."
      - ""
      - "&e[드래그-드랍 사용]"
    soulbind:
      enabled: true
      duration-minutes: 0  # 도구 자체의 각인 (영구)

  # 영혼각인기 (MMOItems)
  soulbind_tool_mmo:
    kind: SOULBIND_TOOL
    use-type: mmoitems
    soulbind-apply-duration: 60
    mmoitems-type: "consumable"
    mmoitems-id: "영혼각인기"
    soulbind:
      enabled: true
      duration-minutes: 0

  # 각인 해제기 (바닐라)
  soulbind_unbind_vanilla:
    kind: SOULBIND_UNBIND_TOOL
    use-type: vanilla
    vanilla-material: SHEARS
    vanilla-name: "&c각인 해제 도구"
    vanilla-lore:
      - "&7각인된 아이템 위에 드래그-드랍하여"
      - "&7영혼각인을 &c해제&7합니다."
      - ""
      - "&e[드래그-드랍 사용]"
    soulbind:
      enabled: true
      duration-minutes: 0

  # 각인 해제기 (MMOItems)
  soulbind_unbind_mmo:
    kind: SOULBIND_UNBIND_TOOL
    use-type: mmoitems
    mmoitems-type: "consumable"
    mmoitems-id: "각인해제기"
    soulbind:
      enabled: true
      duration-minutes: 0
```

**지원하는 아이템 종류 (kind):**

| kind | 설명 |
|------|------|
| `CONSUMABLE_PROTECTION` | 사망 시 자동 소모되는 보호권 |
| `TIMED_PROTECTION` | 우클릭으로 활성화하는 시간제 보호권 |
| `SOULBIND_TOOL` | 아이템에 영혼각인을 적용하는 도구 |
| `SOULBIND_UNBIND_TOOL` | 각인을 해제하는 도구 |

**use-type:**
- `vanilla` — 마인크래프트 기본 아이템 (`vanilla-material`, `vanilla-name`, `vanilla-lore` 사용)
- `mmoitems` — MMOItems 플러그인 아이템 (`mmoitems-type`, `mmoitems-id` 사용)

**soulbind 설정:**
- `enabled: true/false` — 각인 활성화 여부
- `duration-minutes: 0` — 영구 각인 (infinite 자동 적용)
- `duration-minutes: 60` — 60분 후 만료

**soulbind-apply-duration (SOULBIND_TOOL 전용):**
- 이 도구로 다른 아이템에 각인을 적용할 때의 지속 시간 (분 단위)
- `0` 또는 `-1`이면 영구 각인 적용

### messages.yml — 메시지 설정

```yaml
death: "&c인벤토리 {inv_percent}% ({items_dropped}개), 경험치 {exp_percent}% ({exp_dropped}exp)를 잃었습니다."
protected: "&a인벤토리 보호권을 소모하여 아무것도 잃지 않았습니다!"
timed-protected: "&a인벤토리 보호 상태 임으로 아무것도 잃지 않았습니다! (남은 시간: {remaining})"
timed-already-active: "&e이미 보호 상태입니다. (남은 시간: {remaining})"
timed-activated: "&a인벤토리 보호가 {duration}분간 활성화되었습니다."
timed-remaining-five-minutes: "&e보호 상태가 5분 남았습니다. 남은 시간: {remaining}"
timed-remaining-one-minute: "&e보호 상태가 1분 남았습니다. 남은 시간: {remaining}"
soulbound-applied: "&a아이템에 영혼각인이 적용되었습니다. (대상: {owner}, 지속시간: {remaining})"
soulbound-extended: "&a이미 각인된 아이템의 유지시간이 연장되었습니다. (남은 시간: {remaining})"
soulbound-unbound: "&a아이템의 영혼각인이 해제되었습니다."
soulbound-cant-pickup: "&c이 아이템은 {owner}의 각인 아이템입니다. 획득할 수 없습니다."
soulbound-forced-dropped: "&e이 플레이어가 소유자가 아니라서 아이템을 강제로 드랍했습니다."
soulbound-already-infinite: "&e이 아이템은 이미 무한 각인 상태입니다."
soulbound-lore-format: "&7각인: &b{owner} &7| 만료: &b{expiry}"
time-format: "{minutes}분 {seconds_padded}초"
```

**Placeholders:**
- `{inv_percent}` — 실제 드랍된 인벤토리 퍼센트
- `{exp_percent}` — 설정된 경험치 드랍 퍼센트
- `{items_dropped}` — 실제 드랍된 아이템 개수
- `{exp_dropped}` — 실제 드랍된 경험치 양
- `{remaining}` — 남은 시간 (time-format 기반)
- `{duration}` — 설정된 시간 (분)
- `{owner}` — 각인 소유자 이름
- `{expiry}` — 각인 만료 시간 (timezone 기반)
- `{minutes}`, `{seconds}`, `{seconds_padded}`, `{total_seconds}` — 시간 포맷용

---

## 영혼각인(Soulbind) 시스템 상세

### 저장 방식
- 각인 정보는 아이템의 `PersistentDataContainer(PDC)`에 저장됩니다.
- `soulbind_owner` — 소유자 UUID (STRING)
- `soulbind_expiry` — 만료 epoch millis (LONG), `-1`은 영구
- `soulbind_lore_text` — 현재 적용된 로어 텍스트 (STRING, 서버 재시작 후에도 로어 중복 방지용)

### 만료 처리
- **이벤트 기반**: 아이템 픽업, 인벤토리 클릭/드래그, 사망 시 각인 만료를 검사하여 자동 제거합니다.
- **주기적 검사**: `ProtectionAlertManager`가 1초마다 배치 단위로 접속자 인벤토리를 스캔하여 만료된 각인을 제거합니다. `soulbind-scan-batches` 값으로 성능을 조절할 수 있습니다.

### 제한 사항
- **픽업 제한**: 각인된 아이템은 소유자만 픽업할 수 있습니다. `invkeeper.soulbind.bypass` 권한이 있으면 우회 가능.
- **이동 제한**: Shift-click, 드래그 이동 시 비소유자의 각인 아이템은 강제로 바닥에 드랍됩니다.
- **자동 이동 차단**: 호퍼, 드로퍼, 광산수레 등 자동 아이템 이동 시 각인 아이템을 차단합니다.

---

## 아키텍처

```
com.invkeeper
├── InvKeeperPlugin.java          # 메인 클래스, 리스너 등록, 설정 로드
├── command/
│   └── InvKeeperCommand.java     # /invkeeper 명령어 처리
├── config/
│   ├── ConfigManager.java        # 3개 설정 파일 로드 및 규칙 해석
│   ├── WorldRule.java             # 월드별 드랍율
│   ├── PermissionRule.java        # 권한별 드랍율 (priority 기반)
│   └── ProtectionItemConfig.java  # 아이템 설정 (4가지 Kind)
├── listener/
│   ├── PlayerDeathListener.java   # 사망 처리, 보호권 소모, 드랍
│   ├── ProtectionItemUseListener.java # 시간형 보호권 우클릭 사용
│   ├── SoulbindInventoryListener.java # 각인 도구 드래그-드랍
│   ├── SoulbindPickupListener.java    # 각인 아이템 픽업 제한
│   ├── SoulbindTransferListener.java  # 호퍼/Shift-click/드래그 이동 제한
│   └── WorldLoadListener.java         # 월드 로드 시 keepInventory=false 강제
├── protection/
│   ├── ProtectionManager.java     # 보호권 소모/매칭 중앙 관리
│   ├── MMOItemsHook.java          # MMOItems 리플렉션 연동
│   ├── VanillaProtectionItems.java # 바닐라 아이템 PDC 기반 생성/식별
│   ├── TimedProtectionStore.java  # PDC 기반 시간 보호 저장
│   └── ProtectionAlertManager.java # 1초 주기 태스크 (알림 + 각인 만료 검사)
├── soulbind/
│   └── SoulbindManager.java       # PDC 기반 각인 관리 (적용/제거/만료/로어)
└── util/
    └── MessageUtil.java           # 색상 변환, 시간 포맷팅, 타임존
```

### 데이터 흐름
1. **플러그인 로드**: `ConfigManager`가 `config.yml`, `items.yml`, `messages.yml`을 로드합니다.
2. **사망 발생**: `PlayerDeathListener`가 보호권을 확인하고, 없으면 드랍율에 따라 아이템과 경험치를 드랍합니다.
3. **보호권 사용**: 소모형은 자동 소모, 시간형은 우클릭으로 `TimedProtectionStore`에 만료 시간을 저장합니다.
4. **각인 적용**: `SoulbindInventoryListener`가 각인 도구 사용 시 `SoulbindManager`를 통해 PDC에 각인 정보를 저장합니다.
5. **각인 제한**: 픽업/이동 리스너가 각인 상태를 확인하여 소유자 외에는 차단합니다.
6. **만료 처리**: 주기적 스캔과 이벤트 기반 검사로 만료된 각인을 자동 제거합니다.

### 메모리 관리
- `InvKeeperPlugin.CleanupListener`가 플레이어 접속 종료 시 `ProtectionAlertManager`의 리마인더 상태를 정리하여 메모리 누수를 방지합니다.
- `TimedProtectionStore`는 PDC를 사용하므로 별도 정리 없이도 플레이어 로그아웃 시에도 보호 상태가 유지됩니다.

---

## 빌드 및 의존성

- **Java**: 21 이상
- **서버 코어**: Paper 1.21 이상 (또는 호환되는 코어)
- **의존성**:
  - Paper API (provided)
  - MMOItems-API 6.9.5-SNAPSHOT (선택 사항, provided)
  - MythicLib-dist 1.6.2-SNAPSHOT (선택 사항, provided)

### 빌드 명령어
```bash
mvn clean package
```

빌드된 JAR 파일은 `target/InvKeeper.jar`에 생성됩니다.

---

## 다운로드 및 적용

1. [릴리스 페이지](https://github.com/qlqo0001-beep/InvKeeper/releases)에서 최신 JAR 파일을 다운로드합니다.
2. 서버의 `plugins` 폴더에 넣습니다.
3. 서버를 재시작하면 `plugins/InvKeeper/` 폴더에 설정 파일이 생성됩니다.
4. `items.yml`과 `messages.yml`을 필요에 맞게 수정합니다.
5. `/invkeeper reload`로 설정을 적용합니다.

---

## 배치 파일 사용법

로컬에만 존재하는 배치 파일들(`.gitignore`에서 제외됨)입니다.

### build.bat — 원클릭 빌드

더블클릭만 하면 Maven으로 플러그인을 자동 빌드합니다. `apache-maven-*` 폴더가 있으면 해당 Maven을, 없으면 시스템 `mvn`을 사용합니다. 빌드 결과는 `target/InvKeeper.jar`에 생성됩니다.

### push.bat — 깃헙 업로드

더블클릭 후 커밋 메시지만 입력하면 됩니다.

```
1. push.bat 실행
2. 커밋 메시지 입력 (예: "설정 파일 정리 및 README 업데이트")
3. 엔터 → 자동으로 git add → git commit → git push origin main 실행
```

**첫 설정 시 필요** (원격 저장소가 없을 때만 1회):
```
git remote add origin https://github.com/qlqo0001-beep/InvKeeper.git
```

---

## 라이선스

이 프로젝트는 MIT 라이선스를 따릅니다.
