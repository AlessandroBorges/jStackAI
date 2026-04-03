package io.github.stackai.core.interfaces;

import io.github.stackai.core.model.Chunk;
import io.github.stackai.core.model.DigestStrategy;
import io.github.stackai.core.model.DocumentDigestConfig;

import java.net.URI;
import java.util.List;

public interface IDocumentDigest {

    List<Chunk> process(URI source, DocumentDigestConfig config);

    List<List<Chunk>> process(List<URI> sources, DocumentDigestConfig config);

    List<DigestStrategy> supportedStrategies();
}
