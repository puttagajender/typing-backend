package com.brothers.typing.learning.recovery.controller;

import com.brothers.typing.learning.recovery.dto.WeakKeyRecoveryRequest;
import com.brothers.typing.learning.recovery.dto.WeakKeyRecoveryResponse;
import com.brothers.typing.learning.recovery.service.WeakKeyRecoveryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/learning/recovery")
public class WeakKeyRecoveryController {

    private final WeakKeyRecoveryService recoveryService;

    public WeakKeyRecoveryController(WeakKeyRecoveryService recoveryService) {
        this.recoveryService = recoveryService;
    }

    @PostMapping("/generate")
    public ResponseEntity<WeakKeyRecoveryResponse> generate(
            @Valid @RequestBody WeakKeyRecoveryRequest request) {
        return ResponseEntity.ok(recoveryService.generate(request));
    }
}
