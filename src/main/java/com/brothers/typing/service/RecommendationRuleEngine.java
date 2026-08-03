package com.brothers.typing.service;

public interface RecommendationRuleEngine {

    Recommendation evaluate(RecommendationInput input);
}
