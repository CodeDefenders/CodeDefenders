CREATE TABLE classrooms
(
    ID        int(11)      NOT NULL AUTO_INCREMENT,
    Name      VARCHAR(100) NOT NULL,
    Room_Code VARCHAR(20)  NOT NULL,
    Password  CHAR(60)     NULL DEFAULT NULL,
    Open      tinyint(1)   NOT NULL DEFAULT 0,

    PRIMARY KEY (ID),

    CONSTRAINT unique_room_code
        UNIQUE (Room_Code),

    CONSTRAINT check_name CHECK (Name <> ''),
    CONSTRAINT check_password CHECK (Password <> ''),
    CONSTRAINT check_room_code CHECK (Room_Code <> '')
)
AUTO_INCREMENT = 100;


CREATE TABLE classroom_members
(
    User_ID      int NOT NULL,
    Classroom_ID int NOT NULL,
    Role         enum('STUDENT', 'MODERATOR', 'OWNER') NOT NULL,

    PRIMARY KEY (User_ID, Classroom_ID),

    CONSTRAINT fk_user_id
        FOREIGN KEY (User_ID) REFERENCES users (User_ID) ON UPDATE CASCADE ON DELETE CASCADE,

    CONSTRAINT fk_classroom_id
        FOREIGN KEY (Classroom_ID) REFERENCES classrooms (ID) ON UPDATE CASCADE ON DELETE CASCADE
);
