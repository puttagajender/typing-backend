package com.brothers.typing.practice.passage;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.random.RandomGenerator;

@Configuration
class PracticePassageRandomConfiguration {

    @Bean
    RandomGenerator practicePassageRandomGenerator() {
        return RandomGenerator.getDefault();
    }
}
