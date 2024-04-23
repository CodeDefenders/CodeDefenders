CREATE TABLE roles
(
    User_ID INT                       not null,
    Role    ENUM ('admin', 'teacher') not null,
    CONSTRAINT fk__roles__user_id
        FOREIGN KEY (User_ID) REFERENCES users (User_ID)
            ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT unique__roles
        UNIQUE (User_ID, Role)
);
