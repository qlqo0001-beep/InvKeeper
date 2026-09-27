<img width="2400" height="1260" alt="invkeeper_banner" src="https://github.com/user-attachments/assets/f6e4ca3a-9a45-4c37-af81-38068a0a47be" />


# InvKeeper

> Paper 1.21+ 기반 인벤토리 보호·무덤(Grave)·영혼각인(소울바인드) 플러그인

플레이어 사망 시 인벤토리와 경험치 손실을 아이템과 권한/월드 설정으로 세밀하게 제어하고, 아이템에 소유자를 각인하여 거래나 도난을 방지할 수 있습니다. 무덤 시스템을 켜면 사망 시 잃는 아이템을 바닥에 흩뿌리는 대신 사망 지점에 무덤을 생성해 보관하며, 주인은 언제든 회수하고 다른 플레이어는 도굴 도구로 일정 시간 시전해야 열 수 있습니다. MMOItems 플러그인과의 연동을 지원합니다.

---

## 주요 기능

### 1. 인벤토리 보호권
- **소모형 보호권 (CONSUMABLE_PROTECTION)**: 사망 시 자동으로 1개 소모되어 인벤토리와 경험치를 완전히 보호합니다.
- **시간형 보호권 (TIMED_PROTECTION)**: 우클릭으로 일정 시간 동안 보호 상태를 활성화합니다. 시간 만료 또는 서버 재시작 시 자동으로 해제됩니다.

### 2. 영혼각인 (Soulbind)
- 아이템에 소유자 UUID와 만료 시간/스택을 PDC(PersistentDataContainer)에 저장합니다.
- 소유자만 픽업/이동 가능하며, 타인은 픽업이 차단되고 Shift-click 시 강제 드랍됩니다.
- 호퍼/드로퍼/광산수레 등 자동 이동도 차단합니다.
- **시간형**: 만료 시간이 지나면 자동으로 각인이 제거됩니다.
- **스택형**: 사망할 때마다 스택이 1씩 감소하며, 0이 되면 각인이 해제됩니다.

### 3. 각인 도구
- **시간형 각인기 (SOULBIND_TOOL_TIME)**: 아이템 위에 드래그-드랍하여 시간 기반 각인을 적용합니다. `soulbind-duration`으로 지속 시간(분)을 설정합니다.
- **스택형 각인기 (SOULBIND_TOOL_STACK)**: 아이템 위에 드래그-드랍하여 스택 기반 각인을 적용합니다. `soulbind-stacks`로 스택 수를 설정합니다.
- **각인 해제기 (SOULBIND_UNBIND_TOOL)**: 각인된 아이템의 각인을 해제합니다.
- **참고**: 시간형 각인과 스택형 각인은 동시에 적용할 수 없습니다. 이미 각인된 아이템에 다른 타입의 각인을 적용하면 거부됩니다.

### 4. 월드별 / 권한별 드랍율
- `config.yml`의 `rules.world`로 월드별 인벤토리/경험치 드랍 퍼센트를 설정합니다.
- `rules.permissions`로 권한별 우선순위(priority)와 드랍율을 설정할 수 있습니다. 우선순위가 높은 규칙이 우선 적용됩니다.

### 5. MMOItems 연동
- MMOItems로 등록된 아이템도 보호권 및 각인 도구로 사용할 수 있습니다.
- `LiveMMOItem` 및 `NBTItem` API를 리플렉션으로 지원하여 다양한 MMOItems 버전과 호환됩니다.

