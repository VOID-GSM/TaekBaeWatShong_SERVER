package com.example.taekbaewatshongserver.domain.auth.entity

enum class RefreshTokenRevokeReason {
    ROTATED, // 재발급으로 회전됨
    LOGOUT, // 로그아웃
    REUSE_DETECTED, // 재사용(탈취) 감지로 family 전체 폐기
}
