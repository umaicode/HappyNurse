// HandoverChecks DTO → Domain 매퍼
// synthesis.{idx} 키만 통과 — 그 외 슬롯 키는 방어적으로 drop.
package com.happynurse.data.remote.mapper

import com.happynurse.data.remote.model.HandoverChecksDataDto
import com.happynurse.domain.model.CheckMeta
import com.happynurse.domain.model.HandoverChecks

private val SYNTHESIS_KEY_REGEX = Regex("^synthesis\\.(\\d+)$")

fun HandoverChecksDataDto.toDomain(fallbackHandoverId: String): HandoverChecks {
    val raw = checkedItemsJson.orEmpty()
    val parsed = raw.mapNotNull { (k, v) ->
        val m = SYNTHESIS_KEY_REGEX.matchEntire(k) ?: return@mapNotNull null
        val idx = m.groupValues[1].toIntOrNull() ?: return@mapNotNull null
        idx to CheckMeta(by = v.by.orEmpty(), at = v.at.orEmpty())
    }.toMap()
    return HandoverChecks(
        handoverId = handoverId?.toString()?.takeIf { it.isNotBlank() } ?: fallbackHandoverId,
        checkedSynthesisIndex = parsed,
    )
}
