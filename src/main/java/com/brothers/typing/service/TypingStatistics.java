package com.brothers.typing.service;

/**
 * Immutable, internally complete result of the typing-statistics calculation.
 * Counts use the same UTF-16 character units as the existing alignment engine.
 *
 * @param totalCharactersTyped number of characters submitted by the typist
 * @param correctCharacters aligned characters that exactly match the reference text
 * @param wrongCharacters aligned substitutions
 * @param missingCharacters reference characters with no corresponding typed character
 * @param extraCharacters typed characters with no corresponding reference character
 * @param netMistakes total alignment errors: wrong + missing + extra
 * @param durationInSeconds positive whole elapsed seconds used by the API
 * @param charactersPerMinute totalCharactersTyped divided by elapsed minutes
 * @param grossWpm charactersPerMinute divided by the conventional five characters per word
 * @param correctWpm correctCharacters divided by five and elapsed minutes
 * @param accuracy percentage of exact matches among all alignment outcomes
 */
record TypingStatistics(
        int totalCharactersTyped,
        int correctCharacters,
        int wrongCharacters,
        int missingCharacters,
        int extraCharacters,
        int netMistakes,
        long durationInSeconds,
        double charactersPerMinute,
        double grossWpm,
        double correctWpm,
        double accuracy
) {
}
