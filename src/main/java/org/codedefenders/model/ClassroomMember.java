package org.codedefenders.model;

public class ClassroomMember {
    private final int userId;
    private final int classroomId;
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
