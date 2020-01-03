package org.codedefenders.notification.events.server.test;

import com.google.gson.annotations.Expose;

public class TestDuplicateCheckedEvent extends TestLifecycleEvent {
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
