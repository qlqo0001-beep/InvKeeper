# InvKeeper

InvKeeper는 Minecraft Paper 26.2 서버를 위한 플러그인으로, 플레이어 사망 시 인벤토리와 경험치 드랍을 월드, 권한, 보호 아이템 기준으로 정밀하게 제어합니다.

## 주요 기능

- `keepInventory` 게임룰을 항상 `false`로 강제하여 인벤토리 복제 버그를 방지
- 월드별 기본 드랍 비율 설정
- 권한별 우선순위 기반 드랍 비율 적용
- 사망 시 41칸 전체 인벤토리(메인, 핫바, 방어구, 오프핸드)에서 랜덤 슬롯 드랍
- 경험치를 총량으로 계산하여 정확한 비율만큼 스폰
- 보호 아이템 2종 지원
  - 소모형 보호권: 소지만 해도 사망 시 자동 사용 후 1개 차감
  - 시간형 보호권: 우클릭 사용 시 지정 시간 동안 사망 보호
- **영혼각인(Soulbind) 시스템**
  - 아이템에 소유자 등록 및 만료 시간 설정 (PDC 기반)
  - 소유자만 픽업/이동 가능, 타인 픽업 시도 차단
  - Shift-click 비소유자 이동 시도 시 강제 드랍
  - 호퍼/드로퍼 등 자동 아이템 이동 차단
  - 드래그-드랍 방식으로 각인/해제 가능한 도구 아이템 지원
  - `/invkeeper soulbind unbind/inspect` 관리자 명령어
  - `invkeeper.soulbind.bypass` 권한으로 각인 우회 가능
- MMOItems 연동 지원 (LiveMMOItem 또는 NBTItem API를 사용하여 가능한 한 넓은 버전 호환성 제공)
- `/invkeeper status` 명령으로 현재 보호 상태, 적용 중인 드랍 비율 확인
- `/invkeeper soulbind inspect <player>` 명령으로 각인 아이템 목록 확인
- 보호권 활성 상태 자동 알림 (5분, 1분 남음)
- 탭 완성 지원

## 설치

1. `mvn clean package`로 빌드합니다.
2. 생성된 `InvKeeper.jar`를 서버 `plugins` 폴더에 복사합니다.
3. 서버를 시작하거나 재시작하면 `config.yml`이 생성됩니다.

## 구성

`config.yml`에서 다음을 설정합니다.

- `force-keep-inventory-false`: `true`일 경우 모든 월드의 `keepInventory` 게임룰을 강제 `false`로 설정
- `rules.world`: 월드별 인벤토리/경험치 드랍 비율
- `rules.permissions`: 우선순위 기반 권한 드랍 규칙
- `protection-items`: 각 보호 아이템을 고유 키로 정의하고, MMOItems 전용 또는 바닐라 전용 항목을 개별적으로 설정합니다.
  - `kind`: `consumable` 또는 `timed`
  - `use-type`: `mmoitems` 또는 `vanilla`
  - `duration-minutes`: `timed` 아이템의 지속 시간 (분)
    - `soulbind`: 영혼각인 설정 (`true` / 상세 설정 가능)
      - `enabled`: 각인 활성화 (아이템 획득 시 자동 각인 여부)
      - `tool`: 드래그-드랍 각인 도구 여부 (true = 각인/해제 도구)
      - `duration-minutes`: 각인 지속 시간 (0 = 무한)
      - `infinite`: 무한 각인 여부
      - `unbind`: 각인 해제 도구 여부
  - MMOItems와 바닐라 항목 모두를 지원하려면 각각 별도의 항목을 추가하세요.
- `messages`: 사망 및 보호 메시지 템플릿

## 명령어

| 명령어 | 설명 | 권한 |
|--------|------|------|
| `/invkeeper reload` | 설정을 다시 불러옵니다. | `invkeeper.admin` |
| `/invkeeper give <player> <item-key> [amount]` | 보호 아이템을 지급합니다. | `invkeeper.admin` |
| `/invkeeper status` | 자신에게 적용 중인 월드/권한 드랍 비율과 남은 시간 보호 상태를 확인합니다. | - |
| `/invkeeper soulbind unbind <player> [slot\|all]` | 대상 플레이어의 특정 슬롯 또는 전체 각인을 해제합니다. | `invkeeper.admin` |
| `/invkeeper soulbind inspect <player>` | 대상 플레이어의 각인 아이템 목록을 조회합니다. | `invkeeper.admin` |

