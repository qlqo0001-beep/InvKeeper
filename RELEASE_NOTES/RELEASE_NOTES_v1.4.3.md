# InvKeeper v1.4.3 릴리즈 노트

> 출시일: 2026-09-27
> 주요 변경: **시간형 보호권 남은 시간 알림 시점 설정 및 종료 알림 추가**

---

## ✨ 기능 개선

### 남은 시간 알림 시점을 어드민이 직접 설정
- 기존에는 5분 전 / 1분 전 알림만 고정으로 전송되었음.
- 이제 `messages.yml`의 `timed-remaining-alerts` 섹션에서 **키: 남은 시간(초), 값: 메시지**로 원하는 만큼 추가/삭제 가능 (예: 300, 60, 10, 3초 전).
- 순서는 상관없으며 남은 시간이 긴 알림부터 전송. 각 알림은 보호권 1회 사용당 한 번씩만 전송.
- 1초마다 확인하므로 최대 1초 정도 늦게 뜰 수 있음 (예: 3초 알림은 2~3초 남았을 때 전송). 정확한 남은 시간은 `{remaining}` 사용.
- 숫자가 아니거나 0 이하인 키는 콘솔 경고 후 무시.

```yaml
timed-remaining-alerts:
  300: "&e보호 상태가 5분 남았습니다. 남은 시간: {remaining}"
  60: "&e보호 상태가 1분 남았습니다. 남은 시간: {remaining}"
```

### 사용 시간 종료 알림 추가
- 보호 시간이 종료되는 순간 `timed-expired` 메시지 전송. `""`로 비워두면 전송하지 않음.
- 접속 중에 종료된 경우에만 전송 (오프라인 중 종료된 경우 재접속 시 전송하지 않음).

```yaml
timed-expired: "&c인벤토리 보호 시간이 종료되었습니다."
```

---

## 🔁 업데이트 시 참고
- 기존 서버의 `messages.yml`은 자동으로 갱신되지 않음. `timed-remaining-alerts` 섹션이 없으면 기존 `timed-remaining-five-minutes` / `timed-remaining-one-minute` 키로 이전과 동일하게 동작.
- 알림 시점을 변경하려면 위 섹션을 `messages.yml`에 직접 추가 후 `/invkeeper reload`.
- `timed-expired` 키가 없어도 기본 문구로 종료 알림이 전송됨.

---

## 🧱 영향을 받는 파일
- `ProtectionAlertManager`, `ConfigManager`
- `messages.yml`, `pom.xml`, `plugin.yml`
