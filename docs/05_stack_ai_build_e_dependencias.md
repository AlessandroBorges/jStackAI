# Stack AI — Build, Dependências e Pipeline de Parsing

*Versão 1.0 · 2026*

---

## 1. Estrutura do Repositório

O projeto adota o modelo **monorepo** com um único repositório GitHub contendo o parent POM e todos os módulos filhos. Essa decisão é coerente com o estágio atual do projeto: os módulos evoluem juntos, refatorações cruzam fronteiras entre módulos com frequência e o ciclo de release é único.

```
github.com/<usuario>/stack-ai/
├── pom.xml                        ← parent POM (packaging: pom)
├── stack-ai-core/
│   └── pom.xml
├── stack-ai-llm-client/
│   └── pom.xml
├── stack-ai-document-digest/
│   └── pom.xml
├── stack-ai-rag/
│   └── pom.xml
└── stack-ai-examples/
    └── pom.xml
```

**Vantagens do monorepo para este projeto:**

- Um único `git clone` para ter todo o código.
- Build completo com `mvn install` na raiz.
- Pull Requests que afetam múltiplos módulos são atômicos.
- Pipeline de CI/CD (GitHub Actions) com um único workflow.
- Refatorações em interfaces do `core` com impacto imediato nos módulos dependentes.

---

## 2. Stack de Tecnologia

| Tecnologia | Versão | Papel |
|---|---|---|
| Java | 21 | Linguagem única; requisito mínimo do Lucene 10.x |
| Apache Maven | 3.9.x | Build e gerenciamento de módulos |
| Lombok | 1.18.36 | Redução de boilerplate (Builder, getters, etc.) |
| Apache Lucene | 10.4.0 | Motor de busca textual, vetorial e híbrida no RAG |
| Apache Tika | 2.9.2 | Extração de texto e detecção de língua |
| Apache Commons Lang | 3.17.0 | Utilitários de string e validação |
| Apache Commons IO | 2.17.0 | Utilitários de I/O para leitura de documentos |
| OkHttp | 4.12.0 | Cliente HTTP para chamadas à API do LLM |
| Jackson Databind | 2.18.2 | Serialização/deserialização de JSON (payloads OpenAI) |
| JSoup | 1.18.3 | Limpeza e normalização do XHTML produzido pelo Tika |
| FlexMark | 0.64.8 | Conversão XHTML → Markdown e parse de AST Markdown |
| JUnit Jupiter | 5.11.3 | Testes unitários |
| AssertJ | 3.26.3 | Assertions fluentes nos testes |

---

## 3. Pipeline de Parsing de Documentos

### 3.1 Visão Geral

O `stack-ai-document-digest` adota **Markdown como formato canônico interno**. Todo documento de entrada — independentemente do formato original — é convertido para Markdown antes de ser entregue às estratégias de chunking. Isso simplifica radicalmente as estratégias, que passam a trabalhar sobre sintaxe Markdown previsível em vez de lidar com cada formato de origem.

```
URI (PDF, DOCX, HTML, TXT, ...)
        │
        ▼
  [Apache Tika]
  Extração de texto → XHTML estruturado
  Detecção de língua → "pt" / "es" / "en"
        │
        ▼
  [JSoup]
  Limpeza e normalização do XHTML
  Remoção de atributos desnecessários
  Correção de estrutura malformada
        │
        ▼
  [FlexMark — html2md-converter]
  Conversão XHTML → Markdown canônico
        │
        ▼
  Markdown  ←  formato interno único
        │
        ▼
  [Strategy: Hierarchical / Normative / Fallback]
        │
        ▼
  List<Chunk>
```

### 3.2 Por Que XHTML como Intermediário

O Tika produz XHTML como saída padrão para qualquer formato de entrada. Isso garante que a etapa seguinte (JSoup) sempre receba o mesmo tipo de documento, independentemente da origem. Um PDF, um DOCX e uma página HTML são todos normalizados para XHTML pelo Tika antes de qualquer outra transformação.

### 3.3 Por Que Markdown como Formato Canônico

As estratégias de chunking (`HierarchicalDigest`, `NormativeDigest`, `DocumentFallbackDigest`) precisam identificar estruturas como títulos, seções e parágrafos. O Markdown expõe essas estruturas de forma explícita e parseável — um `#` é inequivocamente um título H1, `##` é H2, e assim por diante. Trabalhar sobre Markdown é mais simples e mais robusto do que trabalhar sobre XHTML ou sobre texto plano.