### 6. 무덤(Grave) 시스템
- 사망 시 잃는 아이템/경험치를 바닥에 드랍하는 대신, 사망 지점에 무덤(기본: 통 블록)을 생성해 보관합니다. (`config.yml`의 `grave.enabled`로 on/off, 기본 켜짐)
- 위 월드별/권한별 드랍율 설정은 무덤이 켜져 있어도 그대로 "얼마나 잃는지" 계산에 쓰이며, 잃는 몫이 바닥 대신 무덤에 담깁니다.
- **가상 GUI**: 실제 통 인벤토리가 아니라 플러그인이 관리하는 가상 GUI로 열립니다. 장비 4칸·왼손 1칸·인벤토리 36칸이 원래 슬롯 그대로 표시되고, 남은 경험치는 경험치병 아이콘으로 표시되어 클릭 시 회수됩니다. "모두 회수" 버튼(기본 슬롯 49, 맨 아랫줄 중앙)으로 한 번에 전량 회수하면 GUI가 자동으로 닫힙니다. 미사용 슬롯은 이름 없는 회색 색유리판으로 채워집니다.
- **도굴**: 타인의 무덤은 기본적으로 열 수 없으며, 도굴 도구(`grave_loot_vanilla` 등, kind: `GRAVE_LOOT_TOOL`)를 사용해 일정 시간(기본 300초) 시전해야 열 수 있습니다. 시전 중 무덤 주인이 무덤을 열면 도굴이 즉시 취소되고 도굴자·주인 모두에게 안내 메시지가 전송됩니다. 시전 상태는 서버 재시작 후에도 남은 시간 그대로 이어집니다.
- **만료**: `grave.expire` 설정으로 무덤이 일정 시간 후 자동 소멸하도록 할 수 있고, 권한별로 다른 만료 시간(`invkeeper.grave.time.vip`, `invkeeper.grave.time.vvip`)을 부여할 수 있습니다.
- **홀로그램**: 무덤 위에 TextDisplay 엔티티로 소유자/남은 시간/도굴 상태를 실시간 표시합니다(외부 홀로그램 플러그인 불필요). 서버 재시작 시에도 중복 없이 자동으로 다시 스폰됩니다.
- **히스토리**: 무덤이 사라질 때(회수/도굴/만료) 사망 시점 원본 아이템 스냅샷과 회수 상태가 기록됩니다. 어드민은 `/invkeeper grave history <player>`로 조회해 좌클릭으로 무덤 위치까지 텔레포트, 우클릭으로 그 당시 상태 그대로의 가상 GUI를 열어 아이템을 회수할 수 있습니다. 회수됨(노랑)/도굴됨(빨강)/미회수(연두) 상태가 색유리판으로 구분됩니다.
- **성능**: 무덤 관련 디스크 저장(생성/회수/도굴/히스토리)은 모두 비동기로 처리되어, 사망이나 무덤 상호작용이 서버 TPS/MSPT에 영향을 주지 않습니다.

---

## 명령어

| 명령어 | 설명 | 권한 |
|--------|------|------|
| `/invkeeper status` | 현재 적용 중인 드랍율과 시간제 보호 상태를 확인합니다 | `invkeeper.status` |
| `/invkeeper reload` | 설정 파일을 다시 불러옵니다 | `invkeeper.admin` |
| `/invkeeper give <플레이어> <아이템> [개수]` | 보호 아이템을 지급합니다 | `invkeeper.admin` |
| `/invkeeper soulbind inspect <플레이어>` | 대상 플레이어의 각인된 아이템 목록을 확인합니다 | `invkeeper.admin` |
| `/invkeeper soulbind unbind <플레이어> [슬롯\|all]` | 특정 슬롯이나 전체 인벤토리의 각인을 강제 해제합니다 | `invkeeper.admin` |
| `/invkeeper grave list` | 서버 내 활성 무덤 목록을 확인합니다 (페이지 지원) | `invkeeper.grave.admin` |
| `/invkeeper grave history <플레이어>` | 대상 플레이어의 무덤 이력을 조회합니다 (좌클릭: 텔레포트, 우클릭: 원상태 가상 GUI) | `invkeeper.grave.admin` |
| `/invkeeper grave reload` | 무덤 설정을 다시 불러오고 홀로그램을 재동기화합니다 | `invkeeper.grave.admin` |

**별칭**: `/ik` (예: `/ik status`)

---

## 권한

