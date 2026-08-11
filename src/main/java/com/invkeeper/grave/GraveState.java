package com.invkeeper.grave;

/**
 * 무덤의 생애주기 상태.
 */
public enum GraveState {
    ACTIVE,         // 정상 상태 (소유자만 오픈 가능)
    BEING_LOOTED,   // 도굴 시전 진행 중
    LOOTED           // 도굴 완료 (누구나 오픈 가능)
}
