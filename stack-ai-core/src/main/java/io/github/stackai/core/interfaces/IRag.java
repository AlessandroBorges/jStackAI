package io.github.stackai.core.interfaces;

import io.github.stackai.core.model.Chunk;
import io.github.stackai.core.model.RagConfig;

import java.net.URI;
import java.util.List;

public interface IRag {

    String index(URI source, RagConfig config);

    List<String> index(List<URI> sources, RagConfig config);

    List<Chunk> search(String query, int n, RagConfig config);

    void delete(String documentId, RagConfig config);
}
