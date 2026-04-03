package io.github.stackai.core.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LlmModel {

    String id;
    ModelModality modality;
    ModelProfile profile;
    int contextSize;
    int embeddingDimensions;
}
