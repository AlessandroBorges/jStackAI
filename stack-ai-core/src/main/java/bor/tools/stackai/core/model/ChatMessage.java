package bor.tools.stackai.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.SuperBuilder;

@Value
@SuperBuilder
@AllArgsConstructor
public class ChatMessage {
       
    
    /**
     * Role of the message sender. Can be "user", "system", or "assistant".
     * @TODO: Consider using an enum for role to enforce valid values.	
     * 
     */

    String role;
    
    /**
     * The message content. It can be text, reasoning, image, image description,
     * audio, URL or any other relevant information depending on the context of the conversation.
     **/
    String content;
    
    
    /**
     * The content  Type.
     * If null it defaults to "text".
     *    */
    String contentType; // e.g., "text", "image", "audio", "url", etc.
    
    /**
     *  Optional field for additional information, such as stats, reasoning steps, image descriptions, etc.<br>
     *  Possible it will be a instance of<br>
     *   <pre> 
     *   	Map &lt; String, Object &gt;
     *   </pre>
     *    
     */
    @Builder.Default
    Object metadata = null; //

    public ChatMessage(String role, String content) {
	this.role = role;
	this.content = content;
	this.contentType = "text"; // Default to text if not specified
	this.metadata = null; // Default to null if not specified
    }
    
    public ChatMessage(String role, String content, String contentType) {
	this.role = role;
	this.content = content;
	this.contentType = contentType; // Default to text if not specified
	this.metadata = null; // Default to null if not specified
    }
    
    public static ChatMessage user(String content) {
        return new ChatMessage("user", content);
    }

    public static ChatMessage system(String content) {
        return new ChatMessage("system", content);
    }

    public static ChatMessage assistant(String content) {
        return new ChatMessage("assistant", content);
    }
}
