package io.github.stackai.core.model;

import io.github.stackai.core.interfaces.IDocumentDigest;
import io.github.stackai.core.interfaces.ILlmClient;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RagConfig {

    String indexPath;
    int vectorDimensions;
    @Builder.Default int maxResults = 10;
    SearchType searchType;
    ILlmClient llmClient;
    LlmModel embeddingModel;
    String library;
    IDocumentDigest documentDigest;
    DocumentDigestConfig digestConfig;
}
