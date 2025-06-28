package org.codedefenders.llm;

/**
 * This class represents usage statistics of a chat completion request for the GPT API. Specifications about usage
 * statistics properties are available at
 * <a href="https://platform.openai.com/docs/api-reference/chat/object">
 *     https://platform.openai.com/docs/api-reference/chat/object
 * </a>
 */
class GPTCompletionUsage {
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
}
