package bor.tools.stackai.core.interfaces;


import bor.tools.stackai.core.model.Chat;
import bor.tools.stackai.core.model.LlmClientConfig;

/**
 * Core interface for Large Language Model (LLM) client implementations within the stack-ai framework.
 * <p>
 * This interface defines the essential contract for interacting with various LLM providers,
 * offering both synchronous and asynchronous (streaming) communication patterns for text generation
 * and embedding operations. Implementations of this interface serve as abstraction layers that
 * enable seamless integration with different LLM services such as OpenAI, Anthropic, local models,
 * or custom implementations.
 * </p>
 * 
 * <h2>Core Functionality</h2>
 * <ul>
 *   <li><strong>Text Generation:</strong> Single-turn questions and multi-turn conversations</li>
 *   <li><strong>Streaming Support:</strong> Real-time response processing for improved user experience</li>
 *   <li><strong>Embeddings:</strong> Vector representations of text for semantic operations</li>
 *   <li><strong>Configuration Management:</strong> Flexible configuration per operation</li>
 * </ul>
 * 
 * <h2>Usage Patterns</h2>
 * 
 * <h3>Simple Question-Answer:</h3>
 * <pre>{@code
 * ILlmClient client = // ... obtain implementation
 * LlmClientConfig config = LlmClientConfig.builder()
 *     .apiKey("your-api-key")
 *     .baseUrl("https://api.example.com")
 *     .defaultModel(model)
 *     .build();
 * 
 * String response = client.ask("What is the capital of France?", config);
 * System.out.println(response); // "Paris is the capital of France."
 * }</pre>
 * 
 * <h3>Conversational Chat:</h3>
 * <pre>{@code
 * Chat chat = Chat.builder()
 *     .chatId("conversation-123")
 *     .messages(new ArrayList<>())
 *     .build();
 * 
 * // Add user message to conversation history
 * chat.getMessages().add(new ChatMessage("user", "Hello!"));
 * 
 * String response = client.ask(chat, config);
 * }</pre>
 * 
 * <h3>Streaming Responses:</h3>
 * <pre>{@code
 * ResponseStream stream = new ResponseStream() {
 *     @Override
 *     public void onToken(String token, ContentType type) {
 *         System.out.print(token); // Real-time display
 *     }
 *     
 *     @Override
 *     public void onComplete() {
 *         System.out.println("\nResponse complete!");
 *     }
 *     
 *     @Override
 *     public void onError(Throwable error) {
 *         System.err.println("Error: " + error.getMessage());
 *     }
 * };
 * 
 * client.ask(stream, "Explain quantum computing", config);
 * }</pre>
 * 
 * <h3>Text Embeddings:</h3>
 * <pre>{@code
 * float[] embedding = client.embed("Machine learning algorithms", config);
 * // Use embedding for similarity search, clustering, etc.
 * }</pre>
 * 
 * <h2>Implementation Guidelines</h2>
 * <p>
 * Implementing classes should:
 * </p>
 * <ul>
 *   <li>Handle authentication and API communication securely</li>
 *   <li>Provide appropriate error handling and timeout management</li>
 *   <li>Support both blocking and non-blocking operations</li>
 *   <li>Maintain thread safety for concurrent usage</li>
 *   <li>Implement proper resource cleanup and connection management</li>
 * </ul>
 * 
 * <h2>Error Handling</h2>
 * <p>
 * Implementations should throw appropriate runtime exceptions for:
 * </p>
 * <ul>
 *   <li>Authentication failures</li>
 *   <li>Network connectivity issues</li>
 *   <li>Rate limiting or quota exceeded</li>
 *   <li>Invalid configuration or parameters</li>
 *   <li>Service unavailability</li>
 * </ul>
 * 
 * @author stack-ai team
 * @version 1.0
 * @since 1.0
 * 
 * @see LlmClientConfig
 * @see Chat
 * @see ResponseStream
 */
public interface ILlmClient {

    /**
     * Performs a synchronous question-answer interaction with the configured LLM.
     * <p>
     * This method sends a single question to the language model and waits for the complete
     * response before returning. It's ideal for simple query-response patterns where real-time
     * streaming is not required, such as form validation, quick calculations, or factual queries.
     * </p>
     * 
     * <h3>Usage Example:</h3>
     * <pre>{@code
     * LlmClientConfig config = LlmClientConfig.builder()
     *     .apiKey("sk-...")
     *     .baseUrl("https://api.openai.com")
     *     .defaultModel(gpt4Model)
     *     .timeoutSeconds(30)
     *     .build();
     * 
     * String answer = client.ask("What is the square root of 144?", config);
     * // Returns: "The square root of 144 is 12."
     * }</pre>
     * 
     * <h3>Behavior:</h3>
     * <ul>
     *   <li>Blocks the calling thread until the response is complete</li>
     *   <li>Uses the default model specified in configuration</li>
     *   <li>Returns the full generated text as a single string</li>
     *   <li>Respects timeout settings from the configuration</li>
     * </ul>
     *
     * @param question the user's question or prompt to send to the LLM.
     *                Must not be null or empty. The content should be appropriate
     *                for the target model and use case.
     * @param config   the configuration containing API credentials, model settings,
     *                timeout values, and other operational parameters. Must not be null
     *                and should contain valid authentication information.
     * 
     * @return the complete response text from the LLM. Never null, but may be empty
     *         if the model produces no output. The response represents the model's
     *         complete answer to the provided question.
     * 
     * @throws IllegalArgumentException if question is null, empty, or config is null
     * @throws RuntimeException if authentication fails, network issues occur,
     *                         rate limits are exceeded, the service is unavailable,
     *                         or the operation times out
     * 
     * @see #ask(ResponseStream, String, LlmClientConfig) for streaming version
     * @see LlmClientConfig for configuration options
     */
    String ask(String question, LlmClientConfig config);