| 권한 | 설명 |
|------|------|
| `invkeeper.status` | 자신의 InvKeeper 보호 상태를 확인할 수 있음 |
| `invkeeper.admin` | 모든 관리자 명령어 사용 및 각인 우회 |
| `invkeeper.soulbind.bypass` | 다른 플레이어의 각인된 아이템도 자유롭게 다룰 수 있음 |
| `invkeeper.drop.1` ~ `invkeeper.drop.11` | 등급별 사망 드랍 규칙 적용 (`config.yml`의 `rules.permissions`에서 등급별 우선순위/드랍율 설정, 기본값은 숫자가 클수록 우선순위 높음) |
| `invkeeper.grave.admin` | 무덤 관리 명령어 사용 (`list`, `history`, `reload`) |
| `invkeeper.grave.bypass` | 도굴 도구 없이 타인의 무덤을 강제로 열람 (도굴 진행 중이면 취소시킴) |
| `invkeeper.grave.time.vip` / `invkeeper.grave.time.vvip` | 무덤 만료 시간 등급 (`grave.expire.permission-overrides`에서 설정) |

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

# 아이템에 할당 가능한 최대 스택 수
# 스택형 각인 도구로 아이템에 각인을 적용할 때, 이 값을 초과하는 스택은
# 자동으로 이 값으로 제한됩니다.
# - -1: 무제한 (제한 없음, 기본값)
# - 양수: 해당 값이 최대 스택 수 (도구의 soulbind-stacks가 이 값보다 크면 제한됨)
max-soulbind-stack: -1

rules:
  world:
    # 0~100 사이의 값을 사용하세요. 0은 완전 보호, 100은 모든 아이템/경험치를 드랍합니다.
    default:        { inventory-drop-percent: 0,   exp-drop-percent: 0 }
    world:          { inventory-drop-percent: 50,  exp-drop-percent: 50 }
    world_nether:   { inventory-drop-percent: 70,  exp-drop-percent: 70 }
    world_the_end:  { inventory-drop-percent: 100, exp-drop-percent: 100 }

  permissions:
    # priority가 높은 규칙이 우선 적용됩니다. (등급이 여러 개면 아래처럼 촘촘하게 나눌 수 있습니다)
    - permission: "invkeeper.drop.1"
      priority: 10
      inventory-drop-percent: 100
      exp-drop-percent: 100
    - permission: "invkeeper.drop.6"
      priority: 60
      inventory-drop-percent: 50
      exp-drop-percent: 50
    - permission: "invkeeper.drop.11"
      priority: 100
      inventory-drop-percent: 0
      exp-drop-percent: 0
```

전체 등급(1~11)은 실제 `config.yml` 파일에 모두 정의되어 있으며, 등급이 높을수록(11에 가까울수록) 드랍율이 낮아지도록 기본 설정되어 있습니다.

`config.yml`에는 위 설정 외에 무덤 시스템 전용 `grave:` 섹션이 있습니다(무덤 on/off, 컨테이너 블록, 만료, 히스토리 보관 기간, 홀로그램 문구, 보호 옵션 등). 각 항목은 파일 안에 상세한 한글 주석으로 설명되어 있으니, 실제 설정 시에는 `config.yml`을 직접 열어 확인하는 것을 권장합니다.

```yaml
grave:
  enabled: true
  container: { type: VANILLA, vanilla-material: BARREL }
  max-graves-per-player: 5   # 0 이하 = 무제한
  expire:
    enabled: true
    default-seconds: 3600     # 0 또는 -1 = 무제한
    permission-overrides:
      - permission: "invkeeper.grave.time.vip"
        priority: 10
        seconds: 7200
      - permission: "invkeeper.grave.time.vvip"
        priority: 20
        seconds: -1
  history: { retention-days: 30, max-entries-per-player: 50 }   # 둘 다 0 이하 = 무제한
  disabled-worlds: []
  hologram: { enabled: true, offset-y: 1.0, update-interval-ticks: 20 }
  protection: { prevent-block-break: true, prevent-explosion: true, prevent-piston: true, prevent-hopper: true }
```

### gui.yml — 무덤 GUI 설정

```yaml
grave-gui:
  title: "{player} 의 무덤"
  exp-bottle:
    material: EXPERIENCE_BOTTLE
    name: "&e경험치 {amount}exp"
    lore:
      - "&7클릭하여 경험치를 회수합니다."
  recover-all:
    slot: 49
    material: NETHER_STAR
    name: "&a&l모두 회수"
    lore:
      - "&7클릭 시 장비, 왼손, 인벤토리의 모든 아이템과"
      - "&7경험치를 한 번에 회수합니다."
  recovery-status:
    owner: "&a&l[ 주인 회수 완료 ]"
    looter: "&c&l[ 도굴꾼 회수 ]"
    none: "&7[ 미회수 ]"
