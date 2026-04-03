package bor.tools.stackai.core.model;

import bor.tools.stackai.core.interfaces.ILlmClient;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class LlmClientConfig {

    String apiKey;
    String baseUrl;
    List<LlmModel> models;
    LlmModel defaultModel;
    LlmModel embeddingModel;
    @Builder.Default int timeoutSeconds = 30;
}
