package io.github.stackai.core.interfaces;

import io.github.stackai.core.model.Chat;
import io.github.stackai.core.model.LlmClientConfig;

public interface ILlmClient {

    String ask(Chat chat, LlmClientConfig config);

    void ask(ResponseStream stream, Chat chat, LlmClientConfig config);

    float[] embed(String text, LlmClientConfig config);
}
