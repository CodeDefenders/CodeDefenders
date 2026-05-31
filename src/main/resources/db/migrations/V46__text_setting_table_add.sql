CREATE TABLE text_settings
(
    Language VARCHAR(3)  NOT NULL,
    Name     VARCHAR(50) NOT NULL,
    Value    TEXT DEFAULT NULL,
    PRIMARY KEY (Name, Language)
);
