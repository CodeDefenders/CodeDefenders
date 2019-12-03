package org.codedefenders.notification.events.server.mutant;

import com.google.gson.annotations.Expose;

public class MutantDuplicateCheckedEvent extends MutantLifecycleEvent {
    @Expose private boolean success;
    @Expose private Integer duplicateId;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Integer getDuplicateId() {
        return duplicateId;
    }

    public void setDuplicateId(Integer duplicateId) {
        this.duplicateId = duplicateId;
    }
}
