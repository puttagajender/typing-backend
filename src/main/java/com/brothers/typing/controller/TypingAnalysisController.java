package com.brothers.typing.controller;

import com.brothers.typing.dto.TypingAnalysisRequest;
import com.brothers.typing.dto.TypingAnalysisResponse;
import com.brothers.typing.service.TypingAnalysisService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/typing")
public class TypingAnalysisController {

    private static final Logger log = LoggerFactory.getLogger(TypingAnalysisController.class);

    private final TypingAnalysisService typingAnalysisService;

    public TypingAnalysisController(TypingAnalysisService typingAnalysisService) {
        this.typingAnalysisService = typingAnalysisService;
    }

    @PostMapping("/analyze")
    public ResponseEntity<TypingAnalysisResponse> analyze(
            @Valid @RequestBody TypingAnalysisRequest request) {
        log.info("Typing analysis request received");
        log.info("Request validation completed");
        TypingAnalysisResponse response = typingAnalysisService.analyze(request);
        ResponseEntity<TypingAnalysisResponse> responseEntity = ResponseEntity.ok(response);
        log.info("Typing analysis response sent");
        return responseEntity;
    }

        @GetMapping("/hello")
    public String getWelcome(){
        return "Welcome to Putta's world";
    }
}
