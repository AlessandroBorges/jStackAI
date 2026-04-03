package io.github.stackai.llm;

import io.github.stackai.core.interfaces.ILlmClient;
import io.github.stackai.core.model.ChatMessage;
import io.github.stackai.core.model.LlmClientConfig;

import java.util.List;

public class LlmClient implements ILlmClient {

    private final LlmClientConfig config;

    public LlmClient(LlmClientConfig config) {
        this.config = config;
    }

    @Override
    public String ask(List<ChatMessage> messages, LlmClientConfig config) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public float[] embed(String text, LlmClientConfig config) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
