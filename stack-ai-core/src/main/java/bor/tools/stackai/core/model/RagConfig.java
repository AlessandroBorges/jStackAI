package bor.tools.stackai.core.model;

import bor.tools.stackai.core.interfaces.IDocumentDigest;
import bor.tools.stackai.core.interfaces.ILlmClient;
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
