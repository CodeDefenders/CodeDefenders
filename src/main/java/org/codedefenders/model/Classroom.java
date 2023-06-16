package org.codedefenders.model;

import java.util.Optional;
import java.util.UUID;

import com.google.gson.annotations.Expose;

public class Classroom {
    @Expose
    private final int id;

    @Expose
    private final UUID uuid;

    @Expose
    private final Integer creatorId;

    @Expose
    private final String name;

    private final String password;

    @Expose
    private final boolean open;

    @Expose
    private final boolean visible;

    @Expose
    private final boolean archived;

    public Classroom(
            int id,
            UUID uuid,
            Integer creatorId,
            String name,
            String password,
            boolean open,
            boolean visible,
            boolean archived) {
        this.id = id;
        this.uuid = uuid;
        this.creatorId = creatorId;
        this.name = name;
        this.password = password;
        this.open = open;
        this.visible = visible;
        this.archived = archived;
    }

    public int getId() {
        return id;
    }

    public UUID getUUID() {
        return uuid;
    }

    public Optional<Integer> getCreatorId() {
        return Optional.ofNullable(creatorId);
    }

    public String getName() {
        return name;
    }

    public Optional<String> getPassword() {
        return Optional.ofNullable(password);
    }

    public boolean isOpen() {
        return open;
    }

    public boolean isVisible() {
        return visible;
    }

    public boolean isArchived() {
        return archived;
    }

    public static Builder builderFrom(Classroom classroom) {
        return new Builder(classroom);
    }

    public static class Builder {
        private int id;
        private UUID uuid;
        private Integer creatorId;
        private String name;
        private String password;
        private boolean open;
        private boolean visible;
        private boolean archived;

        private Builder() {

        }

        private Builder(Classroom classroom) {
            this.id = classroom.getId();
            this.uuid = classroom.getUUID();
            this.creatorId = classroom.getCreatorId().orElse(null);
            this.name = classroom.getName();
            this.password = classroom.getPassword().orElse(null);
            this.open = classroom.isOpen();
            this.visible = classroom.isVisible();
            this.archived = classroom.isArchived();
        }

        public Builder setId(int id) {
            this.id = id;
            return this;
        }

        public Builder setUUID(UUID uuid) {
            this.uuid = uuid;
            return this;
        }

        public Builder setCreatorId(Integer creatorId) {
            this.creatorId = creatorId;
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
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

        public Builder setVisible(boolean visible) {
            this.visible = visible;
            return this;
        }

        public Builder setArchived(boolean archived) {
            this.archived = archived;
            return this;
        }

        public Classroom build() {
            return new Classroom(
                    this.id,
                    this.uuid,
                    this.creatorId,
                    this.name,
                    this.password,
                    this.open,
                    this.visible,
                    this.archived
            );
        }
    }
}
