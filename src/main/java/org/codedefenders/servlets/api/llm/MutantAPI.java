package org.codedefenders.servlets.api.llm;

import com.github.javaparser.quality.Nullable;

public class MutantAPI {
    public record SubmitMutantRequestDTO(
            int userId,
            int gameId,
            String code
    ) {
    }

    public record SubmitMutantResponseDTO(
            boolean success,
            String message,

            @Nullable
            Common.MutantDTO mutant, // null if request failed
            @Nullable
            MutantRejectReason rejectReason // null if request succeeded
    ) {
    }

    public enum MutantRejectReason {
        VALIDATION_FAILURE,
        COMPILATION_FAILURE,
    }
}
