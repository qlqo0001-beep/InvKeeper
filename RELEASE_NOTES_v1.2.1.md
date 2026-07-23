# InvKeeper v1.2.1

> Paper 1.21+ 기반 인벤토리 보호 및 영혼각인(소울바인드) 플러그인

## 변경 사항

### max-soulbind-stack 설정 추가
- `config.yml`에 `max-soulbind-stack` 설정 추가
- 아이템에 할당 가능한 최대 스택 수를 제한할 수 있음
- 기본값: `-1` (무제한)
- 양수로 설정 시 해당 값을 초과하는 스택 각인 시도는 거부됨

### 최대 스택 초과 시 거부 로직
- 스택형 각인 도구로 아이템에 각인을 적용할 때 `max-soulbind-stack`을 초과하면 **각인기가 소모되지 않고 거부**
- 새 아이템 각인, 기존 아이템 스택 추가, 무한 각인 전환 모두 체크
- 거부 시 메시지 출력: `"&c최대 각인 스택({max})을 초과하여 적용할 수 없습니다."`

### 설정 키 변경
- `default-soulbind-stacks` → `max-soulbind-stack` (의미 변경)
- `ConfigManager.parseSoulbindStacks()`의 cap 로직 제거 → 런타임 체크로 대체

### 메시지 추가
- `soulbound-max-stack`: 최대 스택 초과 시 거부 메시지

---

## 다운로드

[InvKeeper.jar](https://github.com/qlqo0001-beep/InvKeeper/releases/download/v1.2.1/InvKeeper.jar)

---

## 설치 방법

1. JAR 파일을 서버의 `plugins` 폴더에 넣습니다.
2. 서버를 재시작하면 `plugins/InvKeeper/` 폴더에 설정 파일이 생성됩니다.
3. `items.yml`과 `messages.yml`을 필요에 맞게 수정합니다.
4. `/invkeeper reload`로 설정을 적용합니다.

---

## 주의사항

- v1.2.0에서 업그레이드하는 경우 `config.yml`의 `default-soulbind-stacks`를 `max-soulbind-stack`으로 수동 변경해야 합니다.
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