package com.brothers.typing.learning.coach.service;

import com.brothers.typing.learning.coach.config.AiCoachingProperties;
import com.brothers.typing.learning.coach.dto.CoachingRequest;
import com.brothers.typing.learning.coach.dto.CoachingResponse;
import com.brothers.typing.learning.coach.dto.FingerPerformanceRequest;
import com.brothers.typing.learning.coach.provider.CoachingAiProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class AiCoachingService {

    private static final Logger log = LoggerFactory.getLogger(AiCoachingService.class);

    private final AiCoachingProperties properties;
    private final CoachingAiProvider provider;
    private final CoachingPromptTemplate promptTemplate;
    private final CoachingRuleEngine ruleEngine;
    private final CoachingLessonConfiguration lessonConfiguration;
    private final FallbackCoachingService fallbackService;
    private final CoachingResponseValidator responseValidator;
    private final ObjectMapper objectMapper;

    public AiCoachingService(
            AiCoachingProperties properties,
            CoachingAiProvider provider,
            CoachingPromptTemplate promptTemplate,
            CoachingRuleEngine ruleEngine,
            CoachingLessonConfiguration lessonConfiguration,
            FallbackCoachingService fallbackService,
            CoachingResponseValidator responseValidator,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.provider = provider;
        this.promptTemplate = promptTemplate;
        this.ruleEngine = ruleEngine;
        this.lessonConfiguration = lessonConfiguration;
        this.fallbackService = fallbackService;
        this.responseValidator = responseValidator;
        this.objectMapper = objectMapper;
    }

    public CoachingResponse coach(CoachingRequest request) {
        Set<String> allowedKeys = validate(request);
        boolean ready = ruleEngine.readyForNextLesson(request);
        CoachingResponse fallback = fallbackService.create(request, allowedKeys, ready);
        if (!eligibleForAi(request) || !provider.isAvailable()) {
            return fallbackOrThrow(request, fallback, "not-eligible-or-unavailable");
        }

        String prompt = promptTemplate.create(request, allowedKeys, ready);
        int attempts = properties.isRetryEnabled() ? 2 : 1;
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                CoachingAiProvider.AiProviderResult providerResult = provider.generate(
                        prompt, properties.getTimeoutMillis(), properties.getMaximumResponseTokens());
                AiCoachingDraft draft = parse(providerResult.content());
                CoachingResponse response = responseValidator.validate(
                        draft, fallback, allowedKeys, ready);
                log.info("Coaching generated: lessonId={}, source=ai, provider={}, "
                                + "providerLatencyMillis={}, weakKeyCount={}, success=true",
                        request.lessonId(), provider.providerName(), providerResult.latencyMillis(),
                        request.weakKeys().size());
                return response;
            } catch (JacksonException exception) {
                log.info("Coaching AI response invalid: lessonId={}, provider={}, failureType={}",
                        request.lessonId(), provider.providerName(), exception.getClass().getSimpleName());
                break;
            } catch (Exception exception) {
                log.info("Coaching AI call failed: lessonId={}, provider={}, attempt={}, failureType={}",
                        request.lessonId(), provider.providerName(), attempt,
                        exception.getClass().getSimpleName());
            }
        }
        return fallbackOrThrow(request, fallback, "provider-failure");
    }

    private Set<String> validate(CoachingRequest request) {
        Set<String> lessonKeys = lessonConfiguration.learnedKeys(request.lessonId());
        Set<String> suppliedKeys = new LinkedHashSet<>();
        suppliedKeys.addAll(request.weakKeys());
        suppliedKeys.addAll(request.strongKeys());
        if (request.recoverySummary() != null) {
            suppliedKeys.addAll(request.recoverySummary().keysPractised());
        }
        if (!lessonKeys.containsAll(suppliedKeys)) {
            throw new CoachingRequestException("Coaching request contains an untaught key");
        }
        for (FingerPerformanceRequest performance : request.fingerPerformance()) {
            if (performance.mistakeCount() > performance.attemptCount()) {
                throw new CoachingRequestException(
                        "finger mistakeCount cannot exceed attemptCount");
            }
        }
        return lessonKeys;
    }

    private boolean eligibleForAi(CoachingRequest request) {
        return properties.isEnabled()
                && request.sessionDurationMinutes() >= properties.getMinimumSessionDurationMinutes()
                && request.exerciseCount() >= properties.getMinimumExerciseCount();
    }

    private AiCoachingDraft parse(String content) throws JacksonException {
        return objectMapper.readValue(content == null ? "" : content, AiCoachingDraft.class);
    }

    private CoachingResponse fallbackOrThrow(
            CoachingRequest request, CoachingResponse fallback, String reason) {
        if (!properties.isFallbackEnabled()) {
            throw new CoachingUnavailableException("Coaching is temporarily unavailable");
        }
        log.info("Coaching generated: lessonId={}, source=fallback, providerLatencyMillis=0, "
                        + "weakKeyCount={}, success=true, fallbackReason={}",
                request.lessonId(), request.weakKeys().size(), reason);
        return fallback;
    }
}
