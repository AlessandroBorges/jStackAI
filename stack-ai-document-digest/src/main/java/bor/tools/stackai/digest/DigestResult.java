package bor.tools.stackai.digest;

import bor.tools.stackai.core.model.Chunk;
import bor.tools.stackai.core.model.DigestStrategy;
import lombok.Builder;
import lombok.Value;

import java.net.URI;
import java.util.List;

@Value
@Builder
public final class DigestResult {

    String documentId;
    URI source;
    String rawMarkdown;
    List<Chunk> chunks;
    DigestStrategy strategyUsed;
    String detectedLang;
}
