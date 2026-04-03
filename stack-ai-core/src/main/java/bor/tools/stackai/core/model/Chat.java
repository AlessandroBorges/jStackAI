package bor.tools.stackai.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Chat {

    /** ID retornado pela API (null para nova conversa; preenchido pela implementação). */
    private String chatId;

    /** Histórico de mensagens da conversa. */
    @Builder.Default
    private List<ChatMessage> messages = new ArrayList<>();
}
