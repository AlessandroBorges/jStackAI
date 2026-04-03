package io.github.stackai.llm;

import io.github.stackai.core.interfaces.ILlmClient;
import io.github.stackai.core.interfaces.ResponseStream;
import io.github.stackai.core.model.Chat;
import io.github.stackai.core.model.LlmClientConfig;

public class LlmClient implements ILlmClient {

    private final LlmClientConfig config;

    public LlmClient(LlmClientConfig config) {
        this.config = config;
    }

    @Override
    public String ask(Chat chat, LlmClientConfig config) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void ask(ResponseStream stream, Chat chat, LlmClientConfig config) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public float[] embed(String text, LlmClientConfig config) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
