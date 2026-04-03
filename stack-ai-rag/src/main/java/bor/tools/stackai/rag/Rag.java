package bor.tools.stackai.rag;

import bor.tools.stackai.core.interfaces.IRag;
import bor.tools.stackai.core.model.Chunk;
import bor.tools.stackai.core.model.RagConfig;

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
