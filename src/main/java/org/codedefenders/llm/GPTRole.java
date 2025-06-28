package org.codedefenders.llm;

import com.google.gson.annotations.SerializedName;

/**
 * This enum represents the role property of a message object for the GPT API. Specifications about the possible roles
 * are available at
 * <a href="https://platform.openai.com/docs/api-reference/chat/create">
 *     https://platform.openai.com/docs/api-reference/chat/create
 * </a>
 */
enum GPTRole {
    @SerializedName("system") SYSTEM,
    @SerializedName("user") USER,
    @SerializedName("assistant") ASSISTANT
    //TODO "developer" role for models o1 and newer
}