```

- `title` — `{player}`로 무덤 주인 이름 치환
- `exp-bottle` — 경험치가 남아있을 때 표시되는 아이콘 (`{amount}` = 남은 경험치)
- `recover-all` — "모두 회수" 버튼의 슬롯/아이콘/이름/설명 (기본 슬롯 49, 나머지 미사용 슬롯은 이름 없는 회색 색유리판으로 채워짐)
- `recovery-status` — 히스토리 GUI 로어에 표시되는 회수 상태 문구

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
      duration-minutes: 0

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

  # 시간형 각인기 (바닐라)
  soulbind_tool_time_vanilla:
    kind: SOULBIND_TOOL_TIME
    use-type: vanilla
    soulbind-duration: 60
    vanilla-material: amethyst_shard
    vanilla-name: "&d시간형 각인기"
    vanilla-lore:
      - "&7다른 아이템 위에 드래그-드랍하여"
      - "&7해당 스택 전체에 시간형 영혼각인을 적용합니다."
      - ""
      - "&e[드래그-드랍 사용]"
    soulbind:
      enabled: true
      duration-minutes: 0

  # 시간형 각인기 (MMOItems)
  soulbind_tool_time_mmo:
    kind: SOULBIND_TOOL_TIME
    use-type: mmoitems
    soulbind-duration: 60
    mmoitems-type: "consumable"
    mmoitems-id: "시간형각인기"
    soulbind:
      enabled: true
      duration-minutes: 0

  # 스택형 각인기 (바닐라)
  soulbind_tool_stack_vanilla:
    kind: SOULBIND_TOOL_STACK
    use-type: vanilla
    soulbind-stacks: 1
    vanilla-material: heart_of_the_sea
    vanilla-name: "&b스택형 각인기"
    vanilla-lore:
      - "&7다른 아이템 위에 드래그-드랍하여"
      - "&7해당 스택 전체에 스택형 영혼각인을 적용합니다."
      - "&7(사망 시 스택 1 소모, 0이면 해제)"
      - ""
      - "&e[드래그-드랍 사용]"
    soulbind:
      enabled: true
      duration-minutes: 0

  # 스택형 각인기 (MMOItems)
  soulbind_tool_stack_mmo:
    kind: SOULBIND_TOOL_STACK
    use-type: mmoitems
    soulbind-stacks: 1
    mmoitems-type: "consumable"
    mmoitems-id: "스택형각인기"
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

  # 도굴 도구 (바닐라) — 무덤 시스템 전용
  grave_loot_vanilla:
    kind: GRAVE_LOOT_TOOL
    cast-time-seconds: 300
    use-type: vanilla
    vanilla-material: TRIPWIRE_HOOK
    vanilla-name: "&5도굴 도구"
    vanilla-lore:
      - "&7무덤을 우클릭하면"
      - "&7{duration}초 후 무덤을 도굴할 수 있습니다."
```

**지원하는 아이템 종류 (kind):**

| kind | 설명 |
|------|------|
| `CONSUMABLE_PROTECTION` | 사망 시 자동 소모되는 보호권 |
| `TIMED_PROTECTION` | 우클릭으로 활성화하는 시간제 보호권 |
| `SOULBIND_TOOL_TIME` | 시간형 각인 도구 (`soulbind-duration`으로 시간 설정) |
| `SOULBIND_TOOL_STACK` | 스택형 각인 도구 (`soulbind-stacks`로 스택 설정) |
| `SOULBIND_UNBIND_TOOL` | 각인 해제 도구 |
| `GRAVE_LOOT_TOOL` | 무덤 도굴 도구 (`cast-time-seconds`로 시전 시간(초) 설정, 로어의 `{duration}`으로 표시) |

