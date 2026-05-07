// NFC 태깅 → 환자 정보 통합 Repository
// GET /nfc/patients/entry → patientId 확인 → GET /patient/{patientId} → encounterId + 풍부한 환자 정보
package com.happynurse.data.repository

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
        val entryRes = nfcTokenApi.resolveByToken(token)
        val entryBody = entryRes.body()
        val entry = if (entryRes.isSuccessful && entryBody?.success == true && entryBody.data != null) {
            entryBody.data
        } else {
            throw Exception(entryBody?.message ?: "NFC 환자 조회 실패 (${entryRes.code()})")
        }

        // 2) patientId → encounterId + 풍부한 환자 정보
        val patientRes = patientApi.getPatient(entry.patientId)
        val patientBody = patientRes.body()
        val patient = if (patientRes.isSuccessful && patientBody?.success == true && patientBody.data != null) {
            patientBody.data
        } else {
            throw Exception(patientBody?.message ?: "환자 정보 조회 실패 (${patientRes.code()})")
        }

        NfcPatientInfo(
            patientId = patient.patientId,
            encounterId = patient.encounterId,
            patientName = patient.name,
            roomName = patient.roomName ?: entry.roomName,
            diseaseName = patient.diseaseName,
            chiefComplaint = patient.chiefComplaint,
            surgeryName = patient.surgeryName,
            attendingPhysicianName = patient.attendingPhysicianName,
        )
    }
}
