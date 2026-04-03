Podemos ter algumas versões overloaded do método ask():

 ## Versão stream:
 
 ```java
  public boolean ask(ResponseStream stream, List<ChatMessage> messages, LlmClientConfig config)
 ```
 
 ## Interface para Streamming
 
 Sugestão para interface de streaming

```java
package bor.tools.simplellm;

/**
 * Interface for handling streaming responses from a chat service.
 * This interface defines methods that are called during the streaming 
 * process, allowing for real-time handling of tokens, completion events, 
 * and error notifications.
 * 
 * @author Alessandro Borges
 */
public interface ResponseStream {

	/**
	 * Enumeration of content types that can be streamed from LLM services.
	 */
	public enum ContentType {
		/**
		 * Regular response content (text, code, structured data)
		 */
		TEXT,

		/**
		 * Chain-of-thought reasoning from reasoning-capable models (o1, o3, etc.)
		 */
		REASONING,
		/** Image generated */
		IMAGE,
		
		/** Audio Generated **/
		AUDIO,

		/**
		 * Function/tool call invocations and parameters
		 */
		TOOL_CALL,

		/**
		 * Metadata information (usage stats, model info, etc.)
		 */
		METADATA
	}

    /**
     * Invoked when a new token is received from the stream.
     *
     * @param token The content of the token received, which may represent
     *              a part of the response or a continuation of the message.
     * @param type The type of content being streamed
     */
    void onToken(String token, ContentType type);

    /**
     * Invoked when a new token is received from the stream (backward compatibility).
     * This method provides backward compatibility for existing implementations.
     * By default, it delegates to onToken(token, ContentType.TEXT).
     *
     * @param token The content of the token received
     */
    default void onToken(String token) {
        onToken(token, ContentType.TEXT);
    }

    /**
     * Convenience method for handling only text content.
     * Implementations can override this to handle text content specifically.
     *
     * @param token The text content token
     */
    default void onTextToken(String token) {
        onToken(token, ContentType.TEXT);
    }

    /**
     * Convenience method for handling only reasoning content.
     * Implementations can override this to handle reasoning content specifically.
     *
     * @param reasoning The reasoning content token
     */
    default void onReasoningToken(String reasoning) {
        onToken(reasoning, ContentType.REASONING);
    }

    /**
     * Convenience method for handling tool calls.
     * Implementations can override this to handle tool calls specifically.
     *
     * @param toolCall The tool call content token
     */
    default void onToolCallToken(String toolCall) {
        onToken(toolCall, ContentType.TOOL_CALL);
    }

    /**
     * Convenience method for handling metadata.
     * Implementations can override this to handle metadata specifically.
     *
     * @param metadata The metadata content token
     */
    default void onMetadataToken(String metadata) {
        onToken(metadata, ContentType.METADATA);
    }
    
    /**
     * Invoked when the streaming process is complete.
     * This method signifies that no further tokens will be sent, and 
     * any final processing can be performed.
     */
    void onComplete();
    
    /**
     * Invoked when an error occurs during the streaming process.
     *
     * @param error The throwable that represents the error that occurred, 
     *              providing details about the issue encountered.
     */
    void onError(Throwable error);
}

```