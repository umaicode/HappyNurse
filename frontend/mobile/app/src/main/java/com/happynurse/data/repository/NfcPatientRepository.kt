// NFC 태깅 → 환자 정보 통합 Repository
// GET /nfc/patients/entry → patientId 확인 → GET /patient/{patientId} → encounterId + 풍부한 환자 정보
package com.happynurse.data.repository

import com.happynurse.data.remote.apiCall
import com.happynurse.data.remote.api.NfcTokenApi
import com.happynurse.data.remote.api.PatientApi
import com.happynurse.domain.model.NfcPatientInfo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NfcPatientRepository @Inject constructor(
    private val nfcTokenApi: NfcTokenApi,
    private val patientApi: PatientApi,
) {
    suspend fun resolveByToken(token: String): Result<NfcPatientInfo> = runCatching {
        // 1) NFC token → 환자 기본 정보
        val entry = apiCall("NFC 환자 조회 실패") { nfcTokenApi.resolveByToken(token) }.getOrThrow()

        // 2) patientId → encounterId + 풍부한 환자 정보
        val patient = apiCall("환자 정보 조회 실패") { patientApi.getPatient(entry.patientId) }.getOrThrow()

        NfcPatientInfo(
            patientId = patient.patientId,
            encounterId = patient.encounterId,
            patientName = patient.name,
            roomName = patient.roomName ?: entry.roomName,
            bedName = patient.bedName,
            birthDate = patient.birthDate,
            diseaseName = patient.diseaseName,
            chiefComplaint = patient.chiefComplaint,
            surgeryName = patient.surgeryName,
            attendingPhysicianName = patient.attendingPhysicianName,
        )
    }
}
