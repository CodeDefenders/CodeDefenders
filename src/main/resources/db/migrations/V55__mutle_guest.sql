-- An inaccessible account owns guest games without authenticating visitors.
INSERT INTO users (User_ID, Username, Password, Email)
VALUES (8, 'Mutle Guest', 'MUTLE_GUEST_INACCESSIBLE', 'mutle-guest@dummy.com');
