package io.github.stackai.core.model;

import io.github.stackai.core.interfaces.ILlmClient;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DocumentDigestConfig {

    DigestStrategy strategy;
    @Builder.Default int chunkSize = 1000;
    @Builder.Default int overlap = 100;
    String defaultLang;
    String library;
    ILlmClient llmClient;
    LlmModel embeddingModel;
    @Builder.Default boolean generateMetadata = false;
}
