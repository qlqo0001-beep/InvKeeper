# InvKeeper Release v1.0.1

## 변경 사항

- 사망 메시지에서 실제로 잃은 아이템 개수와 경험치 양을 출력하도록 개선
- 인벤토리 손실 비율을 실제 드랍된 슬롯 수 기준으로 계산하여 표시
- MMOItems 버전 제한을 제거하여 설치된 MMOItems를 가능한 한 넓게 인식하도록 호환성 체크 개선
- `/invkeeper status` 명령어 추가
- 보호 상태 자동 알림 추가 (5분, 1분 남음)
- 명령어 탭 완성 기능 추가
- `/invkeeper give`에서 사용할 보호권 종류 단어를 설정 파일로 변경 가능하게 개선
- 시간형 보호권의 지속 시간을 `duration-minutes`로 설정할 수 있도록 개선

## 배포 정보

- GitHub 저장소: https://github.com/qlqo0001-beep/InvKeeper
- 빌드 명령어: `mvn clean package`
- 플러그인 JAR: `target/InvKeeper.jar`
