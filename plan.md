# InvKeeper 리팩토링 계획서  
  
## 개요  
설정 파일 분리 및 Kind enum 확장으로 boolean 플래그 제거  
  
## 파일 구조  
- config.yml: rules 및 기본 설정  
- items.yml: 아이템 정의  
- messages.yml: 메시지 템플릿  
  
## Kind 종류  
- CONSUMABLE_PROTECTION: 소모형 보호권  
- TIMED_PROTECTION: 시간형 보호권  
- SOULBIND_TOOL: 각인 도구  
- SOULBIND_UNBIND_TOOL: 각인 해제 도구  
  
## 작업 내역  
- [x] 설정 파일 3종 분리  
- [x] ProtectionItemConfig Kind 확장  
- [x] ProtectionManager Kind 기반 로직  
- [x] SoulbindInventoryListener Kind 기반  
- [x] ConfigManager 3파일 로드  
- [x] 빌드 성공 
