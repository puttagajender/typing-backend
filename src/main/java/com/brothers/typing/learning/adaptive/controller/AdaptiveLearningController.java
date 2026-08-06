package com.brothers.typing.learning.adaptive.controller;

import com.brothers.typing.learning.adaptive.dto.NextStepRequest;
import com.brothers.typing.learning.adaptive.dto.NextStepResponse;
import com.brothers.typing.learning.adaptive.service.AdaptiveLearningService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/learning")
public class AdaptiveLearningController {

    private final AdaptiveLearningService adaptiveLearningService;

    public AdaptiveLearningController(AdaptiveLearningService adaptiveLearningService) {
        this.adaptiveLearningService = adaptiveLearningService;
    }

    @PostMapping("/next-step")
    public ResponseEntity<NextStepResponse> nextStep(@Valid @RequestBody NextStepRequest request) {
        return ResponseEntity.ok(adaptiveLearningService.determineNextStep(request));
    }
}
