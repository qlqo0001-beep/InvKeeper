# InvKeeper Release v1.1.0

## 변경 사항

### 영혼각인(Soulbind) 시스템 도입
- 아이템에 소유자 UUID와 만료 시간을 PDC(PersistentDataContainer)에 저장하는 각인 시스템 구현
- 소유자만 픽업 가능, 타인 픽업 시도 시 차단 및 메시지 출력
- Shift-click 비소유자 이동 시도 시 강제 드랍 처리
- 호퍼/드로퍼/광산수레 자동 아이템 이동 차단 (InventoryMoveItemEvent + InventoryPickupItemEvent 이중 방어)
- 드래그 이동 시 비소유자 각인 아이템 포함 시 이벤트 취소
- 사망 시 각인 아이템 드랍 제외

### 각인 도구 아이템
- 드래그-드랍 방식으로 각인/연장/해제 가능한 도구 아이템 지원
- 각인 해제 도구(unbind) 지원
- 무한 각인 및 시간 제한 각인 모두 지원
- 도구 자체의 각인 시간(`soulbind.duration-minutes`)과 적용할 각인의 시간(`soulbind-apply-duration`) 분리
- MMOItems 및 바닐라 아이템 모두 각인 도구로 사용 가능

### 관리자 명령어 추가
- `/invkeeper soulbind unbind <player> [slot|all]` - 특정 슬롯 또는 전체 각인 해제
- `/invkeeper soulbind inspect <player>` - 대상 플레이어의 각인 아이템 목록 조회 (슬롯, 아이템명, 소유자, 남은 시간 표시)

### 권한 추가
- `invkeeper.soulbind.bypass` - 다른 플레이어의 각인 아이템을 획득/이동할 수 있는 권한

### 버그 수정 및 안정성 개선
- 각인 만료 시 PDC 데이터와 로어를 함께 제거하도록 수정
- ProtectionAlertManager에 1초 주기 만료 검사 추가 (아이템을 건드리지 않아도 만료 처리)
- PDC 기반 로어 텍스트 저장 방식으로 전환 (서버 재시작 후에도 로어 중복 방지)
- 각인 시간 연장 시 로어 중복 생성 버그 수정
- 관리자 강제 해제 명령어에서 로어 미제거 버그 수정
- 모든 이벤트 핸들러에 예외 처리 추가 (단일 예외로 플러그인 크래시 방지)
- ProtectionAlertManager static singleton 제거 (Plugin이 인스턴스 직접 관리)
- Reload 시 Scheduler와 AlertManager 완전 종료 후 재생성

### 문서화
- README에 영혼각인 시스템 상세 설명 추가
- 명령어/권한 테이블 형식으로 정리
- items.yml 설정 가이드 및 주석 보강

## 주의사항

- v1.0.0에서 업그레이드하는 경우 기존 설정 파일(config.yml, items.yml, messages.yml)을 새로 생성하거나 수동으로 업데이트해야 할 수 있습니다.
- `items.yml`에 `soulbind-apply-duration` 필드가 추가되었습니다. 기존 설정을 사용하려면 각인 도구 항목에 이 필드를 추가하세요.

## 배포 정보

- GitHub 저장소: https://github.com/qlqo0001-beep/InvKeeper
- 빌드 명령어: `mvn clean package`
- 플러그인 JAR: `target/InvKeeper.jar`