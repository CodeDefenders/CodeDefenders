/* Drop the old table for chat messages. */
DROP TABLE IF EXISTS `event_chat`;

/* Add a new table for chat messages. */
DROP TABLE IF EXISTS `game_chat_messages`;
CREATE TABLE `game_chat_messages` (
                            `Message_ID` int(11) NOT NULL AUTO_INCREMENT,
                            `Game_ID` int(11) NOT NULL,
                            `User_ID` int(11) NOT NULL,
                            `Role` enum('ATTACKER', 'DEFENDER', 'PLAYER', 'OBSERVER') NOT NULL,
                            `IsAllChat` tinyint(1) NOT NULL,
                            `Message` varchar(500) NOT NULL,
                            `Timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            PRIMARY KEY (`Message_ID`),
                            CONSTRAINT `fk_gameId_chat_messages` FOREIGN KEY (`Game_ID`) REFERENCES `games` (`ID`) ON DELETE CASCADE ON UPDATE CASCADE,
                            CONSTRAINT `fk_userId_chat_messages` FOREIGN KEY (`User_ID`) REFERENCES `users` (`User_ID`) ON DELETE CASCADE ON UPDATE CASCADE
) AUTO_INCREMENT = 0;
