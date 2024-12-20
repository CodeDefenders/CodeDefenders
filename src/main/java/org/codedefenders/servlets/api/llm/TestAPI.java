package org.codedefenders.servlets.api.llm;

import com.github.javaparser.quality.Nullable;

public class TestAPI {
    public record SubmitTestRequestDTO(
            int userId,
            int gameId,
            String code
    ) {
    }

    public record SubmitTestResponseDTO(
            boolean success,
            String message,

            @Nullable
            Common.TestDTO test, // null if request failed
            @Nullable
            TestRejectReason rejectReason // null if request succeeded
    ) {
    }

    public enum TestRejectReason {
        VALIDATION_FAILURE,
        COMPILATION_FAILURE,
        TEST_CUT_FAILURE
    }
}
