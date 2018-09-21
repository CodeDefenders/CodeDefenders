package org.codedefenders.model;

import org.codedefenders.game.GameClass;

/**
 * This class represents a dependency which is uploaded together
 * with a {@link GameClass}. The class under test its uploaded with
 * is referenced by the {@link #classId} field.
 *
 * @author <a href=https://github.com/werli>Phil Werli<a/>
 */
public class Dependency {
    private int id;
    private int classId;
    private String javaFile;
    private String classFile;

    public Dependency(int id, int classId, String javaFile, String classFile) {
        this.id = id;
        this.classId = classId;
        this.javaFile = javaFile;
        this.classFile = classFile;
    }

    public Dependency(int classId, String javaFile, String classFile) {
        this.classId = classId;
        this.javaFile = javaFile;
        this.classFile = classFile;
    }

    public int getId() {
        return id;
    }

    public int getClassId() {
        return classId;
    }

    public String getJavaFile() {
        return javaFile;
    }

    public String getClassFile() {
        return classFile;
    }
}
