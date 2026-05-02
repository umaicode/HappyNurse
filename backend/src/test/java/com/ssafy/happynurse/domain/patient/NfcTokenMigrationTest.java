package com.ssafy.happynurse.domain.patient;

import com.ssafy.happynurse.domain.nfc.util.NfcTokenUtil;
import com.ssafy.happynurse.domain.patient.entity.Patient;
import com.ssafy.happynurse.domain.patient.repository.PatientRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@SpringBootTest
class NfcTokenMigrationTest {

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private NfcTokenUtil nfcTokenUtil;

    @Test
    @Transactional
    @Commit
    @DisplayName("기존 모든 환자들의 NFC 토큰을 새로운 시크릿 키 기반으로 일괄 업데이트한다.")
    void updateAllPatientTokens() {
        List<Patient> patients = patientRepository.findAll();
        int count = 0;

        for (Patient patient : patients) {
            String newToken = nfcTokenUtil.generate(patient.getPatientId());
            patient.updateNfcToken(newToken);
            count++;
        }

        System.out.println("==========================================");
        System.out.println("총 " + count + "명의 환자 NFC 토큰 갱신 완료!");
        System.out.println("==========================================");
    }
}