**use-type:**
- `vanilla` — 마인크래프트 기본 아이템 (`vanilla-material`, `vanilla-name`, `vanilla-lore` 사용)
- `mmoitems` — MMOItems 플러그인 아이템 (`mmoitems-type`, `mmoitems-id` 사용)

**각인 도구 설정:**
- `SOULBIND_TOOL_TIME`:
  - `soulbind-duration`: 각인 지속 시간 (분 단위). `0` 또는 `-1` = 영구, 양수 = 해당 분 후 만료
- `SOULBIND_TOOL_STACK`:
  - `soulbind-stacks`: 각인 스택 수. `-1` = 무한, `1` = 사망 1회 버팀, 양수 = 해당 횟수

**soulbind (도구 자체 각인 설정):**
- `enabled: true/false` — 각인 활성화 여부
- `duration-minutes: 0` — 영구 각인 (infinite 자동 적용)
- `duration-minutes: 60` — 60분 후 만료

**참고:** 시간형 각인과 스택형 각인은 동시에 적용할 수 없습니다. 각인 도구로 다른 타입의 각인이 적용된 아이템에 적용 시도 시 거부 메시지가 출력됩니다.

### messages.yml — 메시지 설정

```yaml
death: "&c인벤토리 {inv_percent}% ({items_dropped}개), 경험치 {exp_percent}% ({exp_dropped}exp)를 잃었습니다."
protected: "&a인벤토리 보호권을 소모하여 아무것도 잃지 않았습니다!"
timed-protected: "&a인벤토리 보호 상태 임으로 아무것도 잃지 않았습니다! (남은 시간: {remaining})"
timed-already-active: "&e이미 보호 상태입니다. (남은 시간: {remaining})"
timed-activated: "&a인벤토리 보호가 {duration}분간 활성화되었습니다."
# 시간형 보호권 남은 시간 알림
# 키: 남은 시간(초), 값: 메시지. 원하는 만큼 추가/삭제할 수 있습니다.
# 예) 10: "&c보호 종료 10초 전입니다!" / 3: "&c3초 후 보호가 종료됩니다!"
# - 순서는 상관없으며, 남은 시간이 긴 알림부터 전송됩니다.
# - 1초마다 확인하므로 최대 1초 정도 늦게 뜰 수 있습니다. (예: 3초 알림은 2~3초 남았을 때 전송)
#   정확한 남은 시간이 필요하면 {remaining} 을 사용하세요. ({remaining} 표시 형식은 time-format 을 따릅니다)
# - 수정 후 /invkeeper reload 로 바로 적용됩니다.
timed-remaining-alerts:
  300: "&e보호 상태가 5분 남았습니다. 남은 시간: {remaining}"
  60: "&e보호 상태가 1분 남았습니다. 남은 시간: {remaining}"
# 시간형 보호권 사용 시간 종료 알림 ("" 로 비워두면 알림을 보내지 않습니다)
timed-expired: "&c인벤토리 보호가 종료되었습니다. &7이제부터 사망 시 아이템을 잃을 수 있습니다."
# 접속하지 않은 동안 보호 시간이 종료된 경우, 다음 접속 시 1회 알림 ("" 로 비워두면 알림을 보내지 않습니다)
# {expired_at}: 종료 시각 (config.yml의 timezone 기준, 예: 2026-09-27 18:30)
timed-expired-offline: "&c자리를 비운 사이 인벤토리 보호가 종료되었습니다. &8(종료: {expired_at}) &7사망 시 아이템을 잃을 수 있으니 주의하세요."
soulbound-applied: "&a아이템에 영혼각인이 적용되었습니다. (대상: {owner}, 지속시간: {remaining})"
soulbound-extended: "&a이미 각인된 아이템의 유지시간이 연장되었습니다. (남은 시간: {remaining})"
soulbound-unbound: "&a아이템의 영혼각인이 해제되었습니다."
soulbound-cant-pickup: "&c이 아이템은 {owner}의 각인 아이템입니다. 획득할 수 없습니다."
soulbound-forced-dropped: "&e이 플레이어가 소유자가 아니라서 아이템을 강제로 드랍했습니다."
soulbound-already-infinite: "&e이 아이템은 이미 무한 각인 상태입니다."
soulbound-lore-format: "&7각인: &b{owner} &7| 만료: &b{expiry}"
soulbound-lore-format-stack: "&7각인: &b{owner} &7| 횟수: &b{stacks}"
soulbound-conflict-type: "&c이 아이템은 {type} 각인 상태입니다. 다른 타입의 각인을 적용할 수 없습니다."
time-format: "{minutes}분 {seconds_padded}초"
```

