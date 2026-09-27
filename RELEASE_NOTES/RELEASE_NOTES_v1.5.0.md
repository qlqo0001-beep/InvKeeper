# InvKeeper v1.5.0 릴리즈 노트

> 출시일: 2026-09-27
> 주요 변경: **위험 지역 안내, 무덤 자물쇠, PvP/PvE 드랍률 분리 추가 및 사망 처리 버그 수정**

---

## ✨ 신규 기능

### 위험 지역 안내
- 접속 시 1회, 그리고 월드 이동 시 **적용 드랍률이 바뀐 경우에만** 채팅으로 안내.
  - 드랍률이 있는 지역: `⚠ 위험 지역 (world) 사망 시 인벤토리 50%, 경험치 50%를 잃습니다.`
  - PvP 드랍률이 다르면 한 줄 추가, 시간형 보호 중이면 남은 시간 표시
  - 일반/PvP 드랍률이 모두 0%인 지역은 "안전 지역"으로 안내 (`danger-notice-safe`를 `""`로 두면 미표시)
- 권한 규칙 때문에 드랍률이 그대로라면 월드를 옮겨도 다시 안내하지 않음.
- 서버 전체 on/off: `config.yml`의 `danger-notice.enabled`
- 개인 on/off: `/invkeeper notice [on|off]` (인자 없으면 전환, 재접속 후에도 유지, 권한 `invkeeper.notice` 기본 허용)

### 무덤 자물쇠 (`kind: GRAVE_LOCK`)
- 사망 시 인벤토리에 있으면 1개가 자동 소모되어 생성되는 무덤에 자물쇠가 걸림.
- 아이템별 설정 (둘 다 동시에 사용 가능, 0이면 미적용)
  - `lock-seconds`: 사망 후 다른 플레이어가 도굴을 시작할 수 없는 시간(초)
  - `extra-cast-seconds`: 도굴 시전 시간 증가량(초)
- 보호권이 발동했거나 잃은 것이 없어 무덤이 생성되지 않으면 소모되지 않음.
- 잠긴 무덤에 도굴을 시도하면 남은 시간을 안내하며 도굴 도구는 소모되지 않음.
- 잠금 중 홀로그램에 남은 시간 표시 (`grave.hologram.locked-line-format`), 서버 재시작 후에도 유지.
- `items.yml`에 예시 아이템 `grave_lock_vanilla` 추가 (로어 placeholder: `{lock_seconds}`, `{extra_cast_seconds}`).

### PvP/PvE 드랍률 분리
- 월드/권한 규칙에 `pvp-inventory-drop-percent`, `pvp-exp-drop-percent` 추가 가능.
- PvP 사망 시 인벤토리/경험치 각각 다음 순서 중 먼저 설정된 값을 적용:
  1. 적용된 권한 규칙의 pvp 값
  2. 월드 규칙의 pvp 값
  3. 일반(PvE) 드랍률
- 다른 플레이어에게 죽은 경우만 PvP로 판정 (자기 화살/TNT 등은 PvE).
- `pvp.protection-items-work` (기본 `true`): `false`면 PvP 사망 시 보호권(소모형/시간형)이 발동하지 않고 소모되지도 않음. 이때 실제로 잃는 것이 있으면 `pvp-protection-ignored` 안내.
- PvP 사망 전용 메시지 `death-pvp` (`{killer}`), `/invkeeper status`에 PvP 드랍률 표시.

---

## 🐛 버그 수정
- **도굴 도구 오소모**: 도굴 도구를 인벤토리 어딘가에 갖고 있으면 손에 든 다른 아이템(예: 검)이 대신 소모되던 문제. 이제 **손에 든 아이템**이 `/invkeeper give`로 지급한 도굴 도구(또는 MMOItems)일 때만 도굴이 시작됨.
- **스택형 각인 미감소**: 무덤 시스템 사용 시 스택형 각인의 스택이 사망해도 줄지 않던 문제.
- **disabled-worlds 아이템 소실**: `grave.disabled-worlds` 월드에서 사망 시 잃는 아이템이 바닥에 떨어지지 않고 사라지던 문제. 이제 기존 방식대로 바닥에 드랍됨.
- **보호 종료 알림 오판정**: 리로드 직후나 접속 직후 보호가 끝나면 "자리를 비운 사이" 메시지가 잘못 나가던 문제.

---

## 🔁 업데이트 시 참고 (기존 서버 동작 변화)
- **도굴 도구**: 이름 없는 일반 철사 덫 갈고리 등 재료만 같은 아이템은 더 이상 도굴 도구로 인정되지 않음. 도구를 손에 들고 우클릭해야 함 (`grave-loot-item-required` 문구 변경).
- **스택형 각인**: 무덤이 켜진 서버에서 사망 시 스택형 각인이 설명대로 1씩 소모됨.
- **disabled-worlds**: 해당 월드 사망 시 잃는 아이템이 바닥에 드랍됨.
- 기존 `config.yml` / `items.yml` / `messages.yml`은 자동으로 갱신되지 않음. 새 키가 없어도 기본값으로 동작하며(PvP 값 없음 = 일반 드랍률, 보호권 PvP 작동, 위험 지역 안내 켜짐), 무덤 자물쇠를 쓰려면 `items.yml`에 아이템을 직접 추가.

---

## 🧱 영향을 받는 파일
- `PlayerDeathListener`, `DangerNoticeListener`(신규), `GraveInteractListener`, `GraveLootItemUseListener`
- `ConfigManager`, `WorldRule`, `PermissionRule`, `ProtectionItemConfig`, `GraveHologramConfig`
- `ProtectionManager`, `ProtectionAlertManager`, `VanillaProtectionItems`
- `Grave`, `GraveManager`, `GraveStorage`, `GraveHologramManager`
- `InvKeeperCommand`, `InvKeeperPlugin`
- `config.yml`, `items.yml`, `messages.yml`, `plugin.yml`, `pom.xml`
