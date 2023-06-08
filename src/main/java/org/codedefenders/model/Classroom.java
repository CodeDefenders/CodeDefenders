package org.codedefenders.model;

import java.util.Optional;

public class Classroom {
    private final int id;
    private final String name;
    private final String roomCode;
    private final String password;
    private final boolean open;

    public Classroom(int id, String name, String roomCode, String password, boolean open) {
        this.id = id;
        this.name = name;
        this.roomCode = roomCode;
        this.password = password;
        this.open = open;
    }

    public Classroom(String name, String roomCode, String password, boolean open) {
        this(-1, name, roomCode, password, open);
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public Optional<String> getPassword() {
        return Optional.ofNullable(password);
    }

    public boolean isOpen() {
        return open;
    }

    public static Builder emptyBuilder() {
        return new Builder();
    }

    public static Builder builderFrom(Classroom classroom) {
        return new Builder(classroom);
    }

    public static class Builder {
        private int id;
        private String name;
        private String roomCode;
        private String password;
        private boolean open;

        private Builder() {

        }

        private Builder(Classroom classroom) {
            this.id = classroom.getId();
            this.name = classroom.getName();
            this.roomCode = classroom.getRoomCode();
            this.password = classroom.getPassword().orElse(null);
            this.open = classroom.isOpen();
        }

        public Builder setId(int id) {
            this.id = id;
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setRoomCode(String roomCode) {
            this.roomCode = roomCode;
            return this;
        }

        public Builder setPassword(String password) {
            this.password = password;
            return this;
        }

        public Builder setOpen(boolean open) {
            this.open = open;
            return this;
        }

        public Classroom build() {
            return new Classroom(
                    this.id,
                    this.name,
                    this.roomCode,
                    this.password,
                    this.open
            );
        }
    }
}
