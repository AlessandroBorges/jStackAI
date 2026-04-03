# Stack AI

**A modular AI/LLM toolkit for the JVM — pick only what you need.**

Stack AI provides independent, framework-free Java modules for LLM access, document processing, and retrieval-augmented generation (RAG). The design philosophy is [Apache Commons](https://commons.apache.org/): each module is a focused tool that you adopt individually, without pulling in an entire ecosystem.

```
┌─────────────────────────────────────────────────────────────┐
│                        Your Application                      │
│           (Spring Boot, Quarkus, plain Java, etc.)           │
└───────────────────────┬─────────────────────────────────────┘
                        │  instantiates directly, no DI magic
        ┌───────────────┼───────────────────┐
        ▼               ▼                   ▼
 ┌─────────────┐ ┌─────────────────┐ ┌──────────────┐
 │  LlmClient  │ │ DocumentDigest  │ │     Rag      │
 └─────────────┘ └─────────────────┘ └──────────────┘
        │               │                   │
        └───────────────▼───────────────────┘
                  ┌─────────────┐
                  │    Core     │
                  │ interfaces  │
                  │ & models    │
                  └─────────────┘
```

---

## Table of Contents

- [Why Stack AI](#why-stack-ai)
- [Modules](#modules)
- [Architecture](#architecture)
  - [Core Contracts](#core-contracts)
  - [Data Model](#data-model)
  - [Document Processing Pipeline](#document-processing-pipeline)
  - [Language Detection](#language-detection)
  - [RAG and Search Types](#rag-and-search-types)
  - [Dependency Graph](#dependency-graph)
- [Requirements](#requirements)
- [Building](#building)
- [Usage](#usage)
  - [1. LLM Client only](#1-llm-client-only)
  - [2. Document Digest only](#2-document-digest-only)
  - [3. Full RAG pipeline](#3-full-rag-pipeline)
- [Configuration Reference](#configuration-reference)
- [Design Decisions](#design-decisions)
- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [Contributing](#contributing)
- [License](#license)

---

## Why Stack AI

Most JVM AI libraries come bundled with a web framework, a dependency injection container, or opinionated runtime assumptions. Stack AI does not. It is built around three principles:

**Framework independence.** Modules have no dependency on Spring, Quarkus, or any DI container. You instantiate objects directly with `new` and a builder — the same way you would use Apache Commons IO or Guava.

**Modular adoption.** Each module is a separate Maven artifact. If you only need an LLM client, add only `stack-ai-llm-client`. If you need document chunking without RAG, add only `stack-ai-document-digest`. Modules share contracts through `stack-ai-core` and nothing else.

**No hidden magic.** Dependencies between modules are explicit — passed as constructor arguments or builder fields. There is no classpath scanning, no auto-configuration, no proxies.

---

## Modules

| Module | Artifact | Purpose |
|---|---|---|
| **Core** | `stack-ai-core` | Public interfaces and shared data objects. No business logic. |
| **LLM Client** | `stack-ai-llm-client` | Chat, completion and embedding via the OpenAI-compatible API. |
| **Document Digest** | `stack-ai-document-digest` | Parse documents into structured Markdown chunks ready for indexing. |
| **RAG** | `stack-ai-rag` | Index and search documents with Apache Lucene (textual, vector, hybrid). |
| **Examples** | `stack-ai-examples` | Runnable end-to-end demonstrations. Not published as a library. |

---

## Architecture

### Core Contracts

The three interfaces in `stack-ai-core` define the entire public surface of the project. All concrete modules implement one of them; all cross-module communication goes through them.

```java
// LLM access — chat and embeddings
public interface ILlmClient {
    String  ask(List<ChatMessage> messages, LlmClientConfig config);
    float[] embed(String text, LlmClientConfig config);
}

// Document processing — parse any URI into chunks
public interface IDocumentDigest {
    DigestResult       process(URI source, DocumentDigestConfig config);
    List<DigestResult> process(List<URI> sources, DocumentDigestConfig config);
    List<DigestStrategy> supportedStrategies();
}

// Retrieval — index, search and delete
public interface IRag {
    String       index(URI source, RagConfig config);
    List<String> index(List<URI> sources, RagConfig config);
    List<Chunk>  search(String query, int n, RagConfig config);
    void         delete(String documentId, RagConfig config);
}
```

No concrete module imports another concrete module. `ILlmClient` is injected into `DocumentDigestConfig` and `RagConfig` via the interface, keeping the modules fully decoupled.

---

### Data Model

All objects that cross module boundaries live in `stack-ai-core`. They are immutable and built via the Builder pattern.

#### Chunk

The central unit of text that flows through the entire pipeline.

| Field | Type | Set by | Notes |
|---|---|---|---|
| `id` | `String` (UUID) | RAG at index time | Unique chunk identifier |
| `documentId` | `String` (UUID) | DocumentDigest | Groups all chunks of a source document |
| `library` | `String` | Config | Tenant / scope filter for multi-library setups |
| `type` | `ChunkType` | Chunking strategy | See enum below |
| `sequential` | `int` | Chunking strategy | Position within the source document (0-based) |
| `text` | `String` | Chunking strategy | Extracted and cleaned text content |
| `lang` | `String` | Language detection | `"pt_br"`, `"es"`, `"en_us"` — drives Lucene Analyzer selection |
| `vector` | `float[]` | LlmClient (optional) | `null` until embedding is computed |
| `parentChunkId` | `String` | Chunking strategy | `null` for root chunks; set for hierarchical children |
| `metadata` | `Map<String, String>` | Strategy + LlmClient | Enriched when `generateMetadata = true` |

#### ChunkType

```
EXCERPT      Generic sequential fragment (fallback strategy)
SUMMARY      Abstract or executive summary
TITLE        Title or heading
PARAGRAPH    Complete paragraph
SECTION      Logical section
CONCLUSION   Conclusion or final remarks
QUOTE        Citation or reference
TABLE        Textual representation of a table
LIST         List or enumeration
METADATA     Meta-information about the document
```

#### LlmModel

Describes a model and its capabilities. Used to route requests to the correct model within a single `LlmClient`.

```java
LlmModel gpt4o = LlmModel.builder()
    .id("gpt-4o")
    .modality(ModelModality.TEXT)
    .profile(ModelProfile.THINKING)
    .contextSize(ContextTokens.K128)
    .build();

LlmModel ada = LlmModel.builder()
    .id("text-embedding-3-small")
    .modality(ModelModality.EMBEDDING)
    .embeddingDimensions(1536)
    .build();
```

**ModelModality:** `TEXT` · `EMBEDDING` · `VISION` · `IMAGE`

**ModelProfile:** `FAST` · `CODING` · `THINKING` · `IMAGE_TO_TEXT` · `TEXT_TO_IMAGE`

**ContextTokens constants:** `K8` · `K16` · `K32` · `K64` · `K128` · `K256`

---

### Document Processing Pipeline

`DocumentDigest` converts any supported document format into a list of `Chunk` objects. The pipeline has four stages, with **Markdown as the canonical internal format** before chunking.

```
URI  (PDF, DOCX, HTML, TXT, ...)
 │
 ▼
[Apache Tika]
 Extracts text → XHTML  (consistent structure regardless of source format)
 Detects language → "pt" / "es" / "en"
 │
 ▼
[JSoup]
 Cleans and normalizes the XHTML
 Removes noise, fixes malformed structure
 │
 ▼
[FlexMark — html2md-converter]
 Converts clean XHTML → Markdown
 │
 ▼
 Markdown  ◄─── single canonical format before chunking
 │
 ▼
[Strategy: Hierarchical | Normative | Fallback]
 │
 ▼
 List<Chunk>
```

Using Markdown as the intermediate format means every chunking strategy works on predictable, structured text — `#` is unambiguously an H1, `##` an H2, and so on — regardless of whether the source was a PDF, a Word document, or an HTML page.

#### Chunking Strategies

| Strategy | Auto-detected when | Best for |
|---|---|---|
| `HIERARCHICAL` | Structured headings found (`Chapter`, `Section`, `1.`, `1.1`, `1.1.1`) | Technical reports, manuals, structured documentation |
| `NORMATIVE` | Legal patterns found (`Art.`, `Artigo`, `§`, `Parágrafo`, `Inciso`) | Laws, decrees, regulations, standards |
| `DOCUMENT` | Neither pattern found (fallback) | Novels, generic documents, unstructured text |

Strategy is **auto-detected by default**. You can override it explicitly:

```java
DocumentDigestConfig config = DocumentDigestConfig.builder()
    .strategy(DigestStrategy.HIERARCHICAL)   // override auto-detection
    .defaultLang("pt_br")
    .library("my-library-uuid")
    .build();
```

#### DigestResult

The object returned by `DocumentDigest.process()`. It lives in the `document-digest` module and is not part of `core`, since no other module needs it directly.

```java
public final class DigestResult {
    String         documentId;   // UUID generated by DocumentDigest
    URI            source;       // original document URI
    String         rawMarkdown;  // Markdown before chunking (for inspection)
    List<Chunk>    chunks;       // ordered by sequential
    DigestStrategy strategyUsed;
    String         detectedLang; // "pt_br", "es", "en_us"
}
```

The `rawMarkdown` field lets you inspect the intermediate Markdown before chunking, which is useful for debugging pipeline output.

---

### Language Detection

Tika's `LanguageDetector` (powered by the Optimaize library via `tika-langdetect-optimaize`) detects the document language from the clean Markdown text — not from the raw XHTML, which would introduce tag noise.

The detected language is normalized and stored in `Chunk.lang`:

| Tika output | Normalized `lang` | Lucene Analyzer |
|---|---|---|
| `pt` | `pt_br` | `BrazilianAnalyzer` |
| `es` | `es` | `SpanishAnalyzer` |
| `en` | `en_us` | `EnglishAnalyzer` |
| anything else | original code | `StandardAnalyzer` (fallback) |

The RAG module uses `Chunk.lang` to **select the correct Lucene Analyzer per document** at both index time and query time, ensuring that stemming and stop-word filtering are applied correctly for each language.

---

### RAG and Search Types

`Rag` is document-oriented: you give it a URI, it delegates chunking to `DocumentDigest`, and returns a `documentId` that serves as the key for all subsequent operations.

The Lucene index stores each chunk as a document with the following fields:

| Field | Lucene Type | Purpose |
|---|---|---|
| `id` | `StringField` | Exact chunk lookup |
| `document_id` | `StringField` | Group / filter all chunks of a document |
| `library` | `StringField` | Tenant scope filter |
| `chunk_type` | `StringField` | Filter by `ChunkType` |
| `parent_chunk_id` | `StringField` | Hierarchical navigation |
| `sequential` | `IntPoint` | Ordering within document |
| `text` | `TextField` | Full-text search (language analyzer applied) |
| `vector` | `KnnFloatVectorField` | Semantic similarity (HNSW / cosine) |
| `lang` | `StringField` | Drives Analyzer selection |
| `metadata.*` | `TextField` / `StringField` | Enriched metadata search |

Three search modes are supported:

| `SearchType` | How it works | When to use |
|---|---|---|
| `TEXTUAL` | BooleanQuery on `TextField` with language-aware analyzers | No embeddings available; fast keyword search |
| `VECTOR` | `KnnFloatVectorQuery` with cosine similarity (HNSW) | Semantic understanding; requires embeddings |
| `HYBRID` | Combines textual score + vector similarity | Best overall recall; recommended default |

---

### Dependency Graph

```
stack-ai-core
      ▲              ▲              ▲
      │              │              │
 llm-client    document-digest     rag ──────► document-digest
                                    ▲
                                    │
                                examples ◄──── llm-client
                                         ◄──── document-digest
                                         ◄──── core
```

Key rules enforced by this graph:

- `llm-client` does **not** know `document-digest` or `rag`.
- `document-digest` does **not** know `llm-client` or `rag`. `ILlmClient` arrives via `DocumentDigestConfig`.
- `rag` does **not** know `llm-client`. `ILlmClient` arrives via `RagConfig`.
- Only `examples` holds all modules together.

---

## Requirements

- **Java 21** or later (required by Apache Lucene 10.x)
- **Maven 3.9.x** or later
- An OpenAI-compatible API key (for LLM and embedding features)

---

## Building

Clone the repository and build all modules from the root:

```bash
git clone https://github.com/<your-org>/stack-ai.git
cd stack-ai
mvn install
```

To build and run the examples fat JAR:

```bash
mvn package -pl stack-ai-examples -am
java -jar stack-ai-examples/target/stack-ai-examples-*-jar-with-dependencies.jar
```

To run tests for a specific module:

```bash
mvn test -pl stack-ai-rag
```

---

## Usage

### 1. LLM Client only

Add the dependency:

```xml
<dependency>
    <groupId>io.github.stackai</groupId>
    <artifactId>stack-ai-llm-client</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

Configure and use:

```java
LlmModel gpt4o = LlmModel.builder()
    .id("gpt-4o")
    .modality(ModelModality.TEXT)
    .profile(ModelProfile.THINKING)
    .contextSize(ContextTokens.K128)
    .build();

LlmModel ada = LlmModel.builder()
    .id("text-embedding-3-small")
    .modality(ModelModality.EMBEDDING)
    .embeddingDimensions(1536)
    .build();

LlmClientConfig config = LlmClientConfig.builder()
    .apiKey(System.getenv("OPENAI_API_KEY"))
    .baseUrl("https://api.openai.com/v1")
    .models(List.of(gpt4o, ada))
    .defaultModel(gpt4o)
    .embeddingModel(ada)
    .timeoutSeconds(60)
    .build();

LlmClient client = new LlmClient(config);

// Chat
String answer = client.ask(
    List.of(ChatMessage.user("Explain RAG in one sentence.")),
    config
);

// Embedding
float[] vector = client.embed("text to index", config);
```

The `baseUrl` field makes it easy to switch to a local provider:

```java
// Ollama running locally
.baseUrl("http://localhost:11434/v1")

// LM Studio
.baseUrl("http://localhost:1234/v1")

// Groq
.baseUrl("https://api.groq.com/openai/v1")
```

---

### 2. Document Digest only

Add the dependency:

```xml
<dependency>
    <groupId>io.github.stackai</groupId>
    <artifactId>stack-ai-document-digest</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

**Without automatic embedding** (strategy auto-detected):

```java
DocumentDigestConfig config = DocumentDigestConfig.builder()
    .defaultLang("pt_br")
    .library("my-library-uuid")
    .build();

DocumentDigest digest = new DocumentDigest();
DigestResult result = digest.process(new URI("file:///home/user/report.pdf"), config);

System.out.println(result.getDocumentId());    // UUID
System.out.println(result.getStrategyUsed());  // e.g. HIERARCHICAL
System.out.println(result.getDetectedLang());  // e.g. "pt_br"
System.out.println(result.getRawMarkdown());   // Markdown before chunking

result.getChunks().forEach(chunk ->
    System.out.printf("%d | %s | %s%n",
        chunk.getSequential(), chunk.getType(), chunk.getText())
);
```

**With automatic embedding and forced strategy:**

```java
DocumentDigestConfig config = DocumentDigestConfig.builder()
    .strategy(DigestStrategy.NORMATIVE)  // override auto-detection
    .llmClient(myLlmClient)
    .embeddingModel(adaModel)
    .generateMetadata(true)
    .library("legal-docs")
    .build();

// Multiple documents — each gets its own documentId
List<URI> sources = List.of(
    new URI("file:///laws/lei8666.pdf"),
    new URI("https://example.gov/decree.pdf")
);

List<DigestResult> results = digest.process(sources, config);
```

---

### 3. Full RAG pipeline

Add the dependency (transitively includes `core` and `document-digest`):

```xml
<dependency>
    <groupId>io.github.stackai</groupId>
    <artifactId>stack-ai-rag</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
<dependency>
    <groupId>io.github.stackai</groupId>
    <artifactId>stack-ai-llm-client</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

**Step 1 — Configure the LLM client:**

```java
LlmModel gpt4o = LlmModel.builder()
    .id("gpt-4o")
    .modality(ModelModality.TEXT)
    .profile(ModelProfile.THINKING)
    .contextSize(ContextTokens.K128)
    .build();

LlmModel ada = LlmModel.builder()
    .id("text-embedding-3-small")
    .modality(ModelModality.EMBEDDING)
    .embeddingDimensions(1536)
    .build();

LlmClientConfig clientConfig = LlmClientConfig.builder()
    .apiKey(System.getenv("OPENAI_API_KEY"))
    .baseUrl("https://api.openai.com/v1")
    .defaultModel(gpt4o)
    .embeddingModel(ada)
    .timeoutSeconds(60)
    .build();

LlmClient llmClient = new LlmClient(clientConfig);
```

**Step 2 — Configure DocumentDigest:**

```java
DocumentDigestConfig digestConfig = DocumentDigestConfig.builder()
    .llmClient(llmClient)           // enables auto-embedding per chunk
    .embeddingModel(ada)
    .generateMetadata(true)         // uses LLM to enrich chunk metadata
    .library("my-library-uuid")
    .build();                       // strategy auto-detected per document
```

**Step 3 — Configure and use the RAG:**

```java
RagConfig ragConfig = RagConfig.builder()
    .indexPath("/var/data/my-lucene-index")
    .vectorDimensions(1536)         // must match the embedding model
    .searchType(SearchType.HYBRID)  // TEXTUAL | VECTOR | HYBRID
    .llmClient(llmClient)
    .embeddingModel(ada)
    .library("my-library-uuid")
    .documentDigest(new DocumentDigest())
    .digestConfig(digestConfig)
    .maxResults(5)
    .build();

Rag rag = new Rag();
```

**Step 4 — Index a document:**

```java
// RAG delegates chunking, embedding and metadata to DocumentDigest internally
String docId = rag.index(new URI("file:///docs/contract.pdf"), ragConfig);
System.out.println("Indexed as: " + docId);

// Index multiple documents at once
List<String> docIds = rag.index(
    List.of(new URI("file:///laws/lei8666.pdf"),
            new URI("https://example.gov/regulation.pdf")),
    ragConfig
);
```

**Step 5 — Search and generate an answer:**

```java
List<Chunk> relevant = rag.search("administrative appeal deadline", 5, ragConfig);

String context = relevant.stream()
    .map(Chunk::getText)
    .collect(Collectors.joining("\n\n"));

String answer = llmClient.ask(
    List.of(
        ChatMessage.system("Answer using only the context provided below."),
        ChatMessage.system(context),
        ChatMessage.user("What is the administrative appeal deadline?")
    ),
    clientConfig
);

System.out.println(answer);
```

**Step 6 — Remove a document:**

```java
// Removes all chunks associated with the documentId (cascade)
rag.delete(docId, ragConfig);
```

---

## Configuration Reference

### LlmClientConfig

| Field | Type | Required | Description |
|---|---|---|---|
| `apiKey` | `String` | Yes | API key for the LLM provider |
| `baseUrl` | `String` | Yes | Base URL of the API (OpenAI-compatible) |
| `models` | `List<LlmModel>` | No | Available model catalog |
| `defaultModel` | `LlmModel` | Yes | Used when no model is specified in the call |
| `embeddingModel` | `LlmModel` | For embeddings | Model used by `embed()` |
| `timeoutSeconds` | `int` | No | HTTP timeout (default: 30) |

### DocumentDigestConfig

| Field | Type | Required | Description |
|---|---|---|---|
| `strategy` | `DigestStrategy` | No | `null` = auto-detect |
| `chunkSize` | `int` | No | Max characters per chunk (fallback strategy) |
| `overlap` | `int` | No | Character overlap between chunks |
| `defaultLang` | `String` | No | Used when language detection is inconclusive |
| `library` | `String` | Yes | Tenant / scope identifier |
| `llmClient` | `ILlmClient` | No | Required for auto-embedding and metadata enrichment |
| `embeddingModel` | `LlmModel` | If llmClient set | Embedding model for vectorizing chunks |
| `generateMetadata` | `boolean` | No | Use LLM to enrich chunk metadata (default: `false`) |

### RagConfig

| Field | Type | Required | Description |
|---|---|---|---|
| `indexPath` | `String` | Yes | Lucene index directory path |
| `vectorDimensions` | `int` | For vector/hybrid | Must match the embedding model dimensions |
| `maxResults` | `int` | No | Default result limit for `search()` |
| `searchType` | `SearchType` | Yes | `TEXTUAL`, `VECTOR`, or `HYBRID` |
| `llmClient` | `ILlmClient` | For vector/hybrid | Vectorizes the search query |
| `embeddingModel` | `LlmModel` | For vector/hybrid | Embedding model for the query vector |
| `library` | `String` | Yes | Tenant scope filter applied to all queries |
| `documentDigest` | `IDocumentDigest` | Yes | Chunking implementation |
| `digestConfig` | `DocumentDigestConfig` | Yes | Config passed to `documentDigest` |

---

## Design Decisions

| # | Decision | Rationale |
|---|---|---|
| D-01 | Pure Java — no Kotlin, no Scala | Maximum IDE and build tool compatibility |
| D-02 | No DI framework in modules | Direct instantiation; zero magic; portable anywhere |
| D-03 | Interfaces extracted only when needed | `ILlmClient`, `IDocumentDigest`, `IRag` exist because multiple implementations are planned |
| D-04 | Config objects as injection points | Optional cross-module dependencies appear only in config builders |
| D-05 | `documentId` generated by `DocumentDigest` | Caller should not manage document identity |
| D-06 | `sequential` relative to source document | No global sequence across documents |
| D-07 | `DigestResult` stays in `document-digest` | No other module needs it; keeps `core` minimal |
| D-08 | `URI` as universal document reference | Covers `file:///`, `http://`, `https://` without changing the contract |
| D-09 | Monorepo on GitHub | Modules evolve together; atomic PRs; unified CI |
| D-10 | Markdown as canonical internal format | Predictable structure for chunking strategies regardless of source format |
| D-11 | Pipeline: Tika → JSoup → FlexMark | Each library has a single responsibility; XHTML as the only intermediary |
| D-12 | Language detection on clean Markdown text | More reliable than detecting on raw XHTML with tag noise |
| D-13 | `Chunk.lang` drives Lucene Analyzer selection | Correct stemming and stop-word filtering per language at index and query time |
| D-14 | `tika-langdetect-optimaize` as optional module | Pay the dependency cost only when detection is needed |
| D-15 | Tika with selective parsers | Lean artifact; consistent with minimal-dependency philosophy |
| D-16 | `rawMarkdown` exposed in `DigestResult` | Allows pipeline inspection without polluting `Chunk` |
| D-17 | Apache Lucene 10.4.0 | Java 21 minimum aligns with the project baseline |

---

## Technology Stack

| Library | Version | Role |
|---|---|---|
| Java | 21 | Language and runtime |
| Apache Lucene | 10.4.0 | Textual, vector (HNSW) and hybrid search |
| Apache Tika | 2.9.2 | Document parsing and language detection |
| Apache Commons Lang | 3.17.0 | String utilities and validation |
| Apache Commons IO | 2.17.0 | I/O utilities for URI and stream handling |
| OkHttp | 4.12.0 | HTTP client for LLM API calls |
| Jackson Databind | 2.18.2 | JSON serialization for OpenAI-format payloads |
| JSoup | 1.18.3 | XHTML cleanup and normalization |
| FlexMark | 0.64.8 | XHTML → Markdown conversion and AST parsing |
| Lombok | 1.18.36 | Builder and accessor generation |
| JUnit Jupiter | 5.11.3 | Unit testing |
| AssertJ | 3.26.3 | Fluent test assertions |

---

## Project Structure

```
stack-ai/
├── pom.xml                                    ← parent POM
│
├── stack-ai-core/
│   └── src/main/java/io/github/stackai/core/
│       ├── interfaces/
│       │   ├── ILlmClient.java
│       │   ├── IDocumentDigest.java
│       │   └── IRag.java
│       └── model/
│           ├── Chunk.java
│           ├── ChunkType.java
│           ├── ChatMessage.java
│           ├── LlmModel.java
│           ├── ModelModality.java
│           ├── ModelProfile.java
│           └── ContextTokens.java
│
├── stack-ai-llm-client/
│   └── src/main/java/io/github/stackai/llm/
│       ├── LlmClient.java
│       └── LlmClientConfig.java
│
├── stack-ai-document-digest/
│   └── src/main/java/io/github/stackai/digest/
│       ├── DocumentDigest.java
│       ├── DocumentDigestConfig.java
│       ├── DigestStrategy.java
│       ├── DigestResult.java
│       └── strategy/
│           ├── HierarchicalDigest.java
│           ├── NormativeDigest.java
│           └── DocumentFallbackDigest.java
│
├── stack-ai-rag/
│   └── src/main/java/io/github/stackai/rag/
│       ├── Rag.java
│       └── RagConfig.java
│
└── stack-ai-examples/
    └── src/main/java/io/github/stackai/examples/
        └── RagExample.java
```

---

## Contributing

Contributions are welcome. Please open an issue before starting significant work so we can discuss direction and avoid duplication.

**Guidelines:**

- All code must be pure Java 21 — no Kotlin, no Scala.
- No new mandatory dependency on any DI framework or application server.
- New cross-module data objects belong in `stack-ai-core` only if two or more modules need them.
- Each module must compile and pass tests independently with `mvn test -pl <module>`.
- Follow the existing Builder + immutable object pattern for all config and model classes.

---

## License

This project is licensed under the [Apache License 2.0](LICENSE).
