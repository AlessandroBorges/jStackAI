package bor.tools.stackai.llm;

import bor.tools.stackai.core.interfaces.ILlmClient;
import bor.tools.stackai.core.model.ChatMessage;
import bor.tools.stackai.core.model.LlmClientConfig;

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
