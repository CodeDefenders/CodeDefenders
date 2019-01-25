package org.codedefenders.api.analytics;

import org.codedefenders.game.Role;

public class KillmapDataDTO implements Comparable<KillmapDataDTO> {
    private int userId;
    private int classId;
    private String userName;
    private String className;
    private Role role;
    private int usefulMutants;
    private int usefulTests;

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getClassId() {
        return classId;
    }

    public void setClassId(int classId) {
        this.classId = classId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public int getUsefulMutants() {
        return usefulMutants;
    }

    public void setUsefulMutants(int usefulMutants) {
        this.usefulMutants = usefulMutants;
    }

    public int getUsefulTests() {
        return usefulTests;
    }

    public void setUsefulTests(int usefulTests) {
        this.usefulTests = usefulTests;
    }

    @Override
    public int compareTo(KillmapDataDTO o) {
        if (this.classId != o.classId) {
            return this.classId - o.classId;
        } else {
            return this.userId - o.userId;
        }
    }
}
