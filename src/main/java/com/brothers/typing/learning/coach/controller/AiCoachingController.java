package com.brothers.typing.learning.coach.controller;

import com.brothers.typing.learning.coach.dto.CoachingRequest;
import com.brothers.typing.learning.coach.dto.CoachingResponse;
import com.brothers.typing.learning.coach.service.AiCoachingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/learning")
public class AiCoachingController {

    private final AiCoachingService coachingService;

    public AiCoachingController(AiCoachingService coachingService) {
        this.coachingService = coachingService;
    }

    @PostMapping("/coach")
    public ResponseEntity<CoachingResponse> coach(@Valid @RequestBody CoachingRequest request) {
        return ResponseEntity.ok(coachingService.coach(request));
    }
}