무덤 시스템 관련 메시지는 파일 하단에 별도 섹션으로 있습니다:

```yaml
grave-created: "&e무덤이 생성되었습니다. ({world}, {x}, {y}, {z})"
grave-not-owner: "&c이 무덤의 주인은 {owner} 입니다."
grave-opened: "&a무덤을 열었습니다."
grave-max-reached: "&c무덤 최대 개수({max})에 도달하여 가장 오래된 무덤이 정리되었습니다."
grave-expired: "&7({world}, {x}, {y}, {z}) 의 무덤이 시간이 지나 사라졌습니다."
grave-fully-recovered: "&e무덤의 모든 아이템을 회수하여 무덤이 사라졌습니다."
grave-loot-start-caster: "&a도굴을 시작합니다. {seconds}초 후 무덤이 열립니다."
grave-loot-start-owner-alert: "&c누군가 당신의 무덤을 도굴중입니다!"
grave-loot-cancelled: "&c도굴중, {owner}이(가) 무덤을 확인하여 도굴이 취소되었습니다."
grave-loot-blocked-owner: "&a무덤 도굴을 막았습니다!"
grave-loot-already-in-progress: "&c이미 다른 플레이어가 이 무덤을 도굴하고 있습니다."
grave-loot-self-blocked: "&c자신의 무덤은 도굴할 수 없습니다."
grave-loot-complete-caster: "&a도굴이 완료되었습니다! 이제 무덤을 열 수 있습니다."
grave-loot-item-required: "&c도굴 아이템이 필요합니다."
grave-history-emptied: "&a히스토리에 남아있던 아이템을 모두 회수했습니다. (히스토리는 유지됩니다)"
grave-history-no-items: "&c이 무덤에는 남아있는 아이템이 없습니다."
```

**Placeholders:**
- `{inv_percent}` — 실제 드랍된 인벤토리 퍼센트
- `{exp_percent}` — 설정된 경험치 드랍 퍼센트
- `{items_dropped}` — 실제 드랍된 아이템 개수
- `{exp_dropped}` — 실제 드랍된 경험치 양
- `{remaining}` — 남은 시간 (time-format 기반) 또는 스택형의 경우 "N회"
- `{duration}` — 설정된 시간 (분)
- `{owner}` — 각인 소유자 이름
- `{expiry}` — 각인 만료 시간 (timezone 기반, 시간형)
- `{stacks}` — 남은 스택 수 (스택형)
- `{type}` — 각인 타입 이름 (충돌 메시지용)
- `{minutes}`, `{seconds}`, `{seconds_padded}`, `{total_seconds}` — 시간 포맷용 (grave-loot-start-caster의 `{seconds}`는 도굴 시전 시간(초)을 그대로 넣어줌)
- `{world}`, `{x}`, `{y}`, `{z}` — 무덤 좌표 (grave-created, grave-expired)
- `{max}` — 플레이어당 무덤 최대 개수 (grave-max-reached)

---

## 영혼각인(Soulbind) 시스템 상세

### 저장 방식
- 각인 정보는 아이템의 `PersistentDataContainer(PDC)`에 저장됩니다.
- `soulbind_owner` — 소유자 UUID (STRING)
- `soulbind_expiry` — 만료 epoch millis (LONG), `-1`은 영구 (시간형)
- `soulbind_stacks` — 남은 스택 수 (INTEGER), `-1`은 무한 (스택형)
- `soulbind_lore_text` — 현재 적용된 로어 텍스트 (STRING, 서버 재시작 후에도 로어 중복 방지용)

