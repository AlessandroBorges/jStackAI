package io.github.stackai.rag;

import io.github.stackai.core.interfaces.IRag;
import io.github.stackai.core.model.Chunk;
import io.github.stackai.core.model.RagConfig;

import java.net.URI;
import java.util.List;

public class Rag implements IRag {

    @Override
    public String index(URI source, RagConfig config) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<String> index(List<URI> sources, RagConfig config) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<Chunk> search(String query, int n, RagConfig config) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void delete(String documentId, RagConfig config) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
