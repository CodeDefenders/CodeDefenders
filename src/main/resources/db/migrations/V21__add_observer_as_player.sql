ALTER TABLE
    `players`
    MODIFY COLUMN
        `Role` enum (
        'ATTACKER',
        'DEFENDER',
        'PLAYER',
        'OBSERVER'
        );