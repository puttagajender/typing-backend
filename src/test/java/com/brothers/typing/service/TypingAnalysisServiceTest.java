package com.brothers.typing.service;

import com.brothers.typing.dto.ComparisonDetailResponse;
import com.brothers.typing.dto.MistakeDetailResponse;
import com.brothers.typing.dto.MistakeType;
import com.brothers.typing.dto.TypingAnalysisRequest;
import com.brothers.typing.dto.TypingAnalysisResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TypingAnalysisServiceTest {

    private static final Instant START = Instant.parse("2026-08-03T10:00:00Z");
    private final TypingAnalysisService service = new TypingAnalysisService(
            RecommendationTestFactory.recommendationService(),
            RecommendationTestFactory.weakKeyAnalysisService());

    @Test
    void exactMatchProducesPerfectAccuracyAndNoMistakes() {
        TypingAnalysisRequest request = request("hello", "hello", 60);

        TypingAnalysisResponse response = service.analyze(request);

        assertEquals(1.0, response.wpm());
        assertEquals(100.0, response.accuracy());
        assertEquals(0, response.mistakeCount());
        assertTrue(response.mistakeDetails().isEmpty());
        assertComparisonTypes(response,
                MistakeType.MATCH, MistakeType.MATCH, MistakeType.MATCH,
                MistakeType.MATCH, MistakeType.MATCH);
    }

    @Test
    void twoPassagesJoinedWithSpaceAreAnalyzedAsContinuousText() {
        String text = "The first passage ends here. The second passage starts now.";

        TypingAnalysisResponse response = service.analyze(request(text, text, 60));

        assertEquals(100.0, response.accuracy());
        assertEquals(0, response.mistakeCount());
        assertEquals(text.length(), response.comparisonDetails().size());
    }

    @Test
    void fiveJoinedPassagesAreAnalyzedWithoutSentenceAssumptions() {
        String text = String.join(" ", List.of(
                "Passage one.", "Passage two.", "Passage three.",
                "Passage four.", "Passage five."));

        TypingAnalysisResponse response = service.analyze(request(text, text, 120));

        assertEquals(100.0, response.accuracy());
        assertEquals(0, response.mistakeCount());
    }

    @Test
    void newlineSeparatedPassagesPreserveSeparators() {
        String original = "First  passage.\nSecond passage.\n\nThird passage.";
        String typed = "First  passage.\nSecond passage.\n\nThird passage.";

        TypingAnalysisResponse response = service.analyze(request(original, typed, 60));

        assertEquals(100.0, response.accuracy());
        assertEquals(0, response.mistakeCount());
        assertTrue(response.comparisonDetails().stream()
                .filter(detail -> detail.expectedCharacter() != null && detail.expectedCharacter() == '\n')
                .allMatch(detail -> detail.mistakeType() == MistakeType.MATCH));
    }

    @Test
    void completedPassageAndPartialNextPassageMarkOnlyRemainingTextMissing() {
        String first = "The first passage is complete.";
        String second = "The next passage is only partly typed.";
        String original = first + " " + second;
        String typed = first + " " + "The next passage";

        TypingAnalysisResponse response = service.analyze(request(original, typed, 30));

        assertEquals(original.length() - typed.length(), response.missingCharacterCount());
        assertEquals(0, response.wrongCharacterCount());
        assertEquals(0, response.extraCharacterCount());
        assertConsistentMistakeTotals(response);
    }

    @Test
    void longValidTimedInputIsAnalyzedWithinExpectedBounds() {
        String original = "continuous typing passage ".repeat(100);
        String typed = original.substring(0, original.length() - 37);

        TypingAnalysisResponse response = service.analyze(request(original, typed, 300));

        assertEquals(300, response.durationInSeconds());
        assertEquals(37, response.missingCharacterCount());
        assertTrue(response.accuracy() >= 0.0 && response.accuracy() <= 100.0);
        assertTrue(response.correctWpm() <= response.grossWpm());
        assertConsistentMistakeTotals(response);
    }

    @Test
    void maximumLengthExactMatchAvoidsQuadraticAlignmentStorage() {
        String text = "a".repeat(6000);

        TypingAnalysisResponse response = service.analyze(request(text, text, 300));

        assertEquals(6000, text.length());
        assertEquals(6000, response.comparisonDetails().size());
        assertEquals(100.0, response.accuracy());
        assertEquals(0, response.mistakeCount());
    }

    @Test
    void trailingSeparatorIsNotTrimmedOrIgnored() {
        TypingAnalysisResponse response = service.analyze(request("First. Second. ", "First. Second.", 60));

        assertEquals(1, response.missingCharacterCount());
        assertEquals(Character.valueOf(' '), response.mistakeDetails().get(0).expectedCharacter());
        assertConsistentMistakeTotals(response);
    }

    @Test
    void oneWrongCharacterIsIdentified() {
        TypingAnalysisRequest request = request("cat", "cut", 60);

        TypingAnalysisResponse response = service.analyze(request);

        assertMistakes(response,
                detail(1, 'a', 'u', MistakeType.WRONG_CHARACTER));
        assertComparisonTypes(response,
                MistakeType.MATCH, MistakeType.WRONG_CHARACTER, MistakeType.MATCH);
    }

    @Test
    void multipleWrongCharactersAreIdentified() {
        TypingAnalysisRequest request = request("abcdef", "abXYef", 60);

        TypingAnalysisResponse response = service.analyze(request);

        assertMistakes(response,
                detail(2, 'c', 'X', MistakeType.WRONG_CHARACTER),
                detail(3, 'd', 'Y', MistakeType.WRONG_CHARACTER));
    }

    @Test
    void oneMissingCharacterIsIdentified() {
        TypingAnalysisRequest request = request("cart", "cat", 60);

        TypingAnalysisResponse response = service.analyze(request);

        assertMistakes(response,
                detail(2, 'r', null, MistakeType.MISSING_CHARACTER));
        assertComparisonTypes(response,
                MistakeType.MATCH, MistakeType.MATCH,
                MistakeType.MISSING_CHARACTER, MistakeType.MATCH);
    }

    @Test
    void multipleMissingCharactersAreIdentified() {
        TypingAnalysisRequest request = request("abcdef", "abef", 60);

        TypingAnalysisResponse response = service.analyze(request);

        assertMistakes(response,
                detail(2, 'c', null, MistakeType.MISSING_CHARACTER),
                detail(3, 'd', null, MistakeType.MISSING_CHARACTER));
    }

    @Test
    void oneExtraCharacterIsIdentified() {
        TypingAnalysisRequest request = request("cat", "cart", 60);

        TypingAnalysisResponse response = service.analyze(request);

        assertMistakes(response,
                detail(2, null, 'r', MistakeType.EXTRA_CHARACTER));
        assertComparisonTypes(response,
                MistakeType.MATCH, MistakeType.MATCH,
                MistakeType.EXTRA_CHARACTER, MistakeType.MATCH);
    }

    @Test
    void multipleExtraCharactersAreIdentified() {
        TypingAnalysisRequest request = request("ab", "axyb", 60);

        TypingAnalysisResponse response = service.analyze(request);

        assertMistakes(response,
                detail(1, null, 'x', MistakeType.EXTRA_CHARACTER),
                detail(1, null, 'y', MistakeType.EXTRA_CHARACTER));
    }

    @Test
    void mixedWrongMissingAndExtraCharactersAreIdentified() {
        TypingAnalysisRequest request = request("abc|def|ghi", "abX|df|ghiZ", 60);

        TypingAnalysisResponse response = service.analyze(request);

        assertEquals(3, response.mistakeCount());
        assertEquals(List.of(
                detail(2, 'c', 'X', MistakeType.WRONG_CHARACTER),
                detail(5, 'e', null, MistakeType.MISSING_CHARACTER),
                detail(11, null, 'Z', MistakeType.EXTRA_CHARACTER)
        ), response.mistakeDetails());
        assertEquals(1, response.wrongCharacterCount());
        assertEquals(1, response.missingCharacterCount());
        assertEquals(1, response.extraCharacterCount());
    }

    @Test
    void wrongFirstCharacterHasPositionZero() {
        TypingAnalysisRequest request = request("cat", "bat", 60);

        TypingAnalysisResponse response = service.analyze(request);

        assertMistakes(response, detail(0, 'c', 'b', MistakeType.WRONG_CHARACTER));
    }

    @Test
    void wrongLastCharacterHasLastPosition() {
        TypingAnalysisRequest request = request("cat", "car", 60);

        TypingAnalysisResponse response = service.analyze(request);

        assertMistakes(response, detail(2, 't', 'r', MistakeType.WRONG_CHARACTER));
    }

    @Test
    void missingFirstCharacterHasPositionZero() {
        TypingAnalysisRequest request = request("cat", "at", 60);

        TypingAnalysisResponse response = service.analyze(request);

        assertMistakes(response, detail(0, 'c', null, MistakeType.MISSING_CHARACTER));
    }

    @Test
    void missingLastCharacterHasLastPosition() {
        TypingAnalysisRequest request = request("cat", "ca", 60);

        TypingAnalysisResponse response = service.analyze(request);

        assertMistakes(response, detail(2, 't', null, MistakeType.MISSING_CHARACTER));
    }

    @Test
    void extraCharacterAtBeginningHasPositionZero() {
        TypingAnalysisRequest request = request("cat", "xcat", 60);

        TypingAnalysisResponse response = service.analyze(request);

        assertMistakes(response, detail(0, null, 'x', MistakeType.EXTRA_CHARACTER));
    }

    @Test
    void extraCharacterAtEndHasPositionEqualToOriginalLength() {
        TypingAnalysisRequest request = request("cat", "catx", 60);

        TypingAnalysisResponse response = service.analyze(request);

        assertMistakes(response, detail(3, null, 'x', MistakeType.EXTRA_CHARACTER));
    }

    @Test
    void comparisonIsCaseSensitive() {
        TypingAnalysisRequest request = request("Hello", "hello", 60);

        TypingAnalysisResponse response = service.analyze(request);

        assertEquals(80.0, response.accuracy());
        assertMistakes(response, detail(0, 'H', 'h', MistakeType.WRONG_CHARACTER));
    }

    @Test
    void spacesAndPunctuationAreComparedAsCharacters() {
        TypingAnalysisRequest request = request("Hi, Sam!", "Hi.Sam?", 60);

        TypingAnalysisResponse response = service.analyze(request);

        assertEquals(3, response.mistakeCount());
        assertTrue(response.mistakeDetails().stream()
                .anyMatch(detail -> detail.expectedCharacter() != null
                        && detail.expectedCharacter() == ' '));
        assertTrue(response.comparisonDetails().stream()
                .anyMatch(detail -> detail.mistakeType() == MistakeType.MATCH));
    }

    @Test
    void insertionInSentenceRealignsAllFollowingCharacters() {
        String original = "Every day I improve my typing skills";
        String typed = "Every day I improve my mtyping skills";
        TypingAnalysisRequest request = request(original, typed, 60);

        TypingAnalysisResponse response = service.analyze(request);

        assertEquals(1, response.mistakeCount());
        assertEquals(1, response.extraCharacterCount());
        ComparisonDetailResponse extra = response.comparisonDetails().stream()
                .filter(detail -> detail.mistakeType() == MistakeType.EXTRA_CHARACTER)
                .findFirst()
                .orElseThrow();
        assertEquals('m', extra.typedCharacter());
        assertEquals(original.length(), response.comparisonDetails().stream()
                .filter(detail -> detail.mistakeType() == MistakeType.MATCH)
                .count());
    }

    @Test
    void repeatedCharactersAreAlignedWithoutCascadingMistakes() {
        TypingAnalysisRequest request = request("book", "boook", 60);

        TypingAnalysisResponse response = service.analyze(request);

        assertEquals(1, response.mistakeCount());
        assertEquals(1, response.extraCharacterCount());
        assertEquals(4, response.comparisonDetails().stream()
                .filter(detail -> detail.mistakeType() == MistakeType.MATCH)
                .count());
        assertEquals(MistakeType.MATCH,
                response.comparisonDetails().get(response.comparisonDetails().size() - 1).mistakeType());
    }

    @Test
    void comparisonDetailsContainBothOriginalAndTypedPositions() {
        TypingAnalysisRequest request = request("abcde", "abXcde", 60);

        TypingAnalysisResponse response = service.analyze(request);

        assertEquals(new ComparisonDetailResponse(
                        2, 2, null, 'X', MistakeType.EXTRA_CHARACTER),
                response.comparisonDetails().get(2));
        assertEquals(new ComparisonDetailResponse(
                        2, 3, 'c', 'c', MistakeType.MATCH),
                response.comparisonDetails().get(3));
    }

    @Test
    void numbersAndSymbolsAreComparedAsCharacters() {
        TypingAnalysisRequest request = request("A1@3", "A2#3", 60);

        TypingAnalysisResponse response = service.analyze(request);

        assertMistakes(response,
                detail(1, '1', '2', MistakeType.WRONG_CHARACTER),
                detail(2, '@', '#', MistakeType.WRONG_CHARACTER));
    }

    @Test
    void completelyDifferentTextProducesZeroAccuracy() {
        TypingAnalysisRequest request = request("abcd", "WXYZ", 60);

        TypingAnalysisResponse response = service.analyze(request);

        assertEquals(0.0, response.accuracy());
        assertEquals(4, response.mistakeCount());
        assertTrue(response.mistakeDetails().stream()
                .allMatch(detail -> detail.mistakeType() == MistakeType.WRONG_CHARACTER));
    }

    @Test
    void typedTextShorterThanOriginalProducesMissingCharacters() {
        TypingAnalysisRequest request = request("abcdef", "abc", 60);

        TypingAnalysisResponse response = service.analyze(request);

        assertEquals(3, response.mistakeCount());
        assertTrue(response.mistakeDetails().stream()
                .allMatch(detail -> detail.mistakeType() == MistakeType.MISSING_CHARACTER));
    }

    @Test
    void typedTextLongerThanOriginalProducesExtraCharacters() {
        TypingAnalysisRequest request = request("abc", "abcdef", 60);

        TypingAnalysisResponse response = service.analyze(request);

        assertEquals(3, response.mistakeCount());
        assertTrue(response.mistakeDetails().stream()
                .allMatch(detail -> detail.mistakeType() == MistakeType.EXTRA_CHARACTER));
    }

    @Test
    void accuracyIsClampedBetweenZeroAndOneHundred() {
        TypingAnalysisRequest request = request("a", "abcdefghij", 60);

        TypingAnalysisResponse response = service.analyze(request);

        assertTrue(response.accuracy() >= 0.0);
        assertTrue(response.accuracy() <= 100.0);
        assertEquals(10.0, response.accuracy());
    }

    @Test
    void wpmUsesFiveCharactersPerWord() {
        TypingAnalysisRequest request = request("abcdefghijklmnopqrst", "abcdefghijklmnopqrst", 60);

        TypingAnalysisResponse response = service.analyze(request);

        assertEquals(4.0, response.grossWpm());
        assertEquals(4.0, response.correctWpm());
        assertEquals(response.correctWpm(), response.wpm());
    }

    @Test
    void correctWpmUsesCorrectlyTypedCharacterCount() {
        TypingAnalysisRequest request = request("abcde", "xbcde", 60);

        TypingAnalysisResponse response = service.analyze(request);

        assertEquals(1.0, response.grossWpm());
        assertEquals(0.8, response.correctWpm());
        assertEquals(response.correctWpm(), response.wpm());
        assertEquals(80.0, response.accuracy());
    }

    @Test
    void observedLowAccuracyExampleDoesNotReportGrossWpmAsCorrectWpm() {
        String original = "a".repeat(99);
        String typed = "a".repeat(19) + "b".repeat(43);
        TypingAnalysisRequest request = request(original, typed, 17);

        TypingAnalysisResponse response = service.analyze(request);

        assertEquals(43.76, response.grossWpm());
        assertEquals(13.41, response.correctWpm());
        assertEquals(19.19, response.accuracy());
        assertTrue(response.correctWpm() <= response.grossWpm());
    }

    @Test
    void correctWpmNeverExceedsGrossWpm() {
        List<TypingAnalysisRequest> requests = List.of(
                request("hello", "hello", 30),
                request("hello", "hxllo", 30),
                request("hello", "he", 30),
                request("he", "hello", 30),
                request("hello", "XXXXX", 30));

        for (TypingAnalysisRequest request : requests) {
            TypingAnalysisResponse response = service.analyze(request);
            assertTrue(response.correctWpm() <= response.grossWpm());
        }
    }

    @Test
    void durationIsCalculatedInWholeSeconds() {
        TypingAnalysisRequest request = request("hello", "hello", 90);

        TypingAnalysisResponse response = service.analyze(request);

        assertEquals(90, response.durationInSeconds());
        assertEquals(0.67, response.wpm());
    }

    @Test
    void mistakeCountAlwaysMatchesMistakeDetailsSize() {
        TypingAnalysisRequest request = request("testing spaces", "Xestin  spaces!", 60);

        TypingAnalysisResponse response = service.analyze(request);

        assertFalse(response.mistakeDetails().isEmpty());
        assertEquals(response.mistakeDetails().size(), response.mistakeCount());
        assertEquals(response.mistakeCount(),
                response.wrongCharacterCount()
                        + response.missingCharacterCount()
                        + response.extraCharacterCount());
    }

    @Test
    void exactMatchReturnsAllMistakeCountsAsZero() {
        TypingAnalysisRequest request = request("hello", "hello", 60);

        TypingAnalysisResponse response = service.analyze(request);

        assertEquals(0, response.mistakeCount());
        assertEquals(0, response.wrongCharacterCount());
        assertEquals(0, response.missingCharacterCount());
        assertEquals(0, response.extraCharacterCount());
    }

    @Test
    void currentImplementationReturnsAllMistakeDetailsWithoutTruncation() {
        String original = "a".repeat(100);
        String typed = "b".repeat(100);
        TypingAnalysisRequest request = request(original, typed, 60);

        TypingAnalysisResponse response = service.analyze(request);

        assertEquals(100, response.mistakeCount());
        assertEquals(100, response.mistakeDetails().size());
    }

    @Test
    void emptyTypedTextIsPermittedAndMarksEveryOriginalCharacterMissing() {
        TypingAnalysisRequest request = request("hello", "", 60);

        TypingAnalysisResponse response = service.analyze(request);

        assertEquals(0.0, response.wpm());
        assertEquals(0.0, response.accuracy());
        assertEquals(5, response.mistakeCount());
        assertTrue(response.mistakeDetails().stream()
                .allMatch(detail -> detail.mistakeType() == MistakeType.MISSING_CHARACTER));
    }

    @Test
    void rejectsCompletionBeforeStart() {
        TypingAnalysisRequest request = new TypingAnalysisRequest(
                "hello", "hello", START, START.minusSeconds(1));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.analyze(request));

        assertEquals("completedAt must be after startedAt", exception.getMessage());
    }

    @Test
    void rejectsCompletionEqualToStart() {
        TypingAnalysisRequest request = new TypingAnalysisRequest(
                "hello", "hello", START, START);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.analyze(request));

        assertEquals("completedAt must be after startedAt", exception.getMessage());
    }

    private TypingAnalysisRequest request(String original, String typed, long seconds) {
        return new TypingAnalysisRequest(original, typed, START, START.plusSeconds(seconds));
    }

    private MistakeDetailResponse detail(
            int position, Character expected, Character typed, MistakeType type) {
        return new MistakeDetailResponse(position, expected, typed, type);
    }

    private void assertMistakes(
            TypingAnalysisResponse response, MistakeDetailResponse... expectedDetails) {
        assertEquals(expectedDetails.length, response.mistakeCount());
        assertEquals(List.of(expectedDetails), response.mistakeDetails());
    }

    private void assertConsistentMistakeTotals(TypingAnalysisResponse response) {
        assertEquals(response.mistakeDetails().size(), response.mistakeCount());
        assertEquals(response.mistakeCount(),
                response.wrongCharacterCount()
                        + response.missingCharacterCount()
                        + response.extraCharacterCount());
    }

    private void assertComparisonTypes(
            TypingAnalysisResponse response, MistakeType... expectedTypes) {
        assertEquals(List.of(expectedTypes), response.comparisonDetails().stream()
                .map(ComparisonDetailResponse::mistakeType)
                .toList());
    }
}