    /**
     * Performs an asynchronous (streaming) question-answer interaction with the configured LLM.
     * <p>
     * This method sends a question to the language model and processes the response in real-time
     * as tokens are generated. Unlike the synchronous version, this method does not block the
     * calling thread but instead uses the provided {@link ResponseStream} callbacks to handle
     * response tokens, completion events, and errors asynchronously.
     * </p>
     * 
     * <h3>Usage Example:</h3>
     * <pre>{@code
     * ResponseStream stream = new ResponseStream() {
     *     private StringBuilder response = new StringBuilder();
     *     
     *     @Override
     *     public void onToken(String token, ContentType type) {
     *         response.append(token);
     *         System.out.print(token); // Live output
     *     }
     *     
     *     @Override
     *     public void onComplete() {
     *         System.out.println("\nFinal response: " + response.toString());
     *     }
     *     
     *     @Override
     *     public void onError(Throwable error) {
     *         System.err.println("Streaming error: " + error.getMessage());
     *     }
     * };
     * 
     * // Non-blocking call - returns immediately
     * client.ask(stream, "Explain the theory of relativity", config);
     * 
     * // Continue with other work while response streams in background
     * }</pre>
     * 
     * <h3>Behavior:</h3>
     * <ul>
     *   <li>Returns immediately without blocking the calling thread</li>
     *   <li>Invokes {@code onToken()} callbacks as tokens are generated</li>
     *   <li>Calls {@code onComplete()} when the response is finished</li>
     *   <li>Calls {@code onError()} if any errors occur during processing</li>
     *   <li>Suitable for real-time user interfaces and progress indication</li>
     * </ul>
     * 
     * <h3>Thread Safety:</h3>
     * <p>
     * Callback methods may be invoked from different threads. Implementations of
     * {@link ResponseStream} should ensure thread safety for any shared state.
     * </p>
     *
     * @param stream   the response stream handler for processing tokens and events.
     *                Must not be null and should implement all callback methods
     *                appropriately for the specific use case.
     * @param question the user's question or prompt to send to the LLM.
     *                Must not be null or empty. The content should be appropriate
     *                for the target model and use case.
     * @param config   the configuration containing API credentials, model settings,
     *                timeout values, and other operational parameters. Must not be null
     *                and should contain valid authentication information.
     * 
     * @throws IllegalArgumentException if stream is null, question is null/empty, or config is null
     * @throws RuntimeException if authentication fails immediately or configuration is invalid.
     *                         Note that network errors and other issues during streaming will be
     *                         reported via {@code stream.onError()} rather than thrown exceptions.
     * 
     * @see #ask(String, LlmClientConfig) for synchronous version
     * @see ResponseStream for callback interface details
     * @see ResponseStream.ContentType for token type handling
     */
    void ask(ResponseStream stream, String question, LlmClientConfig config);
    
    
    /**
     * Performs a synchronous chat completion using an existing conversation context.
     * <p>
     * This method continues a multi-turn conversation by sending the entire chat history
     * to the language model and waiting for the complete response. The chat object maintains
     * the conversation state, including all previous messages, which provides context for
     * generating coherent and contextually appropriate responses.
     * </p>
     * 
     * <h3>Usage Example:</h3>
     * <pre>{@code
     * // Create a new chat session
     * Chat chat = Chat.builder()
     *     .chatId("user-session-456")
     *     .messages(new ArrayList<>())
     *     .build();
     * 
     * // Add initial user message
     * ChatMessage userMsg = ChatMessage.builder()
     *     .role("user")
     *     .content("Tell me about machine learning")
     *     .build();
     * chat.getMessages().add(userMsg);
     * 
     * // Get response from LLM
     * String response = client.ask(chat, config);
     * 
     * // Add assistant's response to chat history for context
     * ChatMessage assistantMsg = ChatMessage.builder()
     *     .role("assistant")
     *     .content(response)
     *     .build();
     * chat.getMessages().add(assistantMsg);
     * }</pre>
     * 
     * <h3>Behavior:</h3>
     * <ul>
     *   <li>Processes the entire conversation history for context</li>
     *   <li>Maintains conversation state and continuity</li>
     *   <li>Blocks until the complete response is generated</li>
     *   <li>Does not automatically update the chat object with the response</li>
     * </ul>
     *
     * @param chat the conversation context containing the chat ID and message history.
     *            Must not be null. The messages list can be empty for a new conversation,
     *            but typically contains at least one user message. Each message should
     *            have a valid role (user/assistant/system) and content.
     * @param config the configuration containing API credentials, model settings,
     *              timeout values, and other operational parameters. Must not be null
     *              and should contain valid authentication information.
     * 
     * @return the assistant's response text to continue the conversation. Never null,
     *         but may be empty if the model produces no output. This response should
     *         be added to the chat's message history to maintain conversation context.
     * 
     * @throws IllegalArgumentException if chat is null, config is null, or chat contains
     *                                 invalid message structures
     * @throws RuntimeException if authentication fails, network issues occur,
     *                         rate limits are exceeded, the service is unavailable,
     *                         or the operation times out
     * 
     * @see #ask(ResponseStream, Chat, LlmClientConfig) for streaming version
     * @see Chat for conversation structure
     * @see LlmClientConfig for configuration options
     */
    String ask(Chat chat, LlmClientConfig config);

