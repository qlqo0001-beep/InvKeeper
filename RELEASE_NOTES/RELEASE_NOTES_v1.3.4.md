# InvKeeper v1.3.4 릴리즈 노트

**릴리즈 날짜**: 2026-07-26  
**호환 버전**: Paper 1.21.x

---

## 주요 변경사항

### 개선 사항

#### bStats 메트릭스 통합
- **변경**: `org.bstats:bstats-bukkit:3.2.1` 의존성 추가
- **구현**: `InvKeeperPlugin.onEnable()`에서 Metrics 초기화 (플러그인 ID: 32889)
- **커스텀 차트**: MMOItems 설치 여부를 추적하는 SimplePie 차트 추가
- **Shade 플러그인**: `maven-shade-plugin` 3.5.1로 업그레이드 및 bStats 클래스 relocation (`com.invkeeper.libs.bstats`)
- **대시보드**: [https://bstats.org/plugin/bukkit/InvKeeper/32889](https://bstats.org/plugin/bukkit/InvKeeper/32889)

#### 리로드 시 요약 로그 추가
- **변경**: `/invkeeper reload` 명령어 실행 시 로드된 설정 요약을 플레이어에게 표시
- **출력 형식**: `월드 규칙 4개, 권한 규칙 11개, 보호 아이템 10개 로드 완료`
- **위치**: `InvKeeperCommand.handleReload()` 메서드

### 버그 수정

#### 각인 아이템 픽업 차단 메시지에 아이템 이름 표시
- **문제**: `{item_name}` 플레이스홀더가 코드에서 치환되지 않아 메시지에 아이템 이름이 표시되지 않음
- **원인**: `SoulbindPickupListener`, `SoulbindTransferListener`, `SoulbindInventoryListener`에서 `getSoulboundCantPickupMessage()` 호출 시 `.replace("{item_name}", ...)` 누락
- **수정**: 총 4개 호출부 모두 아이템 이름 추출 로직 추가
  ```java
  String itemName = item.getItemMeta() != null && item.getItemMeta().hasDisplayName()
      ? item.getItemMeta().getDisplayName()
      : item.getType().name();
  ```
- **결과**: 메시지에 아이템 이름이 정상 표시됨 (예: "다이아몬드 검은(는) 플레이어명의 각인 아이템입니다.")

#### 스택형 각인 사망 시 보호 및 로어 갱신
- **문제 1**: 스택 1회 각인 아이템이 사망 시 드랍됨
  - **원인**: `PlayerDeathListener.onPlayerDeath()`에서 스택 차감을 보호 확인 전에 수행
  - **수정**: 3단계 처리로 분리 (TIME 만료 정리 → 드랍 계산 → STACK 소모)
- **문제 2**: 스택 차감 후 로어 텍스트가 갱신되지 않음
  - **원인**: `SoulbindManager.decrementStacks()`에서 `updateLore()` 호출 누락
  - **수정**: `decrementStacks()` 호출 후 `updateLore()` 추가

#### 각인 아이템 픽업 메시지 쿨다운 추가
- **문제**: 다른 플레이어의 각인 아이템을 주우려고 할 때 메시지가 매 틱마다 표시되어 채팅 도배
- **수정**:
  - `config.yml`에 `soulbind-pickup-message-cooldown-seconds` 설정 추가 (기본값: 5초)
  - `SoulbindPickupListener`에 플레이어별 메시지 쿨다운 로직 추가
  - 0으로 설정 시 쿨다운 없음 (기존 동작)

### 기타

#### messages.yml YAML 파싱 오류 수정
- **문제**: `soulbound-cant-pickup` 줄 앞에 불필요한 들여쓰기(공백 2칸)가 있어 YAML 파싱 실패
- **수정**: 들여쓰기 제거하여 정상 파싱되도록 수정

---

## 빌드 정보

- **빌드 도구**: Maven 3.9.x
- **Java 버전**: 21
- **Paper API**: 1.21.x (26.2.build+)
- **컴파일 결과**: BUILD SUCCESS
- **산출물**: `target/InvKeeper.jar` (v1.3.4, shaded with bStats)

---

## 마이그레이션 가이드

### 기존 사용자
- 기존 `config.yml`, `items.yml`, `messages.yml`을 그대로 사용 가능
- 설정 파일 변경 없이 자동으로 버그 수정 및 개선 사항 적용

### 새로운 설정
```yaml
# config.yml에 추가된 설정
soulbind-pickup-message-cooldown-seconds: 5  # 기본값 5초, 0이면 쿨다운 없음
```

### 주의사항
- 스택형 각인 아이템을 소지한 상태로 사망 시, 각인 횟수가 1회 차감되고 로어가 갱신됩니다
- 스택이 0이 된 아이템은 각인이 완전히 제거됩니다
- 각인 아이템 픽업 차단 메시지가 5초에 한 번만 표시됩니다 (설정으로 조정 가능)
- bStats 메트릭스가 서버 시작 시 자동으로 초기화됩니다 (옵트아웃 없음)

---

## 알려진 이슈
- 없음

## 다음 버전 예정
- 순수 함수 단위 테스트 추가 (선택적)
- Paper API deprecated 경고 제거 (KEEP_INVENTORY GameRule)