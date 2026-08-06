package com.brothers.typing.learning.coach.provider;

import com.brothers.typing.learning.coach.config.AiCoachingProperties;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;

class SpringAiCoachingProviderTest {

    @Test
    void missingApiKeyMakesProviderUnavailableWithoutCallingTheModel() {
        AiCoachingProperties properties = new AiCoachingProperties();
        properties.setEnabled(true);
        properties.setApiKey("");
        @SuppressWarnings("unchecked")
        ObjectProvider<ChatModel> models = mock(ObjectProvider.class);
        SpringAiCoachingProvider provider = new SpringAiCoachingProvider(models, properties);

        try {
            assertFalse(provider.isAvailable());
        } finally {
            provider.close();
        }
    }
}
