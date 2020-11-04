/* Drop the old table for chat messages. */
DROP TABLE IF EXISTS `event_chat`;

/* Add a new table for chat messages. */
DROP TABLE IF EXISTS `game_chat_messages`;
CREATE TABLE `game_chat_messages` (
                            `Message_ID` int(11) NOT NULL AUTO_INCREMENT,
                            `Player_ID` int(11) NULL,
                            `IsAllChat` tinyint(1) NOT NULL,
                            `Message` varchar(500) NOT NULL,
                            `Timestamp` timestamp NOT NULL,
                            PRIMARY KEY (`Message_ID`),
                            CONSTRAINT `fk_playerId_chat_messages` FOREIGN KEY (`Player_ID`) REFERENCES `players` (`ID`) ON DELETE SET NULL ON UPDATE CASCADE
) AUTO_INCREMENT = 0;
