// 인수인계 체크리스트(서버 영속) 도메인 모델 — synthesis 슬롯 한정
package com.happynurse.domain.model

data class CheckMeta(
    val by: String,
    val at: String,
)

data class HandoverChecks(
    val handoverId: String,
    val checkedSynthesisIndex: Map<Int, CheckMeta>,
)
