# InvKeeper

> 인벤토리 보호 아이템과 영혼각인(소울바인드) 시스템을 제공하는 Paper 플러그인

플레이어가 사망했을 때 인벤토리와 경험치 손실을 막아주는 아이템을 추가하고, 아이템에 소유자를 각인하여 거래나 도난을 방지할 수 있습니다. MMOItems 플러그인과의 연동을 지원합니다.

## 주요 기능

- **인벤토리 보호권** — 사망 시 자동으로 소모되어 인벤토리와 경험치를 완전히 보호합니다.
- **시간제 보호권** — 우클릭으로 일정 시간 동안 보호 상태를 유지합니다.
- **영혼각인 (소울바인드)** — 아이템에 소유자를 각인하여 타인이 획득하거나 사용할 수 없도록 제한합니다.
- **각인 도구 / 해제 도구** — 아이템 위에 드래그하는 방식으로 각인을 적용하거나 해제합니다.
- **월드별 / 권한별 설정** — 세계마다 다른 드랍율을 설정하고, 권한에 따라 우선순위를 적용합니다.
- **MMOItems 연동** — MMOItems로 등록된 아이템도 보호권 및 각인 도구로 사용할 수 있습니다.

## 명령어

| 명령어 | 설명 | 권한 |
|--------|------|------|
| `/invkeeper status` | 현재 적용 중인 드랍율과 보호 상태를 확인합니다 | `invkeeper.status` |
| `/invkeeper reload` | 설정 파일을 다시 불러옵니다 | `invkeeper.admin` |
| `/invkeeper give <플레이어> <아이템> [개수]` | 보호 아이템을 지급합니다 | `invkeeper.admin` |
| `/invkeeper soulbind inspect <플레이어>` | 대상 플레이어의 각인된 아이템 목록을 확인합니다 | `invkeeper.admin` |
| `/invkeeper soulbind unbind <플레이어> [슬롯\|all]` | 특정 슬롯이나 전체 인벤토리의 각인을 강제 해제합니다 | `invkeeper.admin` |

## 권한

| 권한 | 설명 |
|------|------|
| `invkeeper.status` | `/invkeeper status` 명령어 사용 |
| `invkeeper.admin` | 모든 관리자 명령어 사용 및 각인 우회 |
| `invkeeper.soulbind.bypass` | 다른 플레이어의 각인된 아이템도 자유롭게 다룰 수 있음 |

## 설정 파일

### config.yml — 기본 설정

```yaml
# 서버 시작 시 모든 월드의 keepInventory를 false로 강제합니다
force-keep-inventory-false: true

rules:
  world:
    default:
      inventory-drop-percent: 50
      exp-drop-percent: 50
    world_nether:
      inventory-drop-percent: 100
      exp-drop-percent: 100
  permissions:
    - permission: "invkeeper.vip"
      priority: 100
      inventory-drop-percent: 25
      exp-drop-percent: 25
```

- **world**: 월드별 드랍율을 설정합니다. `default`는 설정되지 않은 모든 월드에 적용됩니다.
- **permissions**: 권한별로 다른 드랍율을 적용할 수 있습니다. `priority`가 높은 규칙이 우선 적용됩니다.

### items.yml — 아이템 설정

```yaml
items:
  consumable_vanilla:
    kind: CONSUMABLE_PROTECTION   # 소모형 보호권
    use-type: vanilla
    vanilla-material: PAPER
    vanilla-name: "&b인벤토리 보호권 &7(소모용)"
    soulbind:
      enabled: true
      duration-minutes: 0         # 0 = 영구 각인

  timed_vanilla:
    kind: TIMED_PROTECTION        # 시간형 보호권
    use-type: vanilla
    duration-minutes: 30
    vanilla-material: PAPER
    soulbind:
      enabled: true
      duration-minutes: 0

  soulbind_tool_vanilla:
    kind: SOULBIND_TOOL           # 각인 도구
    use-type: vanilla
    vanilla-material: amethyst_shard
    soulbind:
      enabled: true
      duration-minutes: 0         # 도구 자체의 각인
    soulbind-apply-duration: 60   # 적용할 각인의 지속 시간 (분)
```

지원하는 아이템 종류:
| kind | 설명 |
|------|------|
| `CONSUMABLE_PROTECTION` | 사망 시 자동 소모되는 보호권 |
| `TIMED_PROTECTION` | 우클릭으로 활성화하는 시간제 보호권 |
| `SOULBIND_TOOL` | 아이템에 영혼각인을 적용하는 도구 |
| `SOULBIND_UNBIND_TOOL` | 각인을 해제하는 도구 |

### messages.yml — 메시지 설정

사망 메시지, 보호 알림, 각인 관련 메시지를 서버에 맞게 수정할 수 있습니다. `time-format`과 `timezone`도 이 파일에서 설정합니다.

## 다운로드 및 적용

1. [릴리스 페이지](https://github.com/qlqo0001-beep/InvKeeper/releases)에서 최신 JAR 파일을 다운로드합니다.
2. 서버의 `plugins` 폴더에 넣습니다.
3. 서버를 재시작하면 `plugins/InvKeeper/` 폴더에 설정 파일이 생성됩니다.
4. `items.yml`과 `messages.yml`을 필요에 맞게 수정합니다.
5. `/invkeeper reload`로 설정을 적용합니다.

## 의존성

- **Paper** 1.21 이상 (또는 호환되는 서버 코어)
- **MMOItems** (선택 사항 — 설치되어 있으면 MMOItems 아이템을 보호권/각인 도구로 사용 가능)

## 라이선스

이 프로젝트는 MIT 라이선스를 따릅니다.