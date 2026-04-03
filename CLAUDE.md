# Stack AI — Claude Code Context

## Project Overview

**Stack AI** is a modular AI/LLM toolkit for the JVM. Philosophy: Apache Commons-style — pick only the modules you need, no framework required.

- **GroupId:** `bor.tools.stackai`
- **Version:** `1.0.0-SNAPSHOT`
- **Java:** 21 (minimum; required by Lucene 10.x)
- **Build tool:** Maven 3.9.x

---

## Module Structure

```
stack-ai/
├── pom.xml                     ← parent POM (packaging: pom)
├── stack-ai-core/              ← interfaces + shared models (no business logic)
├── stack-ai-llm-client/        ← OpenAI-compatible HTTP client
├── stack-ai-document-digest/   ← document parsing pipeline (PDF/DOCX/HTML → Chunks)
├── stack-ai-rag/               ← Lucene-based RAG (textual/vector/hybrid)
└── stack-ai-examples/          ← runnable demos; fat JAR only, not published
```

### Dependency Graph

```
stack-ai-core
    ▲           ▲           ▲
    │           │           │
llm-client  digest-doc     rag ──────► digest-doc
                            ▲
                            │
                         examples ◄─── llm-client
                                  ◄─── digest-doc
                                  ◄─── core
```

**Key rule:** `llm-client` and `document-digest` do NOT know each other. `ILlmClient` reaches `document-digest` via `DocumentDigestConfig` (dependency injection via constructor/builder, not a DI container).

---

## Package Structure

| Module | Package |
|---|---|
| `stack-ai-core` | `bor.tools.stackai.core.interfaces` / `bor.tools.stackai.core.model` |
| `stack-ai-llm-client` | `bor.tools.stackai.llm` |
| `stack-ai-document-digest` | `bor.tools.stackai.digest` / `bor.tools.stackai.digest.strategy` |
| `stack-ai-rag` | `bor.tools.stackai.rag` |
| `stack-ai-examples` | `bor.tools.stackai.examples` |

---

## Core Interfaces (stack-ai-core)

```java
// bor.tools.stackai.core.interfaces
ILlmClient       → ask(messages, LlmClientConfig)  / embed(text, LlmClientConfig)
IDocumentDigest  → process(URI, DocumentDigestConfig) : List<Chunk>
IRag             → index(URI, RagConfig) / search(query, n, RagConfig) / delete(docId, RagConfig)
```

---

## Core Model (stack-ai-core)

**Data objects** (cross-module, immutable `@Value @Builder`):
- `Chunk` — id, documentId, library, type, sequential, text, lang, vector, parentChunkId, metadata
- `ChunkType` — enum: EXCERPT, SUMMARY, TITLE, PARAGRAPH, SECTION, CONCLUSION, QUOTE, TABLE, LIST, METADATA
- `ChatMessage` — role, content; static factories: `user()`, `system()`, `assistant()`
- `LlmModel` — id, modality, profile, contextSize, embeddingDimensions
- `ModelModality` — TEXT, EMBEDDING, VISION, IMAGE
- `ModelProfile` — FAST, CODING, THINKING, IMAGE_TO_TEXT, TEXT_TO_IMAGE
- `ContextTokens` — constants: K8, K16, K32, K64, K128, K256

**Config objects** (live in `core` because used in interface signatures):
- `LlmClientConfig` — apiKey, baseUrl, models, defaultModel, embeddingModel, timeoutSeconds
- `DocumentDigestConfig` — strategy, chunkSize, overlap, defaultLang, library, llmClient, embeddingModel, generateMetadata
- `RagConfig` — indexPath, vectorDimensions, maxResults, searchType, llmClient, embeddingModel, library, documentDigest, digestConfig

**Enums** (in `core` because referenced by config/interfaces):
- `DigestStrategy` — HIERARCHICAL, NORMATIVE, DOCUMENT
- `SearchType` — TEXTUAL, VECTOR, HYBRID

---

## Module-Internal Types

`DigestResult` lives only in `stack-ai-document-digest` (not in core). It contains `rawMarkdown` for pipeline inspection. Access it via `DocumentDigest.processToResult()`, not via `IDocumentDigest`.

---

## Document Processing Pipeline

```
URI (PDF, DOCX, HTML, TXT)
 → Apache Tika  (XHTML + language detection)
 → JSoup        (XHTML cleanup)
 → FlexMark     (XHTML → Markdown)
 → Strategy     (Hierarchical | Normative | Document fallback)
 → List<Chunk>
```

Language detection runs on clean Markdown text (not raw XHTML). `Chunk.lang` drives Lucene Analyzer selection at index and query time.

---

## Coding Conventions

- **Pure Java 21** — no Kotlin, no Scala.
- **No DI framework** — direct instantiation with `new` + builder. No Spring, Quarkus, CDI.
- **Immutable objects** — use Lombok `@Value @Builder` for all config/model classes.
- **No hidden magic** — all cross-module dependencies passed explicitly via constructor or builder.
- **URI as document reference** — covers `file:///`, `http://`, `https://` without changing the contract.
- **`@Builder.Default`** for optional fields with defaults (e.g., `timeoutSeconds = 30`).
- **Stub pattern** — unimplemented methods throw `UnsupportedOperationException("Not implemented yet")`.
- New cross-module data objects belong in `stack-ai-core` only if two or more modules need them.
- Each module must compile and pass tests independently with `mvn test -pl <module>`.

---

## Build Commands

```bash
# Full build
mvn install

# Full build, skip tests
mvn install -DskipTests

# Compile a single module (and its dependencies)
mvn compile -pl stack-ai-core -am
mvn compile -pl stack-ai-llm-client -am

# Run tests for a specific module
mvn test -pl stack-ai-rag

# Build and run examples fat JAR
mvn package -pl stack-ai-examples -am
java -jar stack-ai-examples/target/stack-ai-examples-*-jar-with-dependencies.jar
```

---

## Technology Stack

| Library | Version | Role |
|---|---|---|
| Java | 21 | Language |
| Apache Lucene | 10.4.0 | Textual, vector (HNSW), hybrid search |
| Apache Tika | 2.9.2 | Document parsing + language detection |
| Apache Commons Lang | 3.17.0 | String utilities |
| Apache Commons IO | 2.17.0 | I/O utilities |
| OkHttp | 4.12.0 | HTTP client for LLM API |
| Jackson Databind | 2.18.2 | JSON serialization |
| JSoup | 1.18.3 | XHTML cleanup |
| FlexMark | 0.64.8 | XHTML → Markdown + AST parsing |
| Lombok | 1.18.36 | Builder/accessor generation |
| JUnit Jupiter | 5.11.3 | Unit testing |
| AssertJ | 3.26.3 | Fluent assertions |

---

## Key Design Decisions

| # | Decision |
|---|---|
| D-01 | Pure Java — no Kotlin, no Scala |
| D-02 | No DI framework in modules |
| D-03 | Interfaces only where multiple implementations are planned |
| D-04 | Config objects as injection points for cross-module dependencies |
| D-05 | `documentId` generated by `DocumentDigest` (not by caller) |
| D-07 | `DigestResult` stays in `document-digest`; not in `core` |
| D-08 | `URI` as universal document reference |
| D-09 | Monorepo |
| D-10 | Markdown as canonical internal format |
| D-11 | Pipeline: Tika → JSoup → FlexMark |
| D-12 | Language detection on clean Markdown text |
| D-13 | `Chunk.lang` drives Lucene Analyzer selection |
| D-17 | Apache Lucene 10.4.0 (requires Java 21) |
