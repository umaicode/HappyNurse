package com.ssafy.happynurse.domain.nfc.repository;

import com.ssafy.happynurse.domain.nfc.entity.NfcTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NfcTagRepository extends JpaRepository<NfcTag, Long> {

    Optional<NfcTag> findByTagUidAndIsActiveTrue(String tagUid);

    Optional<NfcTag> findByTagUid(String tagUid);
}
