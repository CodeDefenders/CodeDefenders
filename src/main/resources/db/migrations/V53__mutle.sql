CREATE TABLE mutle_fixture
(
    fixture_key       varchar(128) NOT NULL,
    content_hash      char(64)     NOT NULL,
    hidden_puzzle_id  int(11)      NOT NULL,
    template_mutant_id int(11)     NOT NULL,
    installed_at      timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (fixture_key),
    UNIQUE KEY uq_mutle_fixture_mutant (template_mutant_id),
    CONSTRAINT fk_mutle_fixture_puzzle FOREIGN KEY (hidden_puzzle_id) REFERENCES puzzles (Puzzle_ID)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_mutle_fixture_mutant FOREIGN KEY (template_mutant_id) REFERENCES mutants (Mutant_ID)
        ON UPDATE CASCADE ON DELETE RESTRICT
);

CREATE TABLE mutle_game
(
    game_id               int(11)      NOT NULL,
    fixture_key           varchar(128) NOT NULL,
    rotation_period       bigint       NOT NULL,
    user_id               int(11)      NOT NULL,
    submission_token      varchar(64)  NULL DEFAULT NULL,
    submission_started_at timestamp(3) NULL DEFAULT NULL,
    created_at            timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (game_id),
    UNIQUE KEY uq_mutle_game_user_period (user_id, rotation_period),
    KEY idx_mutle_game_user_created (user_id, created_at),
    CONSTRAINT fk_mutle_game_game FOREIGN KEY (game_id) REFERENCES games (ID)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_mutle_game_fixture FOREIGN KEY (fixture_key) REFERENCES mutle_fixture (fixture_key)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_mutle_game_user FOREIGN KEY (user_id) REFERENCES users (User_ID)
        ON UPDATE CASCADE ON DELETE CASCADE
);

INSERT INTO settings (name, type, STRING_VALUE, INT_VALUE, BOOL_VALUE)
VALUES ('ALLOW_MUTLE_SECTION', 'BOOL_VALUE', NULL, NULL, TRUE);