### 각인 타입
- **시간형 (TIME)**: `soulbind_expiry`가 설정됨. 만료 시간이 지나면 자동 해제.
- **스택형 (STACK)**: `soulbind_stacks`가 설정됨. 사망 시 1씩 감소, 0이 되면 해제.
- **상호 배타적**: 한 아이템에 두 타입이 동시에 존재할 수 없음. 각인 도구 적용 시 타입 충돌이 발생하면 거부됨.

### 만료 처리
- **이벤트 기반**: 아이템 픽업, 인벤토리 클릭/드래그, 사망 시 각인 만료/스택 소진을 검사하여 자동 제거합니다.
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
│   ├── InvKeeperCommand.java     # /invkeeper 명령어 처리 (status|reload|give|soulbind|grave)
│   └── handler/
│       ├── GiveHandler.java          # give 서브커맨드
│       ├── SoulbindAdminHandler.java # soulbind 서브커맨드
│       └── GraveAdminHandler.java    # grave 서브커맨드 (list/history/reload)
├── config/
│   ├── ConfigManager.java        # config.yml/items.yml/messages.yml/gui.yml 로드 및 규칙 해석
│   ├── WorldRule.java             # 월드별 드랍율
│   ├── PermissionRule.java        # 권한별 드랍율 (priority 기반)
│   ├── ProtectionItemConfig.java  # 아이템 설정 (CONSUMABLE/TIMED/SOULBIND_*/GRAVE_LOOT_TOOL)
│   └── GraveContainerConfig.java / GraveGuiConfig.java / GraveHologramConfig.java / GraveExpireRule.java
│       # 무덤 컨테이너/GUI/홀로그램/만료 설정
├── listener/
│   ├── PlayerDeathListener.java   # 사망 처리 (무덤 활성화 시 무덤 생성 경로로 분기)
│   ├── ProtectionItemUseListener.java # 시간형 보호권 우클릭 사용
│   ├── SoulbindInventoryListener.java # 각인 도구 드래그-드랍 (TIME/STACK 분기)
│   ├── SoulbindPickupListener.java    # 각인 아이템 픽업 제한
│   ├── SoulbindTransferListener.java  # 호퍼/Shift-click/드래그 이동 제한
│   ├── WorldLoadListener.java         # 월드 로드 시 keepInventory=false 강제
│   ├── GraveInteractListener.java     # 무덤 우클릭 (오픈/도굴 시전/도굴 취소)
│   ├── GraveGuiListener.java          # 무덤 가상 GUI 클릭/드래그/닫기
│   ├── GraveAdminGuiListener.java     # 무덤 목록/히스토리 GUI 클릭
│   └── GraveProtectionListener.java   # 무덤 블록 파괴/폭발/피스톤 방지, 청크 복구
├── protection/
│   ├── ProtectionManager.java     # 보호권 소모/매칭 중앙 관리
│   ├── MMOItemsHook.java          # MMOItems 리플렉션 연동
│   ├── VanillaProtectionItems.java # 바닐라 아이템 PDC 기반 생성/식별
│   ├── TimedProtectionStore.java  # PDC 기반 시간 보호 저장
│   └── ProtectionAlertManager.java # 1초 주기 태스크 (알림 + 각인 만료 검사)
├── soulbind/
│   └── SoulbindManager.java       # PDC 기반 각인 관리 (TIME/STACK, 적용/제거/만료/로어)
├── grave/
│   ├── Grave.java / GraveContents.java / GraveState.java / RecoveryType.java  # 데이터 모델
│   ├── GraveManager.java          # 무덤 생성/조회/삭제/히스토리 뷰 중앙 관리
│   ├── GraveStorage.java / GraveHistoryStorage.java / LootSessionStorage.java # YAML 영속화 (비동기 저장)
│   ├── GraveHologramManager.java  # TextDisplay 홀로그램 관리 (중복 방지)
│   ├── GraveTickManager.java      # 1초 주기 만료/홀로그램 스케줄러
│   ├── LootSessionManager.java    # 도굴 시전 타이머 관리
│   ├── GraveInventoryView.java    # 무덤 가상 GUI
│   ├── GraveListGui.java / GraveHistoryGui.java # 어드민 목록/히스토리 GUI
│   └── block/
│       ├── GraveBlockProvider.java   # 무덤 블록 배치/제거 추상화
│       └── VanillaBlockProvider.java # 바닐라 Material 기반 구현체
└── util/
    └── MessageUtil.java           # 색상 변환, 시간 포맷팅, 타임존
