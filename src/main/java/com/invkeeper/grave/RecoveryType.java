package com.invkeeper.grave;

/**
 * 무덤 회수 주체 유형.
 * 히스토리 GUI 표시에 사용됩니다.
 */
public enum RecoveryType {
    NONE,    // 미회수
    OWNER,   // 주인이 전량 회수
    LOOTER   // 도굴자가 일부라도 회수
}
