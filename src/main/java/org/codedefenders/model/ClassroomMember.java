package org.codedefenders.model;

import com.google.gson.annotations.Expose;

public class ClassroomMember {
    @Expose
    private final int userId;

    @Expose
    private final int classroomId;

    @Expose
    private final ClassroomRole role;

    public ClassroomMember(int userId, int classroomId, ClassroomRole role) {
        this.userId = userId;
        this.classroomId = classroomId;
        this.role = role;
    }

    public int getUserId() {
        return userId;
    }

    public int getClassroomId() {
        return classroomId;
    }

    public ClassroomRole getRole() {
        return role;
    }
}
