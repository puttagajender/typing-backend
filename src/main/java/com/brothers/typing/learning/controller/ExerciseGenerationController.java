package com.brothers.typing.learning.controller;

import com.brothers.typing.learning.dto.ExerciseGenerationRequest;
import com.brothers.typing.learning.dto.ExerciseGenerationResponse;
import com.brothers.typing.learning.service.ExerciseGenerationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/learning/exercises")
public class ExerciseGenerationController {

    private final ExerciseGenerationService generationService;

    public ExerciseGenerationController(ExerciseGenerationService generationService) {
        this.generationService = generationService;
    }

    @PostMapping("/generate")
    public ResponseEntity<ExerciseGenerationResponse> generate(
            @Valid @RequestBody ExerciseGenerationRequest request) {
        return ResponseEntity.ok(generationService.generate(request));
    }
}
