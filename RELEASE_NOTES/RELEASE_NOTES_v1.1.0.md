# InvKeeper v1.1.0

> Paper 1.21+ 기반 인벤토리 보호 및 영혼각인(소울바인드) 플러그인

## 주요 변경 사항

### 영혼각인(Soulbind) 시스템 도입
- 아이템에 소유자 UUID와 만료 시간을 PDC(PersistentDataContainer)에 저장하는 각인 시스템 구현
- 소유자만 픽업 가능, 타인 픽업 시도 시 차단 및 메시지 출력
- Shift-click 비소유자 이동 시도 시 강제 드랍 처리
- 호퍼/드로퍼/광산수레 자동 아이템 이동 차단 (InventoryMoveItemEvent + InventoryPickupItemEvent 이중 방어)
- 드래그 이동 시 비소유자 각인 아이템 포함 시 이벤트 취소
- 사망 시 각인 아이템 드랍 제외 (소유자 보호)

### 각인 도구 아이템 (신규)
- **SOULBIND_TOOL**: 아이템 위에 드래그-드랍하여 각인 적용
- **SOULBIND_UNBIND_TOOL**: 각인 해제
- 무한 각인 및 시간 제한 각인 모두 지원
- 도구 자체의 각인 시간(`soulbind.duration-minutes`)과 적용할 각인의 시간(`soulbind-apply-duration`) 분리
- MMOItems 및 바닐라 아이템 모두 각인 도구로 사용 가능

### 설정 파일 구조 개선
- 설정 파일 3종 분리: `config.yml`(기본 설정), `items.yml`(아이템 정의), `messages.yml`(메시지)
- 4가지 아이템 Kind 도입: CONSUMABLE_PROTECTION, TIMED_PROTECTION, SOULBIND_TOOL, SOULBIND_UNBIND_TOOL
- `timezone` 설정을 `config.yml`로 이동 (Wikipedia 타임존 목록 링크 포함)
- 각인 만료 시간 표시 타임존 지원

### 관리자 명령어 추가
- `/invkeeper soulbind unbind <player> [slot|all]` — 특정 슬롯 또는 전체 각인 해제
- `/invkeeper soulbind inspect <player>` — 대상 플레이어의 각인 아이템 목록 조회 (슬롯, 아이템명, 소유자, 남은 시간 표시)

### 권한 추가
- `invkeeper.soulbind.bypass` — 다른 플레이어의 각인 아이템을 획득/이동할 수 있는 권한

### 성능 최적화
- `soulbind-scan-batches` 설정 도입 (접속자를 배치로 나눠 각인 만료 검사, 부하 분산)
- 권장값: ~20명=1, ~50명=3, ~100명=5(기본값), ~200명=8~10, ~500명=15~20
- Paper 전용 `isSoulboundFast()` 경로 추가 (ItemMeta clone 없이 PDC 직접 조회, 핫루프 성능 향상)

### 버그 수정 및 안정성
- 각인 만료 시 PDC 데이터와 로어를 함께 제거
- ProtectionAlertManager에 1초 주기 만료 검사 추가 (아이템을 건드리지 않아도 만료 처리)
- PDC 기반 로어 텍스트 저장 방식으로 전환 (서버 재시작 후에도 로어 중복 방지)
- 각인 시간 연장 시 로어 중복 생성 버그 수정
- 관리자 강제 해제 명령어에서 로어 미제거 버그 수정
- 모든 이벤트 핸들러에 예외 처리 추가 (단일 예외로 플러그인 크래시 방지)
- ProtectionAlertManager static singleton 제거 (Plugin이 인스턴스 직접 관리)
- Reload 시 Scheduler와 AlertManager 완전 종료 후 재생성
- CleanupListener로 PlayerQuit 시 리마인더 상태 정리 (메모리 누수 방지)

### 문서화
- README에 영혼각인 시스템 상세 설명 추가
- 명령어/권한 테이블 형식으로 정리
- items.yml 설정 가이드 및 주석 보강

---

## 다운로드

[InvKeeper.jar](https://github.com/qlqo0001-beep/InvKeeper/releases/download/v1.1.0/InvKeeper.jar)

---

## 설치 방법

1. JAR 파일을 서버의 `plugins` 폴더에 넣습니다.
2. 서버를 재시작하면 `plugins/InvKeeper/` 폴더에 설정 파일이 생성됩니다.
3. `items.yml`과 `messages.yml`을 필요에 맞게 수정합니다.
4. `/invkeeper reload`로 설정을 적용합니다.

---

## 주의사항

- v1.0.0에서 업그레이드하는 경우 기존 설정 파일을 삭제하고 새로 생성하거나 수동으로 업데이트해야 합니다.
- `items.yml`에 `soulbind-apply-duration` 필드가 추가되었습니다. 각인 도구 항목에 이 필드를 추가하세요.
- Java 21 이상, Paper 1.21 이상 필요

---

## 파일 구조 (빌드 필수)

```
.gitignore
pom.xml
README.md
src/
└── main/
    ├── java/com/invkeeper/  (20개 Java 클래스)
    └── resources/           (4개 설정 파일)
```

---

## 빌드 방법

```bash
mvn clean package
# 출력: target/InvKeeper.jar