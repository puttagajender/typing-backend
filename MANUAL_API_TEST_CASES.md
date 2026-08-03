# Typing Analysis API — Complete Manual Test Suite

Endpoint: `POST /api/v1/typing/analyze`  
Default content type: `application/json`  
Total cases: **198**

> Contract baseline: this suite targets the response in the latest specification: `grossWpm`, `correctWpm`, `accuracy`, `durationInSeconds`, four mistake counters, and `mistakeDetails`. Code review on 2026-08-03 found the implementation still returns `wpm` and omits `correctWpm` and the three type counters; **RESP-003/004 are therefore expected to expose a Critical contract defect until implementation catches up.**

> Detail positions are expected to be zero-based. Missing details use `typedCharacter:null`; extra details use `expectedCharacter:null`. Placeholder strings such as `<500 exact characters>` are manual data-generation instructions and must be replaced with the described literal string before sending.

## Happy Path

- [ ] **HP-001**
  - **Category:** Happy Path
  - **Scenario:** Exact match
  - **Sample Request JSON:** `{"originalText":"hello","typedText":"hello","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** grossWpm 1.0; correctWpm 1.0; accuracy 100; all counts 0; details [].
  - **Expected Result:** Successful exact analysis.
  - **Priority:** Critical

- [ ] **HP-002**
  - **Category:** Happy Path
  - **Scenario:** One wrong character
  - **Sample Request JSON:** `{"originalText":"cat","typedText":"cut","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** accuracy 66.67; mistakeCount/wrongCharacterCount 1; wrong detail at position 1.
  - **Expected Result:** One substitution classified correctly.
  - **Priority:** Critical

- [ ] **HP-003**
  - **Category:** Happy Path
  - **Scenario:** Multiple wrong characters
  - **Sample Request JSON:** `{"originalText":"abcdef","typedText":"abXYef","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** accuracy 66.67; mistakeCount/wrong count 2; wrong details at 2 and 3.
  - **Expected Result:** All substitutions returned in order.
  - **Priority:** High

- [ ] **HP-004**
  - **Category:** Happy Path
  - **Scenario:** One missing character
  - **Sample Request JSON:** `{"originalText":"cart","typedText":"cat","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** accuracy 75; missing count 1; missing r at position 2.
  - **Expected Result:** Deletion classified correctly.
  - **Priority:** Critical

- [ ] **HP-005**
  - **Category:** Happy Path
  - **Scenario:** Multiple missing characters
  - **Sample Request JSON:** `{"originalText":"abcdef","typedText":"abef","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** accuracy 66.67; missing count 2; c/d missing at 2/3.
  - **Expected Result:** Consecutive deletions returned.
  - **Priority:** High

- [ ] **HP-006**
  - **Category:** Happy Path
  - **Scenario:** One extra character
  - **Sample Request JSON:** `{"originalText":"cat","typedText":"cart","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** accuracy 66.67; extra count 1; extra r at position 2.
  - **Expected Result:** Insertion classified correctly.
  - **Priority:** Critical

- [ ] **HP-007**
  - **Category:** Happy Path
  - **Scenario:** Multiple extra characters
  - **Sample Request JSON:** `{"originalText":"ab","typedText":"axyb","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** accuracy 0; extra count 2; x/y extra at insertion position 1.
  - **Expected Result:** Consecutive insertions returned.
  - **Priority:** High

- [ ] **HP-008**
  - **Category:** Happy Path
  - **Scenario:** Mixed wrong and extra
  - **Sample Request JSON:** `{"originalText":"abc","typedText":"axdc","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** accuracy 33.33; one wrong and one extra; mistakeCount 2.
  - **Expected Result:** Mixed types and counters agree.
  - **Priority:** Critical

- [ ] **HP-009**
  - **Category:** Happy Path
  - **Scenario:** Mixed wrong, missing, extra
  - **Sample Request JSON:** `{"originalText":"abcdef","typedText":"aXcdYfZ","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** Minimum edit details returned; per-type counters sum to mistakeCount.
  - **Expected Result:** Alignment yields a valid minimum edit script.
  - **Priority:** High

- [ ] **HP-010**
  - **Category:** Happy Path
  - **Scenario:** Typing faster
  - **Sample Request JSON:** `{"originalText":"hello","typedText":"hello","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:00:10Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** grossWpm/correctWpm 6.0; accuracy 100; duration 10.
  - **Expected Result:** Shorter duration increases WPM.
  - **Priority:** High

- [ ] **HP-011**
  - **Category:** Happy Path
  - **Scenario:** Typing slower
  - **Sample Request JSON:** `{"originalText":"hello","typedText":"hello","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:05:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** grossWpm/correctWpm 0.2; accuracy 100; duration 300.
  - **Expected Result:** Longer duration lowers WPM.
  - **Priority:** High

- [ ] **HP-012**
  - **Category:** Happy Path
  - **Scenario:** Sentence with punctuation
  - **Sample Request JSON:** `{"originalText":"Please have a good day.","typedText":"Please have a good day.","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** accuracy 100; no mistakes; WPM based on every UTF-16 character.
  - **Expected Result:** Sentence analyzed successfully.
  - **Priority:** Medium

- [ ] **HP-013**
  - **Category:** Happy Path
  - **Scenario:** Correction near beginning
  - **Sample Request JSON:** `{"originalText":"Please","typedText":"Plewase","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** one EXTRA_CHARACTER w at position 3; counters consistent.
  - **Expected Result:** Early insertion aligned correctly.
  - **Priority:** High

- [ ] **HP-014**
  - **Category:** Happy Path
  - **Scenario:** Correction near end
  - **Sample Request JSON:** `{"originalText":"typing","typedText":"typinx","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** one WRONG_CHARACTER g/x at position 5.
  - **Expected Result:** Late substitution aligned correctly.
  - **Priority:** High

- [ ] **HP-015**
  - **Category:** Happy Path
  - **Scenario:** Empty typed attempt currently allowed
  - **Sample Request JSON:** `{"originalText":"hello","typedText":"","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** grossWpm/correctWpm 0; accuracy 0; five missing details.
  - **Expected Result:** Documents current @NotNull behavior.
  - **Priority:** High

- [ ] **HP-016**
  - **Category:** Happy Path
  - **Scenario:** All mistake types absent
  - **Sample Request JSON:** `{"originalText":"A1 !","typedText":"A1 !","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** all three type counters 0 and details empty.
  - **Expected Result:** Zero-count schema is complete.
  - **Priority:** High

## Validation

- [ ] **VAL-001**
  - **Category:** Validation
  - **Scenario:** originalText null
  - **Sample Request JSON:** `{"originalText":null,"typedText":"a","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `400`
  - **Expected Response Summary:** Validation error: originalText must not be blank.
  - **Expected Result:** Null original rejected.
  - **Priority:** Critical

- [ ] **VAL-002**
  - **Category:** Validation
  - **Scenario:** typedText null
  - **Sample Request JSON:** `{"originalText":"a","typedText":null,"startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `400`
  - **Expected Response Summary:** Validation error: typedText is required.
  - **Expected Result:** Null typed rejected.
  - **Priority:** Critical

- [ ] **VAL-003**
  - **Category:** Validation
  - **Scenario:** startedAt null
  - **Sample Request JSON:** `{"originalText":"a","typedText":"a","startedAt":null,"completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `400`
  - **Expected Response Summary:** Validation error: startedAt is required.
  - **Expected Result:** Null start rejected.
  - **Priority:** Critical

- [ ] **VAL-004**
  - **Category:** Validation
  - **Scenario:** completedAt null
  - **Sample Request JSON:** `{"originalText":"a","typedText":"a","startedAt":"2026-08-03T10:00:00Z","completedAt":null}`
  - **Expected HTTP Status:** `400`
  - **Expected Response Summary:** Validation error: completedAt is required.
  - **Expected Result:** Null completion rejected.
  - **Priority:** Critical

- [ ] **VAL-005**
  - **Category:** Validation
  - **Scenario:** Empty originalText
  - **Sample Request JSON:** `{"originalText":"","typedText":"a","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `400`
  - **Expected Response Summary:** Validation error: originalText must not be blank.
  - **Expected Result:** Empty original rejected.
  - **Priority:** Critical

- [ ] **VAL-006**
  - **Category:** Validation
  - **Scenario:** Empty typedText
  - **Sample Request JSON:** `{"originalText":"a","typedText":"","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** One missing character; accuracy 0; WPM 0.
  - **Expected Result:** Current API accepts empty typed text; log gap if forbidden.
  - **Priority:** High

- [ ] **VAL-007**
  - **Category:** Validation
  - **Scenario:** Blank originalText
  - **Sample Request JSON:** `{"originalText":"   ","typedText":"a","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `400`
  - **Expected Response Summary:** Validation error: originalText must not be blank.
  - **Expected Result:** Whitespace-only original rejected.
  - **Priority:** High

- [ ] **VAL-008**
  - **Category:** Validation
  - **Scenario:** Blank typedText
  - **Sample Request JSON:** `{"originalText":"abc","typedText":"   ","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** Three wrong details; accuracy 0.
  - **Expected Result:** Current API accepts blank typed text; log gap if forbidden.
  - **Priority:** High

- [ ] **VAL-009**
  - **Category:** Validation
  - **Scenario:** Very short originalText
  - **Sample Request JSON:** `{"originalText":"a","typedText":"a","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** accuracy 100; no mistakes; valid one-character input.
  - **Expected Result:** Minimum nonblank original accepted.
  - **Priority:** High

- [ ] **VAL-010**
  - **Category:** Validation
  - **Scenario:** Very long originalText
  - **Sample Request JSON:** `{"originalText":"<10000 a characters>","typedText":"<10000 a characters>","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** Expected successful exact result if resources permit; no size validation exists.
  - **Expected Result:** Detect missing maximum and resource risk.
  - **Priority:** High

- [ ] **VAL-011**
  - **Category:** Validation
  - **Scenario:** Very long typedText
  - **Sample Request JSON:** `{"originalText":"a","typedText":"<10000 a characters>","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** Extras reported; accuracy 0 if resources permit.
  - **Expected Result:** Detect missing typed size maximum.
  - **Priority:** High

- [ ] **VAL-012**
  - **Category:** Validation
  - **Scenario:** completedAt before startedAt
  - **Sample Request JSON:** `{"originalText":"a","typedText":"a","startedAt":"2026-08-03T10:01:00Z","completedAt":"2026-08-03T10:00:00Z"}`
  - **Expected HTTP Status:** `400`
  - **Expected Response Summary:** message completedAt must be after startedAt.
  - **Expected Result:** Negative duration rejected.
  - **Priority:** Critical

- [ ] **VAL-013**
  - **Category:** Validation
  - **Scenario:** completedAt equals startedAt
  - **Sample Request JSON:** `{"originalText":"a","typedText":"a","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:00:00Z"}`
  - **Expected HTTP Status:** `400`
  - **Expected Response Summary:** message completedAt must be after startedAt.
  - **Expected Result:** Zero duration rejected.
  - **Priority:** Critical

- [ ] **VAL-014**
  - **Category:** Validation
  - **Scenario:** Sub-second duration
  - **Sample Request JSON:** `{"originalText":"a","typedText":"a","startedAt":"2026-08-03T10:00:00.000Z","completedAt":"2026-08-03T10:00:00.999Z"}`
  - **Expected HTTP Status:** `400`
  - **Expected Response Summary:** Current whole-second duration becomes 0 and is rejected.
  - **Expected Result:** Sub-second truncation documented.
  - **Priority:** Medium

- [ ] **VAL-015**
  - **Category:** Validation
  - **Scenario:** Missing originalText property
  - **Sample Request JSON:** `{"typedText":"a","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `400`
  - **Expected Response Summary:** originalText validation error.
  - **Expected Result:** Omitted original rejected.
  - **Priority:** Critical

- [ ] **VAL-016**
  - **Category:** Validation
  - **Scenario:** Missing typedText property
  - **Sample Request JSON:** `{"originalText":"a","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `400`
  - **Expected Response Summary:** typedText validation error.
  - **Expected Result:** Omitted typed rejected.
  - **Priority:** Critical

- [ ] **VAL-017**
  - **Category:** Validation
  - **Scenario:** Missing both timestamp properties
  - **Sample Request JSON:** `{"originalText":"a","typedText":"a"}`
  - **Expected HTTP Status:** `400`
  - **Expected Response Summary:** Validation map contains startedAt and completedAt.
  - **Expected Result:** All missing-field errors returned.
  - **Priority:** High

- [ ] **VAL-018**
  - **Category:** Validation
  - **Scenario:** All fields null
  - **Sample Request JSON:** `{"originalText":null,"typedText":null,"startedAt":null,"completedAt":null}`
  - **Expected HTTP Status:** `400`
  - **Expected Response Summary:** Validation map contains all four field errors.
  - **Expected Result:** Aggregated validation works.
  - **Priority:** High

## Accuracy

- [ ] **ACC-001**
  - **Category:** Accuracy
  - **Scenario:** 100% accuracy
  - **Sample Request JSON:** `{"originalText":"abcdefghijklmnopqrst","typedText":"abcdefghijklmnopqrst","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** accuracy 100; mistakeCount 0; type counters/details match edits.
  - **Expected Result:** Accuracy formula and rounding correct.
  - **Priority:** Critical

- [ ] **ACC-002**
  - **Category:** Accuracy
  - **Scenario:** 95% accuracy
  - **Sample Request JSON:** `{"originalText":"abcdefghijklmnopqrst","typedText":"Xbcdefghijklmnopqrst","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** accuracy 95; mistakeCount 1; type counters/details match edits.
  - **Expected Result:** Accuracy formula and rounding correct.
  - **Priority:** Critical

- [ ] **ACC-003**
  - **Category:** Accuracy
  - **Scenario:** 90% accuracy
  - **Sample Request JSON:** `{"originalText":"abcdefghijklmnopqrst","typedText":"XYcdefghijklmnopqrst","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** accuracy 90; mistakeCount 2; type counters/details match edits.
  - **Expected Result:** Accuracy formula and rounding correct.
  - **Priority:** Critical

- [ ] **ACC-004**
  - **Category:** Accuracy
  - **Scenario:** 75% accuracy
  - **Sample Request JSON:** `{"originalText":"abcdefghijklmnopqrst","typedText":"abcdeXXXXXXXXXXpqrst","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** accuracy 75; mistakeCount 5; type counters/details match edits.
  - **Expected Result:** Accuracy formula and rounding correct.
  - **Priority:** Critical

- [ ] **ACC-005**
  - **Category:** Accuracy
  - **Scenario:** 50% accuracy
  - **Sample Request JSON:** `{"originalText":"abcdefghijklmnopqrst","typedText":"abcdefghijXXXXXXXXXX","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** accuracy 50; mistakeCount 10; type counters/details match edits.
  - **Expected Result:** Accuracy formula and rounding correct.
  - **Priority:** Critical

- [ ] **ACC-006**
  - **Category:** Accuracy
  - **Scenario:** 25% accuracy
  - **Sample Request JSON:** `{"originalText":"abcdefghijklmnopqrst","typedText":"abcde","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** accuracy 25; mistakeCount 15; type counters/details match edits.
  - **Expected Result:** Accuracy formula and rounding correct.
  - **Priority:** Critical

- [ ] **ACC-007**
  - **Category:** Accuracy
  - **Scenario:** 10% accuracy
  - **Sample Request JSON:** `{"originalText":"abcdefghijklmnopqrst","typedText":"ab","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** accuracy 10; mistakeCount 18; type counters/details match edits.
  - **Expected Result:** Accuracy formula and rounding correct.
  - **Priority:** Critical

- [ ] **ACC-008**
  - **Category:** Accuracy
  - **Scenario:** 0% accuracy
  - **Sample Request JSON:** `{"originalText":"abcdefghijklmnopqrst","typedText":"XXXXXXXXXXXXXXXXXXXX","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** accuracy 0; mistakeCount 20; type counters/details match edits.
  - **Expected Result:** Accuracy formula and rounding correct.
  - **Priority:** Critical

- [ ] **ACC-009**
  - **Category:** Accuracy
  - **Scenario:** 95% using one missing character
  - **Sample Request JSON:** `{"originalText":"abcdefghijklmnopqrst","typedText":"abcdefghijklmnopqrs","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** accuracy 95; missing count 1.
  - **Expected Result:** Deletion reduces accuracy by 5 points.
  - **Priority:** High

- [ ] **ACC-010**
  - **Category:** Accuracy
  - **Scenario:** 95% using one extra character
  - **Sample Request JSON:** `{"originalText":"abcdefghijklmnopqrst","typedText":"abcdefghijklmnopqrstX","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** accuracy 95; extra count 1.
  - **Expected Result:** Insertion reduces accuracy by 5 points.
  - **Priority:** High

- [ ] **ACC-011**
  - **Category:** Accuracy
  - **Scenario:** 75% using mixed edits
  - **Sample Request JSON:** `{"originalText":"abcdefghijklmnopqrst","typedText":"XbcdefghijklmnoqrstY","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** Five minimum edits; accuracy 75; counters sum to 5.
  - **Expected Result:** Mixed edit accuracy correct.
  - **Priority:** High

- [ ] **ACC-012**
  - **Category:** Accuracy
  - **Scenario:** Raw negative accuracy clamped
  - **Sample Request JSON:** `{"originalText":"ab","typedText":"abWXYZ","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** accuracy 0; extra count 4, not a negative value.
  - **Expected Result:** Accuracy remains in [0,100].
  - **Priority:** Critical

## WPM

- [ ] **WPM-001**
  - **Category:** WPM
  - **Scenario:** Very slow typing
  - **Sample Request JSON:** `{"originalText":"hello","typedText":"hello","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:10:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** gross/correct WPM 0.1; duration 600.
  - **Expected Result:** WPM calculation is stable and nonnegative.
  - **Priority:** High

- [ ] **WPM-002**
  - **Category:** WPM
  - **Scenario:** Normal typing
  - **Sample Request JSON:** `{"originalText":"<200 exact characters>","typedText":"<200 exact characters>","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** gross/correct WPM 40.
  - **Expected Result:** WPM calculation is stable and nonnegative.
  - **Priority:** High

- [ ] **WPM-003**
  - **Category:** WPM
  - **Scenario:** Fast typing
  - **Sample Request JSON:** `{"originalText":"<500 exact characters>","typedText":"<500 exact characters>","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** gross/correct WPM 100.
  - **Expected Result:** WPM calculation is stable and nonnegative.
  - **Priority:** High

- [ ] **WPM-004**
  - **Category:** WPM
  - **Scenario:** Extremely fast typing
  - **Sample Request JSON:** `{"originalText":"<500 exact characters>","typedText":"<500 exact characters>","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:00:10Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** gross/correct WPM 600.
  - **Expected Result:** WPM calculation is stable and nonnegative.
  - **Priority:** High

- [ ] **WPM-005**
  - **Category:** WPM
  - **Scenario:** Very short duration
  - **Sample Request JSON:** `{"originalText":"hello","typedText":"hello","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:00:01Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** gross/correct WPM 60.
  - **Expected Result:** WPM calculation is stable and nonnegative.
  - **Priority:** High

- [ ] **WPM-006**
  - **Category:** WPM
  - **Scenario:** Long duration
  - **Sample Request JSON:** `{"originalText":"hello","typedText":"hello","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-04T10:00:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** gross/correct WPM rounds to 0; duration 86400.
  - **Expected Result:** WPM calculation is stable and nonnegative.
  - **Priority:** High

- [ ] **WPM-007**
  - **Category:** WPM
  - **Scenario:** Large text
  - **Sample Request JSON:** `{"originalText":"<5000 exact characters>","typedText":"<5000 exact characters>","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:05:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** gross/correct WPM 200.
  - **Expected Result:** WPM calculation is stable and nonnegative.
  - **Priority:** High

- [ ] **WPM-008**
  - **Category:** WPM
  - **Scenario:** Small text
  - **Sample Request JSON:** `{"originalText":"a","typedText":"a","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** gross/correct WPM 0.2.
  - **Expected Result:** WPM calculation is stable and nonnegative.
  - **Priority:** High

- [ ] **WPM-009**
  - **Category:** WPM
  - **Scenario:** Errors affect correct WPM
  - **Sample Request JSON:** `{"originalText":"abcde","typedText":"abXde","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** grossWpm 1; accuracy 80; correctWpm follows documented correction formula.
  - **Expected Result:** WPM calculation is stable and nonnegative.
  - **Priority:** High

- [ ] **WPM-010**
  - **Category:** WPM
  - **Scenario:** Extras affect gross WPM
  - **Sample Request JSON:** `{"originalText":"abcde","typedText":"abcdeXXXXX","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** grossWpm 2; accuracy 0; correctWpm 0.
  - **Expected Result:** WPM calculation is stable and nonnegative.
  - **Priority:** High

- [ ] **WPM-011**
  - **Category:** WPM
  - **Scenario:** Empty typed text
  - **Sample Request JSON:** `{"originalText":"abcde","typedText":"","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** grossWpm 0; correctWpm 0.
  - **Expected Result:** WPM calculation is stable and nonnegative.
  - **Priority:** High

- [ ] **WPM-012**
  - **Category:** WPM
  - **Scenario:** Two-decimal rounding
  - **Sample Request JSON:** `{"originalText":"abcdefg","typedText":"abcdefg","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:00:37Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** gross/correct WPM rounded to two decimals.
  - **Expected Result:** WPM calculation is stable and nonnegative.
  - **Priority:** High

## Character Comparison

- [ ] **CMP-001**
  - **Category:** Character Comparison
  - **Scenario:** Wrong first character
  - **Sample Request JSON:** `{"originalText":"cat","typedText":"bat","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** wrong c/b at position 0; counters and mistakeCount agree.
  - **Expected Result:** Character alignment and classification correct.
  - **Priority:** High

- [ ] **CMP-002**
  - **Category:** Character Comparison
  - **Scenario:** Wrong last character
  - **Sample Request JSON:** `{"originalText":"cat","typedText":"car","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** wrong t/r at position 2; counters and mistakeCount agree.
  - **Expected Result:** Character alignment and classification correct.
  - **Priority:** High

- [ ] **CMP-003**
  - **Category:** Character Comparison
  - **Scenario:** Missing first character
  - **Sample Request JSON:** `{"originalText":"cat","typedText":"at","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** missing c at position 0; counters and mistakeCount agree.
  - **Expected Result:** Character alignment and classification correct.
  - **Priority:** High

- [ ] **CMP-004**
  - **Category:** Character Comparison
  - **Scenario:** Missing last character
  - **Sample Request JSON:** `{"originalText":"cat","typedText":"ca","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** missing t at position 2; counters and mistakeCount agree.
  - **Expected Result:** Character alignment and classification correct.
  - **Priority:** High

- [ ] **CMP-005**
  - **Category:** Character Comparison
  - **Scenario:** Extra first character
  - **Sample Request JSON:** `{"originalText":"cat","typedText":"xcat","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** extra x at position 0; counters and mistakeCount agree.
  - **Expected Result:** Character alignment and classification correct.
  - **Priority:** High

- [ ] **CMP-006**
  - **Category:** Character Comparison
  - **Scenario:** Extra last character
  - **Sample Request JSON:** `{"originalText":"cat","typedText":"catx","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** extra x at position 3; counters and mistakeCount agree.
  - **Expected Result:** Character alignment and classification correct.
  - **Priority:** High

- [ ] **CMP-007**
  - **Category:** Character Comparison
  - **Scenario:** Wrong middle character
  - **Sample Request JSON:** `{"originalText":"cat","typedText":"cut","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** wrong a/u at position 1; counters and mistakeCount agree.
  - **Expected Result:** Character alignment and classification correct.
  - **Priority:** High

- [ ] **CMP-008**
  - **Category:** Character Comparison
  - **Scenario:** Repeated character missing
  - **Sample Request JSON:** `{"originalText":"book","typedText":"bok","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** one missing o; deterministic valid position; counters and mistakeCount agree.
  - **Expected Result:** Character alignment and classification correct.
  - **Priority:** High

- [ ] **CMP-009**
  - **Category:** Character Comparison
  - **Scenario:** Skipped character
  - **Sample Request JSON:** `{"originalText":"typing","typedText":"tying","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** missing p at position 2; counters and mistakeCount agree.
  - **Expected Result:** Character alignment and classification correct.
  - **Priority:** High

- [ ] **CMP-010**
  - **Category:** Character Comparison
  - **Scenario:** Duplicate character
  - **Sample Request JSON:** `{"originalText":"typing","typedText":"typping","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** extra p at insertion position 3; counters and mistakeCount agree.
  - **Expected Result:** Character alignment and classification correct.
  - **Priority:** High

- [ ] **CMP-011**
  - **Category:** Character Comparison
  - **Scenario:** Adjacent keyboard mistake
  - **Sample Request JSON:** `{"originalText":"test","typedText":"rest","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** wrong t/r at position 0; counters and mistakeCount agree.
  - **Expected Result:** Character alignment and classification correct.
  - **Priority:** High

- [ ] **CMP-012**
  - **Category:** Character Comparison
  - **Scenario:** Case difference
  - **Sample Request JSON:** `{"originalText":"Hello","typedText":"hello","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** wrong H/h at position 0; counters and mistakeCount agree.
  - **Expected Result:** Character alignment and classification correct.
  - **Priority:** High

- [ ] **CMP-013**
  - **Category:** Character Comparison
  - **Scenario:** Transposed adjacent characters
  - **Sample Request JSON:** `{"originalText":"form","typedText":"from","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** minimum edit script count 2; tie-breaking details verified; counters and mistakeCount agree.
  - **Expected Result:** Character alignment and classification correct.
  - **Priority:** High

- [ ] **CMP-014**
  - **Category:** Character Comparison
  - **Scenario:** Consecutive substitutions
  - **Sample Request JSON:** `{"originalText":"abcdef","typedText":"abXYZf","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** three wrong details at positions 2-4; counters and mistakeCount agree.
  - **Expected Result:** Character alignment and classification correct.
  - **Priority:** High

- [ ] **CMP-015**
  - **Category:** Character Comparison
  - **Scenario:** Alternating substitutions
  - **Sample Request JSON:** `{"originalText":"abcdef","typedText":"aXcYeZ","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** wrong details at positions 1,3,5; counters and mistakeCount agree.
  - **Expected Result:** Character alignment and classification correct.
  - **Priority:** High

- [ ] **CMP-016**
  - **Category:** Character Comparison
  - **Scenario:** Repeated whole word omitted
  - **Sample Request JSON:** `{"originalText":"go go home","typedText":"go home","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** three missing characters representing one 'go '; counters and mistakeCount agree.
  - **Expected Result:** Character alignment and classification correct.
  - **Priority:** High

## Spaces

- [ ] **SPC-001**
  - **Category:** Spaces
  - **Scenario:** Leading spaces exact
  - **Sample Request JSON:** `{"originalText":"  abc","typedText":"  abc","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** 100% and no mistakes.
  - **Expected Result:** Whitespace is preserved and handled per validation contract.
  - **Priority:** High

- [ ] **SPC-002**
  - **Category:** Spaces
  - **Scenario:** Leading spaces missing
  - **Sample Request JSON:** `{"originalText":"  abc","typedText":"abc","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** two missing spaces at 0 and 1.
  - **Expected Result:** Whitespace is preserved and handled per validation contract.
  - **Priority:** High

- [ ] **SPC-003**
  - **Category:** Spaces
  - **Scenario:** Leading space extra
  - **Sample Request JSON:** `{"originalText":"abc","typedText":" abc","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** one extra space at 0.
  - **Expected Result:** Whitespace is preserved and handled per validation contract.
  - **Priority:** High

- [ ] **SPC-004**
  - **Category:** Spaces
  - **Scenario:** Trailing spaces exact
  - **Sample Request JSON:** `{"originalText":"abc  ","typedText":"abc  ","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** 100% and no mistakes.
  - **Expected Result:** Whitespace is preserved and handled per validation contract.
  - **Priority:** High

- [ ] **SPC-005**
  - **Category:** Spaces
  - **Scenario:** Trailing spaces missing
  - **Sample Request JSON:** `{"originalText":"abc  ","typedText":"abc","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** two missing spaces at 3 and 4.
  - **Expected Result:** Whitespace is preserved and handled per validation contract.
  - **Priority:** High

- [ ] **SPC-006**
  - **Category:** Spaces
  - **Scenario:** Trailing space extra
  - **Sample Request JSON:** `{"originalText":"abc","typedText":"abc ","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** one extra space at 3.
  - **Expected Result:** Whitespace is preserved and handled per validation contract.
  - **Priority:** High

- [ ] **SPC-007**
  - **Category:** Spaces
  - **Scenario:** Multiple consecutive spaces exact
  - **Sample Request JSON:** `{"originalText":"a   b","typedText":"a   b","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** 100% and no mistakes.
  - **Expected Result:** Whitespace is preserved and handled per validation contract.
  - **Priority:** High

- [ ] **SPC-008**
  - **Category:** Spaces
  - **Scenario:** One consecutive space missing
  - **Sample Request JSON:** `{"originalText":"a   b","typedText":"a  b","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** one missing space.
  - **Expected Result:** Whitespace is preserved and handled per validation contract.
  - **Priority:** High

- [ ] **SPC-009**
  - **Category:** Spaces
  - **Scenario:** Missing word separator
  - **Sample Request JSON:** `{"originalText":"hello world","typedText":"helloworld","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** one missing space at 5.
  - **Expected Result:** Whitespace is preserved and handled per validation contract.
  - **Priority:** High

- [ ] **SPC-010**
  - **Category:** Spaces
  - **Scenario:** Extra word separator
  - **Sample Request JSON:** `{"originalText":"helloworld","typedText":"hello world","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** one extra space at 5.
  - **Expected Result:** Whitespace is preserved and handled per validation contract.
  - **Priority:** High

- [ ] **SPC-011**
  - **Category:** Spaces
  - **Scenario:** Typed text only spaces
  - **Sample Request JSON:** `{"originalText":"abc","typedText":"   ","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** three wrong characters; accuracy 0.
  - **Expected Result:** Whitespace is preserved and handled per validation contract.
  - **Priority:** High

- [ ] **SPC-012**
  - **Category:** Spaces
  - **Scenario:** Original only spaces
  - **Sample Request JSON:** `{"originalText":"   ","typedText":"   ","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `400`
  - **Expected Response Summary:** validation failure for originalText.
  - **Expected Result:** Whitespace is preserved and handled per validation contract.
  - **Priority:** High

## Special Characters

- [ ] **SPEC-001**
  - **Category:** Special Characters
  - **Scenario:** Numbers exact
  - **Sample Request JSON:** `{"originalText":"0123456789","typedText":"0123456789","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** 100%; no mistakes.
  - **Expected Result:** Special content is treated as text data.
  - **Priority:** Medium

- [ ] **SPEC-002**
  - **Category:** Special Characters
  - **Scenario:** Wrong number
  - **Sample Request JSON:** `{"originalText":"12345","typedText":"12395","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** wrong 4/9 at 3.
  - **Expected Result:** Special content is treated as text data.
  - **Priority:** Medium

- [ ] **SPEC-003**
  - **Category:** Special Characters
  - **Scenario:** Symbols exact
  - **Sample Request JSON:** `{"originalText":"!@#$%^&*","typedText":"!@#$%^&*","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** 100%; no mistakes.
  - **Expected Result:** Special content is treated as text data.
  - **Priority:** Medium

- [ ] **SPEC-004**
  - **Category:** Special Characters
  - **Scenario:** Wrong symbol
  - **Sample Request JSON:** `{"originalText":"@#$%","typedText":"@#&%","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** wrong $/& at 2.
  - **Expected Result:** Special content is treated as text data.
  - **Priority:** Medium

- [ ] **SPEC-005**
  - **Category:** Special Characters
  - **Scenario:** Punctuation exact
  - **Sample Request JSON:** `{"originalText":"Hello, world!","typedText":"Hello, world!","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** 100%; no mistakes.
  - **Expected Result:** Special content is treated as text data.
  - **Priority:** Medium

- [ ] **SPEC-006**
  - **Category:** Special Characters
  - **Scenario:** Punctuation changed
  - **Sample Request JSON:** `{"originalText":"Hi, Sam!","typedText":"Hi. Sam?","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** two wrong punctuation details.
  - **Expected Result:** Special content is treated as text data.
  - **Priority:** Medium

- [ ] **SPEC-007**
  - **Category:** Special Characters
  - **Scenario:** Mixed text exact
  - **Sample Request JSON:** `{"originalText":"Ab1! z9?","typedText":"Ab1! z9?","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** 100%; no mistakes.
  - **Expected Result:** Special content is treated as text data.
  - **Priority:** Medium

- [ ] **SPEC-008**
  - **Category:** Special Characters
  - **Scenario:** Double quotes
  - **Sample Request JSON:** `{"originalText":"He said \"Hi\"","typedText":"He said \"Hi\"","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** 100%; JSON quote escaping preserved.
  - **Expected Result:** Special content is treated as text data.
  - **Priority:** Medium

- [ ] **SPEC-009**
  - **Category:** Special Characters
  - **Scenario:** Single quotes
  - **Sample Request JSON:** `{"originalText":"It's fine","typedText":"Its fine","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** missing apostrophe.
  - **Expected Result:** Special content is treated as text data.
  - **Priority:** Medium

- [ ] **SPEC-010**
  - **Category:** Special Characters
  - **Scenario:** Round brackets
  - **Sample Request JSON:** `{"originalText":"a(b)c","typedText":"a[b]c","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** two wrong bracket details.
  - **Expected Result:** Special content is treated as text data.
  - **Priority:** Medium

- [ ] **SPEC-011**
  - **Category:** Special Characters
  - **Scenario:** Mixed brackets
  - **Sample Request JSON:** `{"originalText":"{[()]}","typedText":"{[()]}","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** 100%; no mistakes.
  - **Expected Result:** Special content is treated as text data.
  - **Priority:** Medium

- [ ] **SPEC-012**
  - **Category:** Special Characters
  - **Scenario:** Forward slash
  - **Sample Request JSON:** `{"originalText":"a/b/c","typedText":"a/b/c","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** 100%; slashes preserved.
  - **Expected Result:** Special content is treated as text data.
  - **Priority:** Medium

- [ ] **SPEC-013**
  - **Category:** Special Characters
  - **Scenario:** Backslash
  - **Sample Request JSON:** `{"originalText":"a\\b\\c","typedText":"a\\b\\c","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** 100%; escaped backslashes preserved.
  - **Expected Result:** Special content is treated as text data.
  - **Priority:** Medium

- [ ] **SPEC-014**
  - **Category:** Special Characters
  - **Scenario:** Control escapes
  - **Sample Request JSON:** `{"originalText":"a\nb\tc","typedText":"a\nb c","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** one wrong tab/space detail.
  - **Expected Result:** Special content is treated as text data.
  - **Priority:** Medium

## Unicode

- [ ] **UNI-001**
  - **Category:** Unicode
  - **Scenario:** Hindi exact
  - **Sample Request JSON:** `{"originalText":"नमस्ते","typedText":"नमस्ते","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** 100%; no mistakes.
  - **Expected Result:** Unicode behavior is recorded, including UTF-16 limitations.
  - **Priority:** High

- [ ] **UNI-002**
  - **Category:** Unicode
  - **Scenario:** Hindi difference
  - **Sample Request JSON:** `{"originalText":"नमस्ते","typedText":"नमसते","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** UTF-16 unit-level missing detail.
  - **Expected Result:** Unicode behavior is recorded, including UTF-16 limitations.
  - **Priority:** High

- [ ] **UNI-003**
  - **Category:** Unicode
  - **Scenario:** Telugu exact
  - **Sample Request JSON:** `{"originalText":"నమస్తే","typedText":"నమస్తే","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** 100%; no mistakes.
  - **Expected Result:** Unicode behavior is recorded, including UTF-16 limitations.
  - **Priority:** High

- [ ] **UNI-004**
  - **Category:** Unicode
  - **Scenario:** Telugu difference
  - **Sample Request JSON:** `{"originalText":"తెలుగు","typedText":"తెలగు","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** UTF-16 unit-level missing detail.
  - **Expected Result:** Unicode behavior is recorded, including UTF-16 limitations.
  - **Priority:** High

- [ ] **UNI-005**
  - **Category:** Unicode
  - **Scenario:** Japanese exact
  - **Sample Request JSON:** `{"originalText":"こんにちは","typedText":"こんにちは","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** 100%; no mistakes.
  - **Expected Result:** Unicode behavior is recorded, including UTF-16 limitations.
  - **Priority:** High

- [ ] **UNI-006**
  - **Category:** Unicode
  - **Scenario:** Japanese wrong character
  - **Sample Request JSON:** `{"originalText":"こんにちは","typedText":"こんばんは","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** minimum character edits returned.
  - **Expected Result:** Unicode behavior is recorded, including UTF-16 limitations.
  - **Priority:** High

- [ ] **UNI-007**
  - **Category:** Unicode
  - **Scenario:** Chinese exact
  - **Sample Request JSON:** `{"originalText":"你好世界","typedText":"你好世界","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** 100%; no mistakes.
  - **Expected Result:** Unicode behavior is recorded, including UTF-16 limitations.
  - **Priority:** High

- [ ] **UNI-008**
  - **Category:** Unicode
  - **Scenario:** Chinese wrong character
  - **Sample Request JSON:** `{"originalText":"你好世界","typedText":"您好世界","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** wrong character detail at position 1.
  - **Expected Result:** Unicode behavior is recorded, including UTF-16 limitations.
  - **Priority:** High

- [ ] **UNI-009**
  - **Category:** Unicode
  - **Scenario:** Emoji exact
  - **Sample Request JSON:** `{"originalText":"🙂","typedText":"🙂","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** 100%; WPM uses two UTF-16 units.
  - **Expected Result:** Unicode behavior is recorded, including UTF-16 limitations.
  - **Priority:** High

- [ ] **UNI-010**
  - **Category:** Unicode
  - **Scenario:** Different emoji
  - **Sample Request JSON:** `{"originalText":"🙂","typedText":"🙃","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** UTF-16 surrogate-level detail; expose code-point limitation.
  - **Expected Result:** Unicode behavior is recorded, including UTF-16 limitations.
  - **Priority:** High

- [ ] **UNI-011**
  - **Category:** Unicode
  - **Scenario:** Accent character composed
  - **Sample Request JSON:** `{"originalText":"café","typedText":"cafe","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** wrong é/e at position 3.
  - **Expected Result:** Unicode behavior is recorded, including UTF-16 limitations.
  - **Priority:** High

- [ ] **UNI-012**
  - **Category:** Unicode
  - **Scenario:** Canonical Unicode forms
  - **Sample Request JSON:** `{"originalText":"é","typedText":"é","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** visually equal forms may differ; no normalization is applied.
  - **Expected Result:** Unicode behavior is recorded, including UTF-16 limitations.
  - **Priority:** High

## Large Inputs

- [ ] **LARGE-001**
  - **Category:** Large Inputs
  - **Scenario:** 100 exact characters
  - **Sample Request JSON:** `{"originalText":"<100 a characters>","typedText":"<100 a characters>","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** accuracy 100; gross/correct WPM 20; zero mistakes.
  - **Expected Result:** Completes correctly; record latency and memory.
  - **Priority:** High

- [ ] **LARGE-002**
  - **Category:** Large Inputs
  - **Scenario:** 500 exact characters
  - **Sample Request JSON:** `{"originalText":"<500 a characters>","typedText":"<500 a characters>","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** accuracy 100; gross/correct WPM 100; zero mistakes.
  - **Expected Result:** Completes correctly; record latency and memory.
  - **Priority:** High

- [ ] **LARGE-003**
  - **Category:** Large Inputs
  - **Scenario:** 1000 exact characters
  - **Sample Request JSON:** `{"originalText":"<1000 a characters>","typedText":"<1000 a characters>","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** accuracy 100; gross/correct WPM 200; zero mistakes.
  - **Expected Result:** Completes correctly; record latency and memory.
  - **Priority:** High

- [ ] **LARGE-004**
  - **Category:** Large Inputs
  - **Scenario:** 5000 exact characters
  - **Sample Request JSON:** `{"originalText":"<5000 a characters>","typedText":"<5000 a characters>","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** accuracy 100; gross/correct WPM 1000; zero mistakes.
  - **Expected Result:** Completes correctly; record latency and memory.
  - **Priority:** High

- [ ] **LARGE-005**
  - **Category:** Large Inputs
  - **Scenario:** 100 characters with 10 edits
  - **Sample Request JSON:** `{"originalText":"<100-char original>","typedText":"<same length with 10 seeded substitutions>","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** accuracy 90; wrong count 10.
  - **Expected Result:** Seeded errors all found.
  - **Priority:** High

- [ ] **LARGE-006**
  - **Category:** Large Inputs
  - **Scenario:** 500 characters with mixed edits
  - **Sample Request JSON:** `{"originalText":"<500-char original>","typedText":"<500-char variant with known mixed edits>","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** Counts and details equal seeded minimum edits.
  - **Expected Result:** Large mixed alignment is correct.
  - **Priority:** High

- [ ] **LARGE-007**
  - **Category:** Large Inputs
  - **Scenario:** 1000 typed shorter
  - **Sample Request JSON:** `{"originalText":"<1000-char original>","typedText":"<900-char prefix>","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** 100 missing details; accuracy 90.
  - **Expected Result:** Large trailing deletion works.
  - **Priority:** High

- [ ] **LARGE-008**
  - **Category:** Large Inputs
  - **Scenario:** 5000 exact characters stress
  - **Sample Request JSON:** `{"originalText":"<5000-char original>","typedText":"<same 5000 chars>","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** Successful schema if resources permit.
  - **Expected Result:** Exercise O(n²) matrix; timeout/OOM is defect.
  - **Priority:** Critical

- [ ] **LARGE-009**
  - **Category:** Large Inputs
  - **Scenario:** Maximum supported size boundary
  - **Sample Request JSON:** `{"originalText":"<configured maximum chars>","typedText":"<same chars>","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** Successful exact response at documented maximum.
  - **Expected Result:** Boundary accepted.
  - **Priority:** Critical

- [ ] **LARGE-010**
  - **Category:** Large Inputs
  - **Scenario:** Maximum plus one
  - **Sample Request JSON:** `{"originalText":"<configured maximum+1 chars>","typedText":"<same chars>","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `413`
  - **Expected Response Summary:** Payload/validation rejection without expensive analysis.
  - **Expected Result:** Expose missing maximum if accepted.
  - **Priority:** Critical

## Random Inputs

- [ ] **RND-001**
  - **Category:** Random Inputs
  - **Scenario:** Keyboard smash
  - **Sample Request JSON:** `{"originalText":"The quick brown fox","typedText":"qazwsxedcrfvtgb","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** minimum edit script returned.
  - **Expected Result:** Arbitrary data is handled deterministically.
  - **Priority:** Medium

- [ ] **RND-002**
  - **Category:** Random Inputs
  - **Scenario:** Completely different equal length
  - **Sample Request JSON:** `{"originalText":"abcdefgh","typedText":"12345678","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** eight wrong details; accuracy 0.
  - **Expected Result:** Arbitrary data is handled deterministically.
  - **Priority:** Medium

- [ ] **RND-003**
  - **Category:** Random Inputs
  - **Scenario:** Completely different unequal length
  - **Sample Request JSON:** `{"originalText":"abc","typedText":"WXYZ","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** minimum four edits; accuracy 0.
  - **Expected Result:** Arbitrary data is handled deterministically.
  - **Priority:** Medium

- [ ] **RND-004**
  - **Category:** Random Inputs
  - **Scenario:** Only numbers exact
  - **Sample Request JSON:** `{"originalText":"9876543210","typedText":"9876543210","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** 100%; no mistakes.
  - **Expected Result:** Arbitrary data is handled deterministically.
  - **Priority:** Medium

- [ ] **RND-005**
  - **Category:** Random Inputs
  - **Scenario:** Only random numbers
  - **Sample Request JSON:** `{"originalText":"1234567890","typedText":"9081726354","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** minimum edits returned.
  - **Expected Result:** Arbitrary data is handled deterministically.
  - **Priority:** Medium

- [ ] **RND-006**
  - **Category:** Random Inputs
  - **Scenario:** Only symbols exact
  - **Sample Request JSON:** `{"originalText":"!@#$%^&*","typedText":"!@#$%^&*","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** 100%; no mistakes.
  - **Expected Result:** Arbitrary data is handled deterministically.
  - **Priority:** Medium

- [ ] **RND-007**
  - **Category:** Random Inputs
  - **Scenario:** Only random symbols
  - **Sample Request JSON:** `{"originalText":"!@#$","typedText":"%^&*","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** four wrong details.
  - **Expected Result:** Arbitrary data is handled deterministically.
  - **Priority:** Medium

- [ ] **RND-008**
  - **Category:** Random Inputs
  - **Scenario:** Only emojis exact
  - **Sample Request JSON:** `{"originalText":"🙂🙃🙂","typedText":"🙂🙃🙂","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** 100%; UTF-16 WPM documented.
  - **Expected Result:** Arbitrary data is handled deterministically.
  - **Priority:** Medium

- [ ] **RND-009**
  - **Category:** Random Inputs
  - **Scenario:** Only emojis different
  - **Sample Request JSON:** `{"originalText":"🙂🙂🙂","typedText":"🙃🙃🙃","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** surrogate-level mistake behavior documented.
  - **Expected Result:** Arbitrary data is handled deterministically.
  - **Priority:** Medium

- [ ] **RND-010**
  - **Category:** Random Inputs
  - **Scenario:** Random mixed Unicode
  - **Sample Request JSON:** `{"originalText":"a1🙂 नम","typedText":"Z9🙃 文","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** minimum UTF-16 edit script without crash.
  - **Expected Result:** Arbitrary data is handled deterministically.
  - **Priority:** Medium

## HTTP Validation

- [ ] **HTTP-001**
  - **Category:** HTTP Validation
  - **Scenario:** Missing body
  - **Sample Request JSON:** ``
  - **Expected HTTP Status:** `400`
  - **Expected Response Summary:** Client error for required request body.
  - **Expected Result:** No 500 or stack trace.
  - **Priority:** Critical

- [ ] **HTTP-002**
  - **Category:** HTTP Validation
  - **Scenario:** Empty JSON object
  - **Sample Request JSON:** `{}`
  - **Expected HTTP Status:** `400`
  - **Expected Response Summary:** Validation errors for all four fields.
  - **Expected Result:** Bean validation runs after deserialization.
  - **Priority:** Critical

- [ ] **HTTP-003**
  - **Category:** HTTP Validation
  - **Scenario:** Malformed JSON
  - **Sample Request JSON:** `{"originalText":"a"`
  - **Expected HTTP Status:** `400`
  - **Expected Response Summary:** Message-not-readable error.
  - **Expected Result:** Parser failure handled safely.
  - **Priority:** Critical

- [ ] **HTTP-004**
  - **Category:** HTTP Validation
  - **Scenario:** Invalid JSON token
  - **Sample Request JSON:** `{not-json}`
  - **Expected HTTP Status:** `400`
  - **Expected Response Summary:** Message-not-readable error.
  - **Expected Result:** Invalid token rejected.
  - **Priority:** High

- [ ] **HTTP-005**
  - **Category:** HTTP Validation
  - **Scenario:** originalText object datatype
  - **Sample Request JSON:** `{"originalText":{},"typedText":"a","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `400`
  - **Expected Response Summary:** Deserialization error.
  - **Expected Result:** Wrong text datatype rejected.
  - **Priority:** High

- [ ] **HTTP-006**
  - **Category:** HTTP Validation
  - **Scenario:** typedText array datatype
  - **Sample Request JSON:** `{"originalText":"a","typedText":[],"startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `400`
  - **Expected Response Summary:** Deserialization error.
  - **Expected Result:** Wrong typed datatype rejected.
  - **Priority:** High

- [ ] **HTTP-007**
  - **Category:** HTTP Validation
  - **Scenario:** startedAt number datatype
  - **Sample Request JSON:** `{"originalText":"a","typedText":"a","startedAt":123,"completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `400`
  - **Expected Response Summary:** Deserialization error.
  - **Expected Result:** Wrong timestamp datatype rejected.
  - **Priority:** High

- [ ] **HTTP-008**
  - **Category:** HTTP Validation
  - **Scenario:** Invalid Instant string
  - **Sample Request JSON:** `{"originalText":"a","typedText":"a","startedAt":"yesterday","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `400`
  - **Expected Response Summary:** Deserialization error.
  - **Expected Result:** Non-ISO timestamp rejected.
  - **Priority:** High

- [ ] **HTTP-009**
  - **Category:** HTTP Validation
  - **Scenario:** text/plain content type
  - **Sample Request JSON:** `{"originalText":"a","typedText":"a","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `415`
  - **Expected Response Summary:** Unsupported media type response.
  - **Expected Result:** Only JSON accepted.
  - **Priority:** Critical

- [ ] **HTTP-010**
  - **Category:** HTTP Validation
  - **Scenario:** No Content-Type
  - **Sample Request JSON:** `{"originalText":"a","typedText":"a","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `415`
  - **Expected Response Summary:** Unsupported media type or documented framework response.
  - **Expected Result:** Content negotiation behavior recorded.
  - **Priority:** Medium

- [ ] **HTTP-011**
  - **Category:** HTTP Validation
  - **Scenario:** GET wrong method
  - **Sample Request JSON:** `{}`
  - **Expected HTTP Status:** `405`
  - **Expected Response Summary:** Method not allowed.
  - **Expected Result:** Only POST accepted.
  - **Priority:** High

- [ ] **HTTP-012**
  - **Category:** HTTP Validation
  - **Scenario:** Unknown JSON property
  - **Sample Request JSON:** `{"originalText":"a","typedText":"a","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z","unknown":"x"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** Normal success under default unknown-field handling.
  - **Expected Result:** Unknown-property contract recorded.
  - **Priority:** Medium

## Response Validation

- [ ] **RESP-001**
  - **Category:** Response Validation
  - **Scenario:** HTTP status on valid request
  - **Sample Request JSON:** `{"originalText":"abc","typedText":"axc","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** 200 and JSON body.
  - **Expected Result:** Response contract assertion passes.
  - **Priority:** Critical

- [ ] **RESP-002**
  - **Category:** Response Validation
  - **Scenario:** Content-Type
  - **Sample Request JSON:** `{"originalText":"abc","typedText":"axc","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** application/json.
  - **Expected Result:** Response contract assertion passes.
  - **Priority:** Critical

- [ ] **RESP-003**
  - **Category:** Response Validation
  - **Scenario:** Complete JSON structure
  - **Sample Request JSON:** `{"originalText":"abc","typedText":"axc","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** grossWpm, correctWpm, accuracy, durationInSeconds, mistakeCount, wrongCharacterCount, missingCharacterCount, extraCharacterCount, mistakeDetails all present.
  - **Expected Result:** Response contract assertion passes.
  - **Priority:** Critical

- [ ] **RESP-004**
  - **Category:** Response Validation
  - **Scenario:** Current implementation contract mismatch
  - **Sample Request JSON:** `{"originalText":"abc","typedText":"axc","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** No legacy wpm field; grossWpm/correctWpm and type counters present.
  - **Expected Result:** Response contract assertion passes.
  - **Priority:** Critical

- [ ] **RESP-005**
  - **Category:** Response Validation
  - **Scenario:** Numeric data types
  - **Sample Request JSON:** `{"originalText":"abc","typedText":"axc","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** WPM/accuracy numbers; duration and counts integers.
  - **Expected Result:** Response contract assertion passes.
  - **Priority:** Critical

- [ ] **RESP-006**
  - **Category:** Response Validation
  - **Scenario:** Details data types
  - **Sample Request JSON:** `{"originalText":"abc","typedText":"axc","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** position integer; character string-or-null; mistakeType enum string.
  - **Expected Result:** Response contract assertion passes.
  - **Priority:** Critical

- [ ] **RESP-007**
  - **Category:** Response Validation
  - **Scenario:** Accuracy range
  - **Sample Request JSON:** `{"originalText":"abc","typedText":"axc","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** 0 <= accuracy <= 100.
  - **Expected Result:** Response contract assertion passes.
  - **Priority:** Critical

- [ ] **RESP-008**
  - **Category:** Response Validation
  - **Scenario:** WPM range
  - **Sample Request JSON:** `{"originalText":"abc","typedText":"axc","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** grossWpm >= 0 and correctWpm >= 0.
  - **Expected Result:** Response contract assertion passes.
  - **Priority:** Critical

- [ ] **RESP-009**
  - **Category:** Response Validation
  - **Scenario:** Correct versus gross WPM
  - **Sample Request JSON:** `{"originalText":"abc","typedText":"axc","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** correctWpm <= grossWpm.
  - **Expected Result:** Response contract assertion passes.
  - **Priority:** Critical

- [ ] **RESP-010**
  - **Category:** Response Validation
  - **Scenario:** Mistake count consistency
  - **Sample Request JSON:** `{"originalText":"abc","typedText":"axc","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** mistakeCount equals mistakeDetails length.
  - **Expected Result:** Response contract assertion passes.
  - **Priority:** Critical

- [ ] **RESP-011**
  - **Category:** Response Validation
  - **Scenario:** Type counter consistency
  - **Sample Request JSON:** `{"originalText":"abc","typedText":"axc","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** wrong + missing + extra equals mistakeCount.
  - **Expected Result:** Response contract assertion passes.
  - **Priority:** Critical

- [ ] **RESP-012**
  - **Category:** Response Validation
  - **Scenario:** Null handling
  - **Sample Request JSON:** `{"originalText":"abc","typedText":"axc","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** only absent side of missing/extra detail is null.
  - **Expected Result:** Response contract assertion passes.
  - **Priority:** Critical

- [ ] **RESP-013**
  - **Category:** Response Validation
  - **Scenario:** Detail order
  - **Sample Request JSON:** `{"originalText":"abc","typedText":"axc","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** details follow deterministic text alignment order.
  - **Expected Result:** Response contract assertion passes.
  - **Priority:** Critical

- [ ] **RESP-014**
  - **Category:** Response Validation
  - **Scenario:** Rounding
  - **Sample Request JSON:** `{"originalText":"abc","typedText":"axc","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** WPM and accuracy rounded according to contract, expected two decimals.
  - **Expected Result:** Response contract assertion passes.
  - **Priority:** Critical

## Performance

- [ ] **PERF-001**
  - **Category:** Performance
  - **Scenario:** 100 sequential small requests
  - **Sample Request JSON:** `{"originalText":"hello","typedText":"hello","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** 100/100 responses correct; no failures.
  - **Expected Result:** Measure without test automation code; use an API client runner.
  - **Priority:** High

- [ ] **PERF-002**
  - **Category:** Performance
  - **Scenario:** 500 sequential small requests
  - **Sample Request JSON:** `{"originalText":"hello","typedText":"hello","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** 500/500 responses correct; stable memory.
  - **Expected Result:** Measure without test automation code; use an API client runner.
  - **Priority:** High

- [ ] **PERF-003**
  - **Category:** Performance
  - **Scenario:** 1000 sequential small requests
  - **Sample Request JSON:** `{"originalText":"hello","typedText":"hello","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** 1000/1000 responses correct; no progressive slowdown.
  - **Expected Result:** Measure without test automation code; use an API client runner.
  - **Priority:** High

- [ ] **PERF-004**
  - **Category:** Performance
  - **Scenario:** 100 sequential 500-char requests
  - **Sample Request JSON:** `{"originalText":"hello","typedText":"hello","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** All correct; latency recorded.
  - **Expected Result:** Measure without test automation code; use an API client runner.
  - **Priority:** High

- [ ] **PERF-005**
  - **Category:** Performance
  - **Scenario:** 100 sequential 1000-char requests
  - **Sample Request JSON:** `{"originalText":"hello","typedText":"hello","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** All correct; latency/memory recorded.
  - **Expected Result:** Measure without test automation code; use an API client runner.
  - **Priority:** High

- [ ] **PERF-006**
  - **Category:** Performance
  - **Scenario:** Average response time
  - **Sample Request JSON:** `{"originalText":"hello","typedText":"hello","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** Record warm average, min, max, p50, p95, p99.
  - **Expected Result:** Measure without test automation code; use an API client runner.
  - **Priority:** High

- [ ] **PERF-007**
  - **Category:** Performance
  - **Scenario:** Large mixed-edit response time
  - **Sample Request JSON:** `{"originalText":"hello","typedText":"hello","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** Completes within agreed SLA.
  - **Expected Result:** Measure without test automation code; use an API client runner.
  - **Priority:** High

- [ ] **PERF-008**
  - **Category:** Performance
  - **Scenario:** Recovery after large request
  - **Sample Request JSON:** `{"originalText":"hello","typedText":"hello","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** Following small request returns promptly and correctly.
  - **Expected Result:** Measure without test automation code; use an API client runner.
  - **Priority:** High

## Security

- [ ] **SEC-001**
  - **Category:** Security
  - **Scenario:** HTML tags
  - **Sample Request JSON:** `{"originalText":"<b>Hello</b>","typedText":"<b>Hello</b>","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** treated as text; 100%.
  - **Expected Result:** No execution, persistence, leakage, or server error.
  - **Priority:** Critical

- [ ] **SEC-002**
  - **Category:** Security
  - **Scenario:** JavaScript
  - **Sample Request JSON:** `{"originalText":"<script>alert(1)</script>","typedText":"<script>alert(1)</script>","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** not executed; 100%.
  - **Expected Result:** No execution, persistence, leakage, or server error.
  - **Priority:** Critical

- [ ] **SEC-003**
  - **Category:** Security
  - **Scenario:** SQL injection string
  - **Sample Request JSON:** `{"originalText":"' OR 1=1 --","typedText":"' OR 1=1 --","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** treated as text; no SQL effects.
  - **Expected Result:** No execution, persistence, leakage, or server error.
  - **Priority:** Critical

- [ ] **SEC-004**
  - **Category:** Security
  - **Scenario:** SQL destructive string
  - **Sample Request JSON:** `{"originalText":"'; DROP TABLE users;--","typedText":"'; DROP TABLE users;--","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** treated as text; no internal error.
  - **Expected Result:** No execution, persistence, leakage, or server error.
  - **Priority:** Critical

- [ ] **SEC-005**
  - **Category:** Security
  - **Scenario:** Large payload
  - **Sample Request JSON:** `{"originalText":"<multi-megabyte text>","typedText":"<same multi-megabyte text>","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `413`
  - **Expected Response Summary:** rejected by size limit or safely processed.
  - **Expected Result:** No execution, persistence, leakage, or server error.
  - **Priority:** Critical

- [ ] **SEC-006**
  - **Category:** Security
  - **Scenario:** JSON injection text
  - **Sample Request JSON:** `{"originalText":"\"},\"admin\":true,\"x\":\"","typedText":"\"},\"admin\":true,\"x\":\"","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** escaped string remains data.
  - **Expected Result:** No execution, persistence, leakage, or server error.
  - **Priority:** Critical

- [ ] **SEC-007**
  - **Category:** Security
  - **Scenario:** CRLF content
  - **Sample Request JSON:** `{"originalText":"a\r\nb","typedText":"a\r\nb","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** treated as text; no header injection.
  - **Expected Result:** No execution, persistence, leakage, or server error.
  - **Priority:** Critical

- [ ] **SEC-008**
  - **Category:** Security
  - **Scenario:** Path traversal text
  - **Sample Request JSON:** `{"originalText":"../../etc/passwd","typedText":"../../etc/passwd","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** treated as text.
  - **Expected Result:** No execution, persistence, leakage, or server error.
  - **Priority:** Critical

- [ ] **SEC-009**
  - **Category:** Security
  - **Scenario:** Template expression
  - **Sample Request JSON:** `{"originalText":"${7*7}","typedText":"${7*7}","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** not evaluated.
  - **Expected Result:** No execution, persistence, leakage, or server error.
  - **Priority:** Critical

- [ ] **SEC-010**
  - **Category:** Security
  - **Scenario:** Null escape
  - **Sample Request JSON:** `{"originalText":"a\u0000b","typedText":"a\u0000b","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** valid escaped control character handled without crash.
  - **Expected Result:** No execution, persistence, leakage, or server error.
  - **Priority:** Critical

## Edge Cases

- [ ] **EDGE-001**
  - **Category:** Edge Cases
  - **Scenario:** Original length one exact
  - **Sample Request JSON:** `{"originalText":"a","typedText":"a","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** 100%; zero mistakes.
  - **Expected Result:** Boundary behavior is correct and stable.
  - **Priority:** High

- [ ] **EDGE-002**
  - **Category:** Edge Cases
  - **Scenario:** Typed length one with long original
  - **Sample Request JSON:** `{"originalText":"abcdef","typedText":"a","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** five missing details; accuracy 16.67.
  - **Expected Result:** Boundary behavior is correct and stable.
  - **Priority:** High

- [ ] **EDGE-003**
  - **Category:** Edge Cases
  - **Scenario:** Single character wrong
  - **Sample Request JSON:** `{"originalText":"a","typedText":"b","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** one wrong; accuracy 0.
  - **Expected Result:** Boundary behavior is correct and stable.
  - **Priority:** High

- [ ] **EDGE-004**
  - **Category:** Edge Cases
  - **Scenario:** Original longer than typed
  - **Sample Request JSON:** `{"originalText":"abcdef","typedText":"abc","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** three missing; accuracy 50.
  - **Expected Result:** Boundary behavior is correct and stable.
  - **Priority:** High

- [ ] **EDGE-005**
  - **Category:** Edge Cases
  - **Scenario:** Typed longer than original
  - **Sample Request JSON:** `{"originalText":"abc","typedText":"abcdef","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** three extra; accuracy 0.
  - **Expected Result:** Boundary behavior is correct and stable.
  - **Priority:** High

- [ ] **EDGE-006**
  - **Category:** Edge Cases
  - **Scenario:** Completely empty request
  - **Sample Request JSON:** `{"originalText":null,"typedText":null,"startedAt":null,"completedAt":null}`
  - **Expected HTTP Status:** `400`
  - **Expected Response Summary:** validation errors; timestamps also null.
  - **Expected Result:** Boundary behavior is correct and stable.
  - **Priority:** High

- [ ] **EDGE-007**
  - **Category:** Edge Cases
  - **Scenario:** Empty typed string
  - **Sample Request JSON:** `{"originalText":"a","typedText":"","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** one missing; WPM 0.
  - **Expected Result:** Boundary behavior is correct and stable.
  - **Priority:** High

- [ ] **EDGE-008**
  - **Category:** Edge Cases
  - **Scenario:** Repeated words exact
  - **Sample Request JSON:** `{"originalText":"go go go","typedText":"go go go","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** 100%; no mistakes.
  - **Expected Result:** Boundary behavior is correct and stable.
  - **Priority:** High

- [ ] **EDGE-009**
  - **Category:** Edge Cases
  - **Scenario:** Repeated word missing
  - **Sample Request JSON:** `{"originalText":"go go go","typedText":"go go","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** three missing characters.
  - **Expected Result:** Boundary behavior is correct and stable.
  - **Priority:** High

- [ ] **EDGE-010**
  - **Category:** Edge Cases
  - **Scenario:** Repeated paragraphs exact
  - **Sample Request JSON:** `{"originalText":"Para one.\n\nPara one.","typedText":"Para one.\n\nPara one.","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** 100%; no mistakes.
  - **Expected Result:** Boundary behavior is correct and stable.
  - **Priority:** High

- [ ] **EDGE-011**
  - **Category:** Edge Cases
  - **Scenario:** Repeated paragraph omitted
  - **Sample Request JSON:** `{"originalText":"Para.\n\nPara.","typedText":"Para.","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** missing second separator/paragraph characters.
  - **Expected Result:** Boundary behavior is correct and stable.
  - **Priority:** High

- [ ] **EDGE-012**
  - **Category:** Edge Cases
  - **Scenario:** Very long paragraph
  - **Sample Request JSON:** `{"originalText":"<5000-char paragraph>","typedText":"<same paragraph>","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** 100% if resources permit.
  - **Expected Result:** Boundary behavior is correct and stable.
  - **Priority:** High

- [ ] **EDGE-013**
  - **Category:** Edge Cases
  - **Scenario:** All repeated characters
  - **Sample Request JSON:** `{"originalText":"aaaaaa","typedText":"aaaaa","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** one missing; ambiguity handled deterministically.
  - **Expected Result:** Boundary behavior is correct and stable.
  - **Priority:** High

- [ ] **EDGE-014**
  - **Category:** Edge Cases
  - **Scenario:** Carriage return versus newline
  - **Sample Request JSON:** `{"originalText":"a\r\nb","typedText":"a\nb","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** one missing carriage-return UTF-16 unit.
  - **Expected Result:** Boundary behavior is correct and stable.
  - **Priority:** High

- [ ] **EDGE-015**
  - **Category:** Edge Cases
  - **Scenario:** Trailing newline missing
  - **Sample Request JSON:** `{"originalText":"abc\n","typedText":"abc","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** one missing newline at 3.
  - **Expected Result:** Boundary behavior is correct and stable.
  - **Priority:** High

- [ ] **EDGE-016**
  - **Category:** Edge Cases
  - **Scenario:** Zero-width character
  - **Sample Request JSON:** `{"originalText":"ab","typedText":"a​b","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `200`
  - **Expected Response Summary:** one extra zero-width character at 1.
  - **Expected Result:** Boundary behavior is correct and stable.
  - **Priority:** High

## Future Scenarios

- [ ] **FUT-001**
  - **Category:** Future Scenarios
  - **Scenario:** OpenAI enrichment request
  - **Sample Request JSON:** `{"originalText":"hello","typedText":"hello","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `N/A`
  - **Expected Response Summary:** AI fields not accepted/used by current deterministic contract.
  - **Expected Result:** Do not execute as a current release acceptance test.
  - **Priority:** Low

- [ ] **FUT-002**
  - **Category:** Future Scenarios
  - **Scenario:** OpenAI service unavailable
  - **Sample Request JSON:** `{"originalText":"hello","typedText":"hello","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `N/A`
  - **Expected Response Summary:** Deferred until integration exists.
  - **Expected Result:** Do not execute as a current release acceptance test.
  - **Priority:** Low

- [ ] **FUT-003**
  - **Category:** Future Scenarios
  - **Scenario:** Database persistence
  - **Sample Request JSON:** `{"originalText":"hello","typedText":"hello","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `N/A`
  - **Expected Response Summary:** No record is stored by current stateless API.
  - **Expected Result:** Do not execute as a current release acceptance test.
  - **Priority:** Low

- [ ] **FUT-004**
  - **Category:** Future Scenarios
  - **Scenario:** Database history lookup
  - **Sample Request JSON:** `{"originalText":"hello","typedText":"hello","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `N/A`
  - **Expected Response Summary:** Deferred until persistence endpoint exists.
  - **Expected Result:** Do not execute as a current release acceptance test.
  - **Priority:** Low

- [ ] **FUT-005**
  - **Category:** Future Scenarios
  - **Scenario:** Authentication missing token
  - **Sample Request JSON:** `{"originalText":"hello","typedText":"hello","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `N/A`
  - **Expected Response Summary:** Current endpoint remains accessible; future expected status TBD.
  - **Expected Result:** Do not execute as a current release acceptance test.
  - **Priority:** Low

- [ ] **FUT-006**
  - **Category:** Future Scenarios
  - **Scenario:** Authentication authorization
  - **Sample Request JSON:** `{"originalText":"hello","typedText":"hello","startedAt":"2026-08-03T10:00:00Z","completedAt":"2026-08-03T10:01:00Z"}`
  - **Expected HTTP Status:** `N/A`
  - **Expected Response Summary:** Deferred until user roles and policy exist.
  - **Expected Result:** Do not execute as a current release acceptance test.
  - **Priority:** Low


