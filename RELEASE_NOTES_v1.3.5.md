# InvKeeper v1.3.5 릴리즈 노트

**릴리즈 날짜**: 2026-07-26  
**호환 버전**: Paper 1.21.x

---

## 주요 변경사항

### 개선 사항

#### 탭 자동완성 권한 필터링
- **변경**: `/invkeeper` 명령어 탭 자동완성에서 권한 없는 서브커맨드를 숨김
- **일반 플레이어**: `status`만 표시
- **관리자 (`invkeeper.admin`)**: `status`, `reload`, `give`, `soulbind` 모두 표시
- **위치**: `InvKeeperCommand.onTabComplete()` 메서드

---

## 빌드 정보

- **빌드 도구**: Maven 3.9.x
- **Java 버전**: 21
- **Paper API**: 1.21.x (26.2.build+)
- **컴파일 결과**: BUILD SUCCESS
- **산출물**: `target/InvKeeper-1.3.5-shaded.jar` (bStats 포함)

---

## 마이그레이션 가이드

### 기존 사용자
- 기존 설정 파일을 그대로 사용 가능
- 코드 변경 없이 자동으로 탭 자동완성 필터링 적용

### 주의사항
- 일반 플레이어가 `/invkeeper <Tab>`을 입력해도 관리자 명령어(`reload`, `give`, `soulbind`)는 표시되지 않음
- 권한이 없는 명령어를 직접 입력하면 기존대로 "권한이 없습니다" 메시지가 출력됨

---

## 알려진 이슈
- 없음

## 다음 버전 예정
- 순수 함수 단위 테스트 추가 (선택적)
- Paper API deprecated 경고 제거 (KEEP_INVENTORY GameRule)