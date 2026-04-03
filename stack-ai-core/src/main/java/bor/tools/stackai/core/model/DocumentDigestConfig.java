package bor.tools.stackai.core.model;

import bor.tools.stackai.core.interfaces.ILlmClient;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DocumentDigestConfig {

    DigestStrategy strategy;
    /**
     * Chunk size for text splitting. 
     * This is the maximum number of characters in each chunk. 
     * The actual chunk size may be smaller if the text is split at a natural boundary (e.g., sentence, paragraph).
     *  The default value is 4000 characters.
     */
    @Builder.Default int chunkSize = 4000;
    /**
     * <h2><b>Chunk overlap in only used in non-hierarquical documents!!!</b></h2>	
     * Overlap size for text splitting. 
     * 
     * This is the number of characters that overlap between consecutive chunks.
     */
    @Builder.Default int overlap = 200;
    
    String defaultLang;
    String library;
    ILlmClient llmClient;
    LlmModel embeddingModel;
    @Builder.Default boolean generateMetadata = false;
}
