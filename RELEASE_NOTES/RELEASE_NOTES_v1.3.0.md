# InvKeeper v1.3.0 릴리즈 노트

**릴리즈 날짜**: 2026-07-24  
**호환 버전**: Paper 1.21.x

---

## 주요 변경사항

### Phase 1 — 안정성 및 운영 편의성 개선

#### 1-1. 설정 오타 경고 로그 추가
- **ConfigManager.parseMaterial()**: `items.yml`의 `vanilla-material` 값이 잘못된 Material 이름일 경우 콘솔에 명확한 경고 메시지 출력
- **MessageUtil.setTimezone()**: `config.yml`의 `timezone` 값이 잘못된 타임존 문자열일 경우 콘솔에 경고 메시지 출력

#### 1-2. 예외 로깅 개선
- **SoulbindInventoryListener**, **SoulbindTransferListener**: catch 블록에서 `e.getClass().getSimpleName()`을 포함하도록 개선
- 이제 `NullPointerException` 등 메시지가 `null`인 예외도 로그에서 예외 타입을 명확하게 확인 가능

#### 1-3. getOwnerName() 공용 메서드 통합
- **SoulbindManager.resolveOwnerName()** 신규 추가
- 온라인 플레이어 → 오프라인 플레이어 → UUID 순으로 이름을 조회하는 통합 메서드
- **SoulbindInventoryListener**, **SoulbindTransferListener**의 중복 로직을 공용 메서드로 교체
- 향후 로직 변경 시 한 곳만 수정해도 일관성 유지

#### 1-4. MMOItems 훅 reload 지원
- **MMOItemsHook.refresh()** 신규 추가
- **ProtectionManager.refreshMmoHook()** 신규 추가
- `/invkeeper reload` 실행 시 MMOItems 플러그인을 재감지하도록 개선
- 서버 시작 시 MMOItems가 늦게 로드된 경우에도 reload로 연동 활성화 가능

#### 1-5. MMOItems give 명령 공백 방어
- **MMOItemsHook.giveItemByCommand()**: `type` 또는 `id`에 공백이 포함된 경우 경고 로그 출력
- 관리자가 `items.yml` 설정 시 공백 오타를 낸 경우 콘솔에서 사전에 감지 가능

### Phase 2 — 운영 편의성 향상

#### 2-1. Enable/Reload 요약 로그
- **InvKeeperPlugin.onEnable()**: 서버 시작 시 로드된 월드 규칙 수, 권한 규칙 수, 보호 아이템 수를 콘솔에 요약 출력
- 운영자가 설정 파일을 얼마나 잘 읽었는지 콘솔만으로 빠르게 확인 가능

#### 2-2. Command 서브커맨드 분리
- **GiveHandler** 신규 생성: `/invkeeper give` 명령 처리 로직 분리
- **SoulbindAdminHandler** 신규 생성: `/invkeeper soulbind unbind/inspect` 명령 처리 로직 분리
- **InvKeeperCommand** 단일 파일의 책임 reduced, 유지보수성 향상

#### 2-3. Config 유효성 검사 확장
- **ConfigManager.loadConfig()**: `rules.world`에 등록된 월드 이름이 실제로 로드되어 있는지 확인
- 존재하지 않는 월드명인 경우 경고 로그 출력 (플러그인 비활성화하지 않음)

---

## 빌드 정보

- **빌드 도구**: Maven 3.9.x
- **Java 버전**: 21
- **Paper API**: 1.21.x
- **컴파일 결과**: BUILD SUCCESS
- **산출물**: `target/InvKeeper.jar` (v1.3.0)

---

## 마이그레이션 가이드

### 기존 사용자
- 기존 `config.yml`, `items.yml`, `messages.yml`을 그대로 사용 가능
- 설정 파일 변경 없이 자동으로 버전 1.3.0 기능 적용

### 새로운 기능 활용
1. **MMOItems 늦은 로드 대응**: MMOItems를 InvKeeper보다 나중에 시작한 경우 `/invkeeper reload` 실행
2. **설정 오타 감지**: `/invkeeper reload` 후 콘솔 로그에서 Material/Timezone 경고 확인
3. **각인 아이템 관리**: `/invkeeper soulbind inspect <player>`로 플레이어의 각인 아이템 목록 확인

---

## 알려진 이슈
- 없음

## 다음 버전 예정
- 순수 함수 단위 테스트 추가 (선택적)
- Paper API deprecated 경고 제거 (KEEP_INVENTORY GameRule)