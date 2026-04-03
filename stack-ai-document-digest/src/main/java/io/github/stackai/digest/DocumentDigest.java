package io.github.stackai.digest;

import io.github.stackai.core.interfaces.IDocumentDigest;
import io.github.stackai.core.model.Chunk;
import io.github.stackai.core.model.DigestStrategy;
import io.github.stackai.core.model.DocumentDigestConfig;

import java.net.URI;
import java.util.List;

public class DocumentDigest implements IDocumentDigest {

    @Override
    public List<Chunk> process(URI source, DocumentDigestConfig config) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<List<Chunk>> process(List<URI> sources, DocumentDigestConfig config) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<DigestStrategy> supportedStrategies() {
        return List.of(DigestStrategy.HIERARCHICAL, DigestStrategy.NORMATIVE, DigestStrategy.DOCUMENT);
    }

    public DigestResult processToResult(URI source, DocumentDigestConfig config) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public List<DigestResult> processToResult(List<URI> sources, DocumentDigestConfig config) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
