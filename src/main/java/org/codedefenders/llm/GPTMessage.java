package org.codedefenders.llm;

/**
 * This class represents a message object for the GPT API. Specifications about message properties are available at
 * <a href="https://platform.openai.com/docs/api-reference/chat/create">
 *     https://platform.openai.com/docs/api-reference/chat/create
 * </a>
 */
class GPTMessage {
    private GPTRole role;
    private String content;


    GPTMessage() { }

    GPTMessage(GPTRole role, String content) {
        this.role = role;
        this.content = content;
    }

    GPTRole getRole() {
        return role;
    }

    String getContent() {
        return content;
    }
}
