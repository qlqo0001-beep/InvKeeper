# InvKeeper v1.4.0 릴리즈 노트

> 출시일: 2026-08-10
> 주요 변경: **무덤(Grave) 시스템 추가**

---

## 🆕 신규 기능: 무덤(Grave) 시스템

플레이어 사망 시 사망 위치에 상자를 생성하고, 잃은 아이템과 경험치를 그 안에 보관하는 무덤 시스템이 추가되었습니다.

### 무덤 생성
- 사망 시 기존 % 드랍 로직과 동일하게, 드랍 대상으로 선택된 아이템이 무덤 상자에 보관됩니다. (나머지는 플레이어가 유지)
- 무덤 GUI는 플레이어 인벤토리와 동일한 레이아웃으로, 잃은 아이템이 원래 슬롯 위치에 표시됩니다.
- 경험치도 % 드랍율에 따라 무덤 GUI의 경험치병으로 회수할 수 있습니다.

### 무덤 GUI (플레이어 인벤토리 레이아웃)
-장비칸(4), 왼손(1), 인벤토리(36) + "모두 회수" 버튼
- 빈 슬롯은 경험치병 아이콘으로 표시, 클릭 시 경험치 회수
- 착용 가능한 장비는 자동 착용, 불가 시 인벤토리로 이동

### 도굴 시스템
- `grave_loot_vanilla` 아이템 사용 시 n초(기본 5분)의 시전 후 타인의 무덤 오픈 가능
- 시전 중 이동 가능, 사망/로그아웃/킥으로 취소되지 않음
- 서버 재시작 시 남은 시간 유지 (영속화)
- 무덤 주인이 시전 중 무덤을 열면 도굴 취소

### 홀로그램
- TextDisplay 엔티티 사용 (외부 플러그인 불필요)
- 무덤 상태별 실시간 업데이트 (ACTIVE / 도굴중 / 도굴완료)
- 서버 재시작/청크 로드 시 자동 복구, 중복 스폰 방지

### 관리자 명령어
- `/invkeeper grave list` — 서버 내 활성 무덤 목록 (페이지 지원)
- `/invkeeper grave history <player>` — 플레이어별 과거 무덤 이력 (회수 상태 표시, 페이지 지원)
- `/invkeeper grave reload` — 무덤 설정 리로드
- `/invkeeper give <player> grave_loot_vanilla` — 도굴 아이템 지급

### 회수 상태 추적
- 주인 전량 회수: "주인 회수 완료" + 무덤 즉시 소멸
- 도굴자 회수: "도굴꾼 회수" 표기
- 어드민 회수: 미표기
- 만료 시 자동 히스토리 기록

---

## 📝 설정 파일 변경사항

### `config.yml` — 신규 `grave:` 섹션
```yaml
grave:
  enabled: true
  container: { type: VANILLA, vanilla-material: BARREL }
  max-graves-per-player: 5
  expire:
    enabled: true
    default-seconds: 3600
    permission-overrides: [...]  # 권한별 만료 시간
  history: { retention-days: 30, max-entries-per-player: 50 }
  hologram: { enabled: true, offset-y: 1.0 }
  protection: { prevent-block-break: true, ... }
```

### `gui.yml` — 신규 파일
- 무덤 GUI의 타이틀, 경험치병 아이콘, "모두 회수" 버튼, 회수 상태 로어 설정 가능

### `items.yml` — 신규 아이템
- `grave_loot_vanilla`: 도굴 아이템 (바닐라, TRIPWIRE_HOOK)

### `messages.yml` — 신규 메시지 13종
- 무덤 생성, 오픈, 만료, 전량 회수, 도굴 관련 메시지 추가

---

## 🔧 기술 변경사항

### 신규 패키지: `com.invkeeper.grave`
- `Grave`, `GraveContents`, `GraveState`, `RecoveryType` — 데이터 모델
- `GraveManager` — 무덤 생성/조회/삭제 중앙 관리
- `GraveStorage`, `GraveHistoryStorage`, `LootSessionStorage` — YAML 영속화
- `GraveHologramManager` — TextDisplay 엔티티 관리
- `GraveTickManager` — 1초 주기 만료/홀로그램 스케줄러
- `LootSessionManager` — 도굴 시전 타이머 관리
- `GraveInventoryView` — 공유 인벤토리 GUI
- `GraveListGui`, `GraveHistoryGui` — 관리자 GUI
- `GraveBlockProvider`, `VanillaBlockProvider` — 블록 추상화

### 신규 리스너
- `GraveInteractListener` — 무덤 우클릭 이벤트
- `GraveProtectionListener` — 블록 파괴/폭발/피스톤 방지
- `GraveGuiListener` — GUI 클릭/드래그/닫기 이벤트

### 수정된 기존 파일
- `PlayerDeathListener` — `grave.enabled=true` 시 무덤 생성 경로로 분기
- `ProtectionItemConfig.Kind` — `GRAVE_LOOT_TOOL` 값 추가
- `ConfigManager` — grave 설정 및 메시지 로딩 추가
- `InvKeeperCommand` — `grave` 서브커맨드 추가
- `plugin.yml` — 버전 1.4.0, 신규 권한 4종 등록

---

## 🔒 신규 권한
- `invkeeper.grave.admin` — 무덤 관리 (list, history, reload)
- `invkeeper.grave.bypass` — 도굴 아이템 없이 무덤 강제 오픈
- `invkeeper.grave.time.vip` — VIP 무덤 만료 시간 등급
- `invkeeper.grave.time.vvip` — VVIP 무덤 만료 시간 등급 (무제한)
