package bor.tools.stackai.core.interfaces;

import bor.tools.stackai.core.model.Chunk;
import bor.tools.stackai.core.model.DigestStrategy;
import bor.tools.stackai.core.model.DocumentDigestConfig;

import java.net.URI;
import java.util.List;

public interface IDocumentDigest {

    List<Chunk> process(URI source, DocumentDigestConfig config);

    List<List<Chunk>> process(List<URI> sources, DocumentDigestConfig config);

    List<DigestStrategy> supportedStrategies();
}