### 별칭
- `/ik` - `/invkeeper`의 단축 명령어

## 권한

| 권한 | 설명 | 기본값 |
|------|------|--------|
| `invkeeper.admin` | InvKeeper 관리 권한 (reload, give, soulbind unbind/inspect) | op |
| `invkeeper.drop.donor` | 후원자 등급 사망 드랍 규칙 적용 권한 | false |
| `invkeeper.drop.vip` | VIP 등급 사망 드랍 규칙 적용 권한 | false |
| `invkeeper.drop.admin` | 관리자 등급 사망 드랍 규칙 적용 권한 | false |
| `invkeeper.soulbind.bypass` | 다른 플레이어의 각인 아이템을 획득/이동할 수 있는 권한 | false |
| `invkeeper.soulbind.give` | 각인 아이템을 지급할 수 있는 권한 | false |

## 영혼각인(Soulbind) 시스템 상세

### 동작 방식
- 각인된 아이템은 `PersistentDataContainer`에 소유자 UUID와 만료 시간을 저장
- 만료 시간이 `-1`이면 무한 각인
- 아이템 자체에 데이터가 저장되므로 서버 재시작 후에도 유지됨

### 각인 적용 방법
1. **획득 시 자동 각인**: 설정에서 `soulbind: true`로 설정된 보호 아이템은 지급/획득 시 자동 각인
2. **드래그-드랍 각인 도구**: 각인 도구 아이템을 대상 아이템 위에 드래그하여 각인 적용
3. **관리자 명령어**: `/invkeeper soulbind unbind`로 각인 해제

### 각인 해제
1. **각인 해제 도구(unbind)**: `soulbind.unbind: true`로 설정된 아이템으로 드래그-드랍
2. **관리자 명령어**: `/invkeeper soulbind unbind <player> [slot|all]`

### 제한 정책
- 소유자가 아닌 플레이어가 각인 아이템 픽업 시도 → 차단 및 메시지 출력
- Shift-click으로 타인 소유 각인 아이템 이동 시도 → 강제 드랍
- 호퍼/드로퍼/광산수레 자동 이송 → 차단
- 사망 시 각인 아이템은 드랍되지 않음

## 현재 변경 사항

- 사망 메시지에 실제로 잃은 아이템 수와 경험치 양을 표시
- 인벤토리 손실 퍼센트를 실제 드랍된 슬롯 기준으로 계산하여 보여줌
- MMOItems 버전 제한을 제거하여 설치된 MMOItems를 가능한 한 넓게 인식하도록 개선
- 보호 아이템을 `mmoitems` 전용 또는 `vanilla` 전용으로 개별 설정할 수 있도록 개선
- `/invkeeper status` 명령어 추가
- 보호 상태 자동 알림 추가
- 탭 완성 기능 추가
- 영혼각인(Soulbind) 시스템 도입
- 관리자 soulbind 명령어 추가 (unbind/inspect)
- 인벤토리 이동/전송 예외 처리 보완 (호퍼, 드래그, Shift-click)

## 릴리즈

- `v1.1.0`
  - 영혼각인(Soulbind) 시스템 도입
  - `/invkeeper soulbind unbind/inspect` 관리자 명령어 추가
  - 인벤토리 이동 보안 강화 (호퍼/드래그/Shift-click 차단)
  - `invkeeper.soulbind.bypass` 권한 추가
  - 각인 도구 아이템 드래그-드랍 방식 지원
  - 설정 파일에 soulbind 관련 예시 및 주석 추가

- `v1.0.1`
  - 실제 잃은 아이템 퍼센트를 정확히 표시하도록 수정
  - `MMOItems` 버전 호환성 개선
  - GitHub 저장소와 README 추가
