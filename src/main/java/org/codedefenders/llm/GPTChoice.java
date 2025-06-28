package org.codedefenders.llm;

/**
 * This class represents a chat completion choice object for the GPT API. Specifications about chat completion choice
 * properties are available at
 * <a href="https://platform.openai.com/docs/api-reference/chat/object">
 *     https://platform.openai.com/docs/api-reference/chat/object
 * </a>
 */
class GPTChoice {
    private Integer index;
    private GPTMessage message;
    private String finishReason;

    String getMessageContent() {
        return message.getContent();
    }
}
