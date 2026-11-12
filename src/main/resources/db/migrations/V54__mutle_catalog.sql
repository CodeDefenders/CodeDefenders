CREATE TABLE mutle_fixture_bundle
(
    class_key     varchar(128) NOT NULL,
    class_json    mediumtext   NOT NULL,
    resources_json mediumtext  NOT NULL,
    content_hash  char(64)     NOT NULL,
    installed_at  timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (class_key)
);

CREATE TABLE mutle_publication
(
    series      bigint       NOT NULL,
    fixture_key varchar(128) NOT NULL,
    release_at  timestamp    NOT NULL,
    PRIMARY KEY (series),
    UNIQUE KEY uq_mutle_publication_fixture (fixture_key),
    KEY idx_mutle_publication_release (release_at)
);
