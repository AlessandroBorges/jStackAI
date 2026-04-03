package io.github.stackai.core.model;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder(toBuilder = true)
public class Chunk {

    String id;
    String documentId;
    String library;
    ChunkType type;
    int sequential;
    String text;
    String lang;
    float[] vector;
    String parentChunkId;
    Map<String, String> metadata;
}
