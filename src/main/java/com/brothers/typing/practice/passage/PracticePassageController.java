package com.brothers.typing.practice.passage;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/practice/passages")
public class PracticePassageController {

    private final PracticePassageSelectionService selectionService;

    public PracticePassageController(PracticePassageSelectionService selectionService) {
        this.selectionService = selectionService;
    }

    @GetMapping("/next")
    public ResponseEntity<PracticePassageResponse> next(
            @RequestParam PracticeCategory category,
            @RequestParam PracticeDifficulty difficulty,
            @RequestParam(required = false) String excludeId) {
        PracticePassage passage = selectionService.selectNext(category, difficulty, excludeId);
        return ResponseEntity.ok(PracticePassageResponse.from(passage));
    }
}
