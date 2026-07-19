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
- MMOItems 연동 지원 (6.10 이상)
- `/invkeeper status` 명령으로 현재 보호 상태, 적용 중인 드랍 비율 확인
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
- `protection-items`: 소모형/시간형 보호권 설정
- `messages`: 사망 및 보호 메시지 템플릿

## 명령어

- `/invkeeper reload`: 설정을 다시 불러옵니다. (`invkeeper.admin` 권한 필요)
- `/invkeeper give <player> <consumable|timed> [amount]`: 보호 아이템을 지급합니다. (`invkeeper.admin` 권한 필요)
- `/invkeeper status`: 자신에게 적용 중인 월드/권한 드랍 비율과 남은 시간 보호 상태를 확인합니다.

## 현재 변경 사항

- 사망 메시지에 실제로 잃은 아이템 수와 경험치 양을 표시
- 인벤토리 손실 퍼센트를 실제 드랍된 슬롯 기준으로 계산하여 보여줌
- MMOItems를 6.10 이상 버전만 인식하도록 개선
- `/invkeeper status` 명령어 추가
- 보호 상태 자동 알림 추가
- 탭 완성 기능 추가

## 릴리즈

- `v1.0.1`
  - 실제 잃은 아이템 퍼센트를 정확히 표시하도록 수정
  - `MMOItems` 버전 호환성 개선
  - GitHub 저장소와 README 추가
