package com.brothers.typing.learning.coach.provider;

import com.brothers.typing.learning.coach.config.AiCoachingProperties;
import jakarta.annotation.PreDestroy;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/** Provider-specific Spring AI adapter; the coaching domain does not depend on a model vendor. */
@Component
public class SpringAiCoachingProvider implements CoachingAiProvider {

    private final ObjectProvider<ChatModel> chatModelProvider;
    private final AiCoachingProperties properties;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "ai-coaching-provider");
        thread.setDaemon(true);
        return thread;
    });

    public SpringAiCoachingProvider(
            ObjectProvider<ChatModel> chatModelProvider,
            AiCoachingProperties properties) {
        this.chatModelProvider = chatModelProvider;
        this.properties = properties;
    }

    @Override
    public boolean isAvailable() {
        return properties.isEnabled()
                && properties.getApiKey() != null
                && !properties.getApiKey().isBlank()
                && chatModelProvider.getIfAvailable() != null;
    }

    @Override
    public String providerName() {
        return properties.getProvider();
    }

    @Override
    public AiProviderResult generate(String prompt, long timeoutMillis, int maximumTokens)
            throws Exception {
        ChatModel chatModel = chatModelProvider.getIfAvailable();
        if (chatModel == null) throw new IllegalStateException("No chat model is configured");
        long started = System.nanoTime();
        Future<String> call = executor.submit(() -> ChatClient.create(chatModel)
                .prompt()
                .options(ChatOptions.builder()
                        .model(properties.getModel())
                        .maxTokens(maximumTokens)
                        .temperature(0.2))
                .user(prompt)
                .call()
                .content());
        try {
            String content = call.get(timeoutMillis, TimeUnit.MILLISECONDS);
            return new AiProviderResult(content, (System.nanoTime() - started) / 1_000_000);
        } catch (Exception exception) {
            call.cancel(true);
            throw exception;
        }
    }

    @PreDestroy
    void close() {
        executor.shutdownNow();
    }
}
