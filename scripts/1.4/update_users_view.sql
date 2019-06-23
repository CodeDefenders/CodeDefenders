CREATE OR REPLACE VIEW `view_valid_users` AS
SELECT * FROM `users`
   WHERE `User_ID` >= 5
    AND Active = 1;
