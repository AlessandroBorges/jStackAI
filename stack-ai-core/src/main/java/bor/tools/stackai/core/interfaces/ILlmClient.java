package bor.tools.stackai.core.interfaces;

import bor.tools.stackai.core.model.ChatMessage;
import bor.tools.stackai.core.model.LlmClientConfig;

import java.util.List;

public interface ILlmClient {

    String ask(List<ChatMessage> messages, LlmClientConfig config);

    float[] embed(String text, LlmClientConfig config);
}