```

### 데이터 흐름
1. **플러그인 로드**: `ConfigManager`가 `config.yml`, `items.yml`, `messages.yml`을 로드합니다.
2. **사망 발생**: `PlayerDeathListener`가 보호권을 확인하고, 없으면 드랍율에 따라 아이템과 경험치를 드랍합니다. 스택형 각인 아이템은 스택을 1 감소시킵니다.
3. **보호권 사용**: 소모형은 자동 소모, 시간형은 우클릭으로 `TimedProtectionStore`에 만료 시간을 저장합니다.
4. **각인 적용**: `SoulbindInventoryListener`가 각인 도구 사용 시 `SoulbindManager`를 통해 PDC에 각인 정보를 저장합니다. TIME/STACK 타입 충돌 시 거부합니다.
5. **각인 제한**: 픽업/이동 리스너가 각인 상태를 확인하여 소유자 외에는 차단합니다.
6. **만료 처리**: 주기적 스캔과 이벤트 기반 검사로 만료된 각인(TIME) 또는 스택이 소진된 각인(STACK)을 자동 제거합니다.
7. **무덤 생성** (`grave.enabled=true`): `PlayerDeathListener`가 드랍 대상으로 선택된 아이템/경험치를 바닥 대신 `GraveManager.createGrave`로 넘겨 사망 지점에 무덤을 생성합니다.
8. **무덤 열람/회수**: `GraveInteractListener`가 우클릭을 가로채 주인이면(도굴 중이었다면 취소 후) 즉시 가상 GUI를 열고, 타인이면 도굴 도구를 요구합니다. `GraveGuiListener`가 아이템 클릭/모두 회수를 처리하며, 저장은 비동기로 수행됩니다.
9. **도굴**: `LootSessionManager`가 시전 타이머를 관리하며, 완료 시 무덤 상태를 LOOTED로 전환합니다. 주인이 시전 중 무덤을 열면 세션이 즉시 취소됩니다.
10. **만료/히스토리**: `GraveTickManager`가 만료된 무덤을 정리하고, `GraveHistoryStorage`가 사망 시점 원본 스냅샷과 회수 상태를 기록합니다. 어드민은 `/invkeeper grave history`로 조회/텔레포트/원상태 GUI 열람 및 회수를 할 수 있습니다.

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

## 업데이트 내역 (요약)

무덤 시스템은 v1.4.0에서 추가된 이후 아래와 같이 다듬어졌습니다. 전체 변경 이력은 [`RELEASE_NOTES/`](RELEASE_NOTES) 폴더의 버전별 파일을 참고하세요.

- **v1.4.3**: 시간형 보호권 남은 시간 알림 시점을 `messages.yml`의 `timed-remaining-alerts`에서 초 단위로 자유롭게 설정(예: 5분/1분/10초/3초 전)할 수 있도록 변경. 보호 시간이 종료되는 순간 알림(`timed-expired`) 추가.
- **v1.4.2**: 사망/무덤 열람/히스토리 조회 시 TPS·MSPT가 흔들리던 원인(메인 스레드 동기 디스크 I/O)을 비동기 처리로 해결. 서버 재시작 후 무덤 홀로그램이 겹쳐 보이던 문제, 도굴 취소 시 무덤 주인에게 알림이 가지 않던 문제, 도굴 도구 로어의 `{duration}` 미표기 문제 수정.
- **v1.4.1**: "모두 회수" 클릭 시 GUI가 닫히지 않던 문제, 도굴 중 주인이 무덤을 열어도 도굴이 취소되지 않던 문제, 서버 재시작 시 무덤 데이터가 유실되던 문제, 어드민 히스토리 GUI의 좌/우클릭(텔레포트/원상태 가상 GUI 열람)이 동작하지 않던 문제 수정.
- **v1.4.0**: 무덤(Grave) 시스템 최초 추가.

---

## 라이선스

이 프로젝트는 MIT 라이선스를 따릅니다.
