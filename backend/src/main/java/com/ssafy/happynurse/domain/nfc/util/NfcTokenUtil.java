package com.ssafy.happynurse.domain.nfc.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;

@Component
public class NfcTokenUtil {

    private final byte[] secretBytes;

    public NfcTokenUtil(@Value("${app.nfc-secret}") String secret) {
        this.secretBytes = secret.getBytes(StandardCharsets.UTF_8);
    }

    public String generate(Long patientId) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretBytes, "HmacSHA256"));
            byte[] hmac = mac.doFinal(String.valueOf(patientId).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hmac);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("NFC token generation failed", e);
        }
    }
}