### 3.4 Papel de Cada Biblioteca

| Biblioteca | Entrada | Saída | Responsabilidade |
|---|---|---|---|
| Apache Tika | URI (qualquer formato) | XHTML + metadados | Extração e estruturação |
| JSoup | XHTML bruto do Tika | XHTML limpo | Normalização e limpeza |
| FlexMark (html2md) | XHTML limpo | Markdown | Conversão de formato |
| FlexMark (parser) | Markdown | AST | Navegação estrutural nas strategies |

### 3.5 rawMarkdown no DigestResult

O objeto `DigestResult` (interno ao módulo `stack-ai-document-digest`) expõe o campo `rawMarkdown` para que o consumidor possa inspecionar o Markdown intermediário gerado antes do chunking. Esse campo não existe no `Chunk` — pertence exclusivamente ao resultado do digest.

```java
public final class DigestResult {
    private final String         documentId;
    private final URI            source;
    private final String         rawMarkdown;   // Markdown antes do chunking
    private final List<Chunk>    chunks;
    private final DigestStrategy strategyUsed;
    private final String         detectedLang;  // "pt_br", "es", "en_us"
}
```

---

## 4. Detecção de Língua

### 4.1 Mecanismo

O Apache Tika, com o módulo `tika-langdetect-optimaize`, implementa detecção automática de língua baseada na biblioteca Optimaize. O resultado inclui o código ISO da língua detectada e um score de confiança.

**Momento da detecção:** a detecção ocorre **após** a conversão para Markdown, sobre o texto limpo. Texto sem ruído de tags HTML produz resultados mais confiáveis.

### 4.2 Línguas Suportadas no Lançamento Inicial

| Código detectado pelo Tika | Normalizado para o campo `lang` | Analyzer no Lucene |
|---|---|---|
| `pt` | `pt_br` | `BrazilianAnalyzer` |
| `es` | `es` | `SpanishAnalyzer` |
| `en` | `en_us` | `EnglishAnalyzer` |
| (qualquer outro) | `lang` original | `StandardAnalyzer` (fallback) |

### 4.3 Impacto no RAG

O campo `lang` do `Chunk` é usado pelo `stack-ai-rag` para selecionar o Analyzer correto do Lucene tanto na **indexação** quanto na **busca**. Isso garante que stemming, stopwords e normalização sejam aplicados corretamente para cada língua.

O `Rag` instancia o Analyzer dinamicamente por documento — não usa um Analyzer global fixo no índice. O Lucene suporta essa abordagem nativamente.

```
Chunk.getLang()  →  "pt_br"  →  BrazilianAnalyzer
                    "es"     →  SpanishAnalyzer
                    "en_us"  →  EnglishAnalyzer
                    (outro)  →  StandardAnalyzer
```

---

## 5. Organização das Dependências Maven

### 5.1 Princípio Geral

O parent POM centraliza **todas** as versões no `<dependencyManagement>`. Os módulos filhos declaram apenas as dependências que efetivamente usam, sem repetir versões. Nenhum módulo herda dependências automaticamente do parent — tudo é explícito.

### 5.2 Dependências por Módulo

#### stack-ai-core
Sem dependências de outros módulos. Usa apenas Lombok (escopo `provided`) e JUnit + AssertJ (escopo `test`).

#### stack-ai-llm-client
| Dependência | Escopo | Justificativa |
|---|---|---|
| `stack-ai-core` | compile | Implementa `ILlmClient`; usa `ChatMessage`, `LlmModel` |
| `okhttp` | compile | Cliente HTTP para a API OpenAI-compatible |
| `jackson-databind` | compile | Serializa/deserializa JSON dos payloads |
| `commons-lang3` | compile | Utilitários de string e validação |
| `lombok` | provided | Builders e getters |

#### stack-ai-document-digest
| Dependência | Escopo | Justificativa |
|---|---|---|
| `stack-ai-core` | compile | Implementa `IDocumentDigest`; usa `Chunk`, `ChunkType` |
| `tika-core` | compile | API principal do Tika |
| `tika-parser-pdf-module` | compile | Suporte a PDF |
| `tika-parser-microsoft-module` | compile | Suporte a DOCX, XLSX, PPTX |
| `tika-parser-html-module` | compile | Suporte a HTML |
| `tika-parser-text-module` | compile | Suporte a TXT e Markdown raw |
| `tika-langdetect-optimaize` | compile | Detecção de língua |
| `jsoup` | compile | Limpeza e normalização do XHTML |
| `flexmark` | compile | Core do parser/renderer de Markdown |
| `flexmark-html2md-converter` | compile | Conversão XHTML → Markdown |
| `flexmark-ext-tables` | compile | Suporte a tabelas no parse do AST |
| `commons-lang3` | compile | Utilitários |
| `commons-io` | compile | Leitura de URIs e streams |
| `lombok` | provided | Builders e getters |

> **Nota:** `stack-ai-document-digest` **não** depende de `stack-ai-llm-client`. A instância de `ILlmClient` chega por injeção via `DocumentDigestConfig`.

#### stack-ai-rag
| Dependência | Escopo | Justificativa |
|---|---|---|
| `stack-ai-core` | compile | Implementa `IRag`; usa `Chunk`, `ILlmClient`, `IDocumentDigest` |
| `stack-ai-document-digest` | compile | Usa `DocumentDigest`, `DocumentDigestConfig`, `DigestResult` internamente |
| `lucene-core` | compile | `IndexWriter`, `IndexSearcher`, campos e queries |
| `lucene-analysis-common` | compile | `BrazilianAnalyzer`, `SpanishAnalyzer`, `EnglishAnalyzer` |
| `lucene-queryparser` | compile | `QueryParser` para busca textual |
| `commons-lang3` | compile | Utilitários |
| `lombok` | provided | Builders e getters |

> **Nota:** `stack-ai-rag` **não** depende de `stack-ai-llm-client`. A instância de `ILlmClient` chega por injeção via `RagConfig`.

#### stack-ai-examples
Depende de todos os módulos acima. Não é publicado como artefato de biblioteca — serve apenas como demonstração e pode gerar um fat JAR via `maven-assembly-plugin`.

### 5.3 Grafo de Dependências

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

---

## 6. Lucene 10.x — Considerações Relevantes

O Apache Lucene 10.4.0 (lançado em 25/02/2026) introduz algumas mudanças em relação à série 9.x que afetam o design do `stack-ai-rag`:

- **Java 21 obrigatório** — alinha com a versão mínima do projeto.
- **`KnnFloatVectorField` estável** — a API de busca vetorial (HNSW) permanece compatível com o design definido.
- **Analyzers de língua** — `BrazilianAnalyzer`, `SpanishAnalyzer` e `EnglishAnalyzer` permanecem no módulo `lucene-analysis-common`.
- **Seleção dinâmica de Analyzer** — o `Rag` seleciona o Analyzer por documento com base no campo `lang` do `Chunk`, em vez de usar um Analyzer global fixo no índice.

---

## 7. Decisões de Design Registradas Nesta Fase

| # | Decisão | Justificativa |
|---|---|---|
| D-09 | Monorepo único no GitHub | Módulos evoluem juntos; build e CI unificados |
| D-10 | Markdown como formato canônico interno | Simplifica as estratégias de chunking; estrutura explícita e parseável |
| D-11 | Pipeline: Tika → JSoup → FlexMark → Markdown | Cada biblioteca tem papel único; XHTML como único intermediário |
| D-12 | Detecção de língua sobre texto limpo (Markdown) | Maior confiabilidade; sem ruído de tags |
| D-13 | `lang` do Chunk determina Analyzer no Lucene | Stemming e stopwords corretos por língua na indexação e busca |
| D-14 | `tika-langdetect-optimaize` como módulo separado | Paga-se o custo apenas quando a detecção é necessária |
| D-15 | Tika com parsers seletivos (não `tika-parsers-standard-package`) | Artefato enxuto; coerente com a filosofia de dependências mínimas |
| D-16 | `rawMarkdown` exposto no `DigestResult` | Permite inspeção do Markdown intermediário sem poluir o `Chunk` |
| D-17 | Lucene 10.4.0 | Java 21 obrigatório alinha com a versão mínima do projeto |
