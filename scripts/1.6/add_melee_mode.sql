/* Add new Player Role for melee game */
ALTER TABLE `players` MODIFY COLUMN `Role` enum('ATTACKER','DEFENDER','PLAYER') NOT NULL;
ALTER TABLE `games` MODIFY COLUMN `ActiveRole` ENUM('ATTACKER','DEFENDER','PLAYER') NOT NULL DEFAULT 'ATTACKER';

/* Add new game mode */
ALTER TABLE `games` MODIFY COLUMN `Mode` enum('SINGLE','DUEL','PARTY','UTESTING','PUZZLE', 'MELEE') NOT NULL DEFAULT 'PARTY';

/* Add new events for melee mode*/
INSERT INTO `event_messages` VALUES
('PLAYER_JOINED','@event_user joined the game'),
('PLAYER_MESSAGE','@event_user: @chat_message'),
('PLAYER_MUTANT_CREATED','@event_user created a mutant'),
('PLAYER_MUTANT_ERROR','@event_user created a mutant that errored'),
('PLAYER_MUTANT_KILLED_EQUIVALENT','@event_user proved a mutant non-equivalent'),
('PLAYER_MUTANT_SURVIVED','@event_user created a mutant that survived'),
('PLAYER_KILLED_MUTANT','@event_user killed a mutant'),
('PLAYER_MUTANT_CLAIMED_EQUIVALENT','@event_user claimed a mutant equivalent'),
('PLAYER_MUTANT_EQUIVALENT','@event_user caught an equivalence'),
('PLAYER_TEST_CREATED','@event_user created a test'),
('PLAYER_TEST_ERROR','@event_user created a test that errored'),
('PLAYER_TEST_READY','Test by @event_user is ready'),
('GAME_MESSAGE_PLAYER','@event_user: @chat_message');
