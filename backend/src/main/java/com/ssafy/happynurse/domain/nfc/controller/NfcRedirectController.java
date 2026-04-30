package com.ssafy.happynurse.domain.nfc.controller;

import com.ssafy.happynurse.domain.nfc.service.NfcService;
import com.ssafy.happynurse.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RequiredArgsConstructor
@RestController
public class NfcRedirectController {

    private final NfcService nfcService;

    @Value("${app.webapp-base-url}")
    private String webappBaseUrl;

    @GetMapping("/nfc/redirect")
    public ResponseEntity<Void> redirect(@RequestParam String token) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(webappBaseUrl);
        try {
            nfcService.resolveToken(token);
            builder.path("/patient").queryParam("token", token);
        } catch (CustomException e) {
            builder.path("/patient/invalid");
        }
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(builder.build().toUri())
                .build();
    }
}