    /**
     * Performs an asynchronous (streaming) chat completion using an existing conversation context.
     * <p>
     * This method continues a multi-turn conversation by sending the entire chat history
     * to the language model and processes the response in real-time as tokens are generated.
     * Unlike the synchronous chat version, this method does not block but uses callbacks
     * to handle the streaming response, making it ideal for interactive chat applications
     * where users need to see responses being typed in real-time.
     * </p>
     * 
     * <h3>Usage Example:</h3>
     * <pre>{@code
     * Chat chat = Chat.builder()
     *     .chatId("interactive-session-789")
     *     .messages(previousMessages)
     *     .build();
     * 
     * ResponseStream stream = new ResponseStream() {
     *     private StringBuilder assistantResponse = new StringBuilder();
     *     
     *     @Override
     *     public void onToken(String token, ContentType type) {
     *         assistantResponse.append(token);
     *         
     *         // Handle different content types
     *         switch (type) {
     *             case TEXT:
     *                 updateChatUI(token); // Update UI in real-time
     *                 break;
     *             case REASONING:
     *                 showReasoningStep(token); // Display thinking process
     *                 break;
     *             case TOOL_CALL:
     *                 handleToolCall(token); // Process function calls
     *                 break;
     *             default:
     *                 break;
     *         }
     *     }
     *     
     *     @Override
     *     public void onComplete() {
     *         // Add completed response to chat history
     *         ChatMessage response = ChatMessage.builder()
     *             .role("assistant")
     *             .content(assistantResponse.toString())
     *             .build();
     *         chat.getMessages().add(response);
     *         
     *         saveChatToDatabase(chat);
     *     }
     *     
     *     @Override
     *     public void onError(Throwable error) {
     *         showErrorInChat("Failed to get response: " + error.getMessage());
     *     }
     * };
     * 
     * // Start streaming response - returns immediately
     * client.ask(stream, chat, config);
     * }</pre>
     * 
     * <h3>Behavior:</h3>
     * <ul>
     *   <li>Processes the entire conversation history for context</li>
     *   <li>Returns immediately without blocking the calling thread</li>
     *   <li>Streams response tokens as they are generated</li>
     *   <li>Maintains conversation state and continuity</li>
     *   <li>Supports different content types (text, reasoning, tool calls, etc.)</li>
     *   <li>Does not automatically update the chat object with the response</li>
     * </ul>
     * 
     * <h3>Thread Safety:</h3>
     * <p>
     * Callback methods may be invoked from different threads. Implementations should
     * ensure thread safety when updating UI components or shared chat state.
     * </p>
     *
     * @param stream the response stream handler for processing tokens and events.
     *              Must not be null and should implement all callback methods
     *              appropriately for the conversational use case.
     * @param chat   the conversation context containing the chat ID and message history.
     *              Must not be null. The messages list can be empty for a new conversation,
     *              but typically contains the conversation history including previous
     *              user and assistant messages.
     * @param config the configuration containing API credentials, model settings,
     *              timeout values, and other operational parameters. Must not be null
     *              and should contain valid authentication information.
     * 
     * @throws IllegalArgumentException if stream is null, chat is null, config is null,
     *                                 or chat contains invalid message structures
     * @throws RuntimeException if authentication fails immediately or configuration is invalid.
     *                         Note that network errors and other issues during streaming will be
     *                         reported via {@code stream.onError()} rather than thrown exceptions.
     * 
     * @see #ask(Chat, LlmClientConfig) for synchronous version
     * @see ResponseStream for callback interface details
     * @see ResponseStream.ContentType for different content types
     * @see Chat for conversation structure
     */
    void ask(ResponseStream stream, Chat chat, LlmClientConfig config);

