# InvKeeper v1.2.2 Release Notes

## 변경 사항

### 버그 수정
- **`/invkeeper status` 명령어에 권한 체크 추가**: `plugin.yml`에 `invkeeper.status` 권한(기본값: true)을 추가하고, `handleStatus()`에서 권한을 검사하도록 수정했습니다.
- **각인 메시지 누락 분기 보완**: `SoulbindInventoryListener`의 TIME/STACK 분기에서 침묵 처리되던 경로들에 메시지를 출력하도록 수정했습니다.
  - 이미 무한 각인된 아이템에 각인 도구 사용 시 → `soulbound-already-infinite` 메시지 출력
  - 기존 각인 연장/업그레이드 시 → `soulbound-extended` 메시지 출력 (기존 `soulbound-applied` 대신)
- **UUID 원문 노출 제거**: `SoulbindTransferListener.onInventoryDrag()`에서 `owner.toString()` 대신 `getOwnerName()` 헬퍼를 사용하여 플레이어 이름을 표시하도록 수정했습니다.
- **사망 메시지 퍼센트 계산 정정**: `countOccupiedSlots()`가 각인 아이템을 제외하도록 수정하여, `{inv_percent}`가 실제 드랍된 비-각인 아이템 비율과 일치하도록 개선했습니다.

### 견고성 개선
- **각인 도구 리스너 인벤토리 타입 가드 추가**: `SoulbindInventoryListener.onInventoryClick()`에서 `event.getClickedInventory()`가 `PLAYER` 타입인 경우에만 로직을 수행하도록 가드를 추가했습니다. 다른 플러그인의 커스텀 GUI(상점 등)에서 각인 도구가 오작동하는 것을 방지합니다.

### 코드 정리
- **`invkeeper.soulbind.give` 권한 노드 제거**: 코드/README에서 참조되지 않는 죽은 권한을 `plugin.yml`에서 제거했습니다.

### 문서
- README의 권한 테이블에 `invkeeper.status` 권한을 추가했습니다.