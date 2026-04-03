package io.github.stackai.core.interfaces;

import io.github.stackai.core.model.ChatMessage;
import io.github.stackai.core.model.LlmClientConfig;

import java.util.List;

public interface ILlmClient {

    String ask(List<ChatMessage> messages, LlmClientConfig config);

    float[] embed(String text, LlmClientConfig config);
}
