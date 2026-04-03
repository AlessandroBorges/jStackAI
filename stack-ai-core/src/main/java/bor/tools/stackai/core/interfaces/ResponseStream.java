package bor.tools.stackai.core.interfaces;

/**
 * Interface for handling streaming responses from a LLM service.
 * Defines callbacks invoked during the streaming process, allowing
 * real-time handling of tokens, completion events, and error notifications.
 */
public interface ResponseStream {

    /**
     * Content types that can be streamed from LLM services.
     */
    enum ContentType {
        /** Regular response content (text, code, structured data). */
        TEXT,
        /** Chain-of-thought reasoning from reasoning-capable models (o1, o3, etc.). */
        REASONING,
        /** Generated image. */
        IMAGE,
        /** Generated audio. */
        AUDIO,
        /** Function/tool call invocations and parameters. */
        TOOL_CALL,
        /** Metadata information (usage stats, model info, etc.). */
        METADATA
    }

    // -------------------------------------------------------------------------
    // Abstract methods — must be implemented
    // -------------------------------------------------------------------------

    /**
     * Invoked when a new token is received from the stream.
     *
     * @param token the content of the token received
     * @param type  the type of content being streamed
     */
    void onToken(String token, ContentType type);

    /**
     * Invoked when the streaming process is complete.
     */
    void onComplete();

    /**
     * Invoked when an error occurs during the streaming process.
     *
     * @param error the throwable representing the error
     */
    void onError(Throwable error);

    // -------------------------------------------------------------------------
    // Default convenience methods — delegate to onToken(String, ContentType)
    // -------------------------------------------------------------------------

    /** Backward-compatible overload; delegates to {@code onToken(token, ContentType.TEXT)}. */
    default void onToken(String token) {
        onToken(token, ContentType.TEXT);
    }

    /** Convenience for text content; delegates to {@code onToken(token, ContentType.TEXT)}. */
    default void onTextToken(String token) {
        onToken(token, ContentType.TEXT);
    }

    /** Convenience for reasoning content; delegates to {@code onToken(reasoning, ContentType.REASONING)}. */
    default void onReasoningToken(String reasoning) {
        onToken(reasoning, ContentType.REASONING);
    }

    /** Convenience for tool-call content; delegates to {@code onToken(toolCall, ContentType.TOOL_CALL)}. */
    default void onToolCallToken(String toolCall) {
        onToken(toolCall, ContentType.TOOL_CALL);
    }

    /** Convenience for metadata content; delegates to {@code onToken(metadata, ContentType.METADATA)}. */
    default void onMetadataToken(String metadata) {
        onToken(metadata, ContentType.METADATA);
    }
}
