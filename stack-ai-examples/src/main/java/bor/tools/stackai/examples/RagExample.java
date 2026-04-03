package bor.tools.stackai.examples;

import bor.tools.stackai.core.model.*;
import bor.tools.stackai.digest.DocumentDigest;
import bor.tools.stackai.llm.LlmClient;
import bor.tools.stackai.rag.Rag;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

public class RagExample {

    public static void main(String[] args) throws Exception {

        // Step 1 — Configure the LLM client
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

        // Step 2 — Configure DocumentDigest
        DocumentDigestConfig digestConfig = DocumentDigestConfig.builder()
                .llmClient(llmClient)
                .embeddingModel(ada)
                .generateMetadata(true)
                .library("my-library-uuid")
                .build();

        // Step 3 — Configure RAG
        RagConfig ragConfig = RagConfig.builder()
                .indexPath("/var/data/my-lucene-index")
                .vectorDimensions(1536)
                .searchType(SearchType.HYBRID)
                .llmClient(llmClient)
                .embeddingModel(ada)
                .library("my-library-uuid")
                .documentDigest(new DocumentDigest())
                .digestConfig(digestConfig)
                .maxResults(5)
                .build();

        Rag rag = new Rag();

        // Step 4 — Index a document
        String docId = rag.index(new URI("file:///docs/contract.pdf"), ragConfig);
        System.out.println("Indexed as: " + docId);

        // Step 5 — Search and generate an answer
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

        // Step 6 — Remove a document
        rag.delete(docId, ragConfig);
    }
}
