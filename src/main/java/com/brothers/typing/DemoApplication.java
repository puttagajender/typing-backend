package com.brothers.typing;

import com.brothers.typing.config.RecommendationProperties;
import com.brothers.typing.config.WeakKeyProperties;
import com.brothers.typing.learning.recovery.config.WeakKeyRecoveryProperties;
import com.brothers.typing.learning.coach.config.AiCoachingProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
        RecommendationProperties.class,
        WeakKeyProperties.class,
        WeakKeyRecoveryProperties.class,
        AiCoachingProperties.class
})
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

}
