package org.codedefenders.llm;

import java.util.List;

/**
 * This class represents the body of a chat completion request for the GPT API. Specifications about the request are
 * available at
 * <a href="https://platform.openai.com/docs/api-reference/chat/create">
 *     https://platform.openai.com/docs/api-reference/chat/create
 * </a>
 */
class GPTRequest {
    private String model;
    private List<GPTMessage> messages;
    private Double temperature;


    GPTRequest(String model, List<GPTMessage> messages, Double temperature) {
        this.model = model;
        this.messages = messages;

        if(temperature < 0.0 || temperature > 2.0) {
            this.temperature = 0.0;
        } else {
            this.temperature = temperature;
        }
    }
}
