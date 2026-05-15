package com.ssafy.happynurse.domain.nurse.controller;

import com.ssafy.happynurse.domain.nurse.service.NursingRecordSseService;
import com.ssafy.happynurse.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/ai")
@RequiredArgsConstructor
public class AiNursingRecordController {

    private final NursingRecordSseService nursingRecordSseService;

    @PostMapping("/nursing-records/{nursingRecordId}/notify")
    public ResponseEntity<ApiResponse<Void>> notifyCreated(
            @PathVariable Long nursingRecordId
    ) {
        nursingRecordSseService.send(nursingRecordId);
        return ResponseEntity.ok(ApiResponse.ok("SSE 발송 완료.", null));
    }
}