    /**
     * Generates vector embeddings for the provided text using the configured embedding model.
     * <p>
     * This method converts textual content into numerical vector representations that capture
     * semantic meaning and relationships. The resulting embeddings can be used for various
     * downstream tasks such as similarity search, clustering, classification, and retrieval-augmented
     * generation (RAG) systems.
     * </p>
     * 
     * <h3>Usage Examples:</h3>
     * 
     * <p><strong>Basic Text Embedding:</strong></p>
     * <pre>{@code
     * LlmClientConfig config = LlmClientConfig.builder()
     *     .apiKey("sk-...")
     *     .baseUrl("https://api.openai.com")
     *     .embeddingModel(textEmbedding3Large)
     *     .build();
     * 
     * String text = "Machine learning is a subset of artificial intelligence";
     * float[] embedding = client.embed(text, config);
     * 
     * System.out.println("Embedding dimensions: " + embedding.length);
     * // Output: Embedding dimensions: 3072 (for text-embedding-3-large)
     * }</pre>
     * 
     * <p><strong>Document Similarity Comparison:</strong></p>
     * <pre>{@code
     * String doc1 = "Natural language processing enables computers to understand text";
     * String doc2 = "NLP helps machines comprehend human language";
     * String doc3 = "Cooking involves preparing food with various ingredients";
     * 
     * float[] embedding1 = client.embed(doc1, config);
     * float[] embedding2 = client.embed(doc2, config);
     * float[] embedding3 = client.embed(doc3, config);
     * 
     * // Calculate cosine similarity
     * double similarity1_2 = cosineSimilarity(embedding1, embedding2); // High similarity
     * double similarity1_3 = cosineSimilarity(embedding1, embedding3); // Low similarity
     * }</pre>
     * 
     * <p><strong>Vector Database Integration:</strong></p>
     * <pre>{@code
     * // Prepare documents for vector storage
     * List<String> documents = Arrays.asList(
     *     "Introduction to machine learning concepts",
     *     "Deep learning neural networks explained",
     *     "Natural language processing techniques"
     * );
     * 
     * for (String doc : documents) {
     *     float[] embedding = client.embed(doc, config);
     *     vectorDatabase.store(doc, embedding);
     * }
     * 
     * // Query similar documents
     * String query = "artificial neural networks";
     * float[] queryEmbedding = client.embed(query, config);
     * List<String> similarDocs = vectorDatabase.findSimilar(queryEmbedding, topK=5);
     * }</pre>
     * 
     * <h3>Behavior:</h3>
     * <ul>
     *   <li>Blocks until the embedding computation is complete</li>
     *   <li>Uses the embedding model specified in configuration</li>
     *   <li>Returns a normalized vector representation of the input text</li>
     *   <li>Dimension size depends on the chosen embedding model</li>
     *   <li>Identical input texts produce identical embedding vectors</li>
     * </ul>
     * 
     * <h3>Embedding Model Considerations:</h3>
     * <ul>
     *   <li><strong>Dimension Size:</strong> Different models produce embeddings of different sizes
     *       (e.g., 1536 for text-embedding-ada-002, 3072 for text-embedding-3-large)</li>
     *   <li><strong>Performance:</strong> Larger models generally provide better semantic understanding
     *       but require more computational resources and storage</li>
     *   <li><strong>Domain Specificity:</strong> Some models are optimized for specific domains
     *       (code, multilingual, scientific text)</li>
     *   <li><strong>Context Length:</strong> Models have maximum input token limits that must be respected</li>
     * </ul>
     *
     * @param text   the input text to convert to embeddings. Must not be null or empty.
     *              The text should be within the model's maximum token limit. For optimal
     *              results, text should be meaningful and well-formed.
     * @param config the configuration containing API credentials, embedding model specification,
     *              timeout values, and other operational parameters. Must not be null and
     *              should specify an appropriate embedding model. If no embedding model is
     *              explicitly set, the default embedding model from the configuration will be used.
     * 
     * @return a float array representing the vector embedding of the input text. The array
     *         is never null and has a fixed size determined by the embedding model used.
     *         Values are typically normalized to unit length for cosine similarity calculations.
     *         The embedding captures semantic meaning and can be used for similarity comparisons.
     * 
     * @throws IllegalArgumentException if text is null, empty, or exceeds the model's token limit,
     *                                 or if config is null or lacks a valid embedding model
     * @throws RuntimeException if authentication fails, network issues occur, rate limits are exceeded,
     *                         the embedding service is unavailable, the specified model is not found,
     *                         or the operation times out
     * 
     * @see LlmClientConfig#embeddingModel for embedding model configuration
     * @see LlmClientConfig#defaultModel as fallback if embedding model is not specified
     */
    float[] embed(String text, LlmClientConfig config);
}
