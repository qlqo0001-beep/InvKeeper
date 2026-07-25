# InvKeeper v1.3.6 릴리즈 노트

**릴리즈 날짜**: 2026-07-26  
**호환 버전**: Paper 1.21.x

---

## 주요 변경사항

### 버그 수정 (중요)

#### 각인 아이템 아이템 복제 버그 수정
- **문제**: 남의 각인 아이템을 shift-click으로 상자에 넣으려 하면 원본은 그대로 남고 사본이 바닥에 떨어지는 아이템 복제 버그
- **원인**: `SoulbindTransferListener.onInventoryClick()`에서 `event.getInventory()`(항상 top 인벤토리)와 `event.getSlot()`(클릭된 인벤토리 기준)을 혼동하여 슬롯을 직접 조작
- **수정**: 슬롯 직접 조작 코드(`setItem(slot, null)`, `dropItemNaturally`)를 제거하고 `setCancelled(true)`만 사용하여 이동 자체를 차단
- **결과**: 아이템 복제 및 `ArrayIndexOutOfBoundsException` 완전 해결

#### 일반 클릭으로 각인 아이템 이동 가능 버그 수정
- **문제**: `if (!event.isShiftClick()) return;` 조건으로 인해 일반 클릭(커서로 집기, 더블클릭, 번호키 이동)은 검사 없이 통과
- **수정**: shift-click 조건 제거, 모든 클릭 타입에서 각인 아이템 이동 차단

#### 각인 아이템 사용 차단 안전망 추가
- **문제**: 비소유자가 남의 각인 아이템을 손에 들고 우클릭 사용/채굴/설치/공격/발사 가능
- **수정**: `SoulbindUseListener` 신규 생성으로 다음 이벤트 차단:
  - `PlayerInteractEvent` (우클릭 사용, 주손/보조손 모두)
  - `BlockBreakEvent` (채굴)
  - `BlockPlaceEvent` (설치)
  - `EntityDamageByEntityEvent` (근접 공격)
  - `EntityShootBowEvent` (활/석궁 발사)
  - `ProjectileLaunchEvent` (트라이던트/투척, 주손+보조손)
- **메시지 쿨다운**: 공격 등 연속 발생 시 메시지 스팸 방지 (기본 3초, `config.yml`에서 조정 가능)

#### ConfigManager 로딩 누락 수정
- **문제**: `soulbind-use-message-cooldown-seconds` 설정이 `config.yml`에 있어도 Java 코드에서 읽지 않아 항상 기본값 3초로 고정
- **수정**: `loadConfig()`에 `soulbindUseMessageCooldownSeconds` 로딩 코드 추가

### 개선 사항

#### ProjectileLaunchEvent 보조손 지원
- 트라이던트나 투척 아이템을 보조손에 들고 던지는 경우도 각인 사용 차단

---

## 빌드 정보

- **빌드 도구**: Maven 3.9.x
- **Java 버전**: 21
- **Paper API**: 1.21.x (26.2.build+)
- **컴파일 결과**: BUILD SUCCESS
- **산출물**: `target/InvKeeper-1.3.6-shaded.jar` (bStats 포함)

---

## 마이그레이션 가이드

### 기존 사용자
- 기존 설정 파일을 그대로 사용 가능
- 코드 변경 없이 자동으로 버그 수정 적용

### 새로운 설정
```yaml
# config.yml에 추가된 설정
soulbind-use-message-cooldown-seconds: 3  # 기본값 3초, 0이면 쿨다운 없음
```

### 주의사항
- 각인 아이템을 상자에 넣으려고 하면 이동이 차단되고 메시지가 표시됨
- 비소유자가 각인 아이템을 사용하려고 하면 모든 행위가 차단됨
- 근접 공격 등 연속 발생 시 메시지는 3초에 한 번만 표시됨 (설정으로 조정 가능)

---

## 알려진 이슈
- 없음

## 다음 버전 예정
- 순수 함수 단위 테스트 추가 (선택적)
- Paper API deprecated 경고 제거 (KEEP_INVENTORY GameRule)