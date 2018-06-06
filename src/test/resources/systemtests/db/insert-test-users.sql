LOCK TABLES `users` WRITE;
ALTER TABLE `users` DISABLE KEYS;

INSERT INTO `users` (User_ID, Username, Password, Email, Validated, Active) VALUES

-- Standard user:
(07, 'codedefenders', '$2a$10$52HLJ5/odEaRS7Cw2K9Siu0HVU.Lz0ISJrs8dY/DbAxrPODPYjXDu', 'codedefenders@web.de', 1, 1), -- password: codedefenderspw

-- Accounts for testing with multiple users:
(08, 'userA', '$2a$10$CDvOGRlOML6GxP5axd.nZu2sN6T5g.oxjkzsc61oD8Mz48ard/A86', 'usera@dummy.com', 1, 1), -- password: passwordA
(09, 'userB', '$2a$10$1Xqk3WheHXb4spUAZrtxrOEtbXDbzLfOayZejlViY.rzEPMEk8AFy', 'userb@dummy.com', 1, 1), -- password: passwordB
(10, 'userC', '$2a$10$1iRtTMukfmM40ZzPaWPwDOSNaSDT7t61iAel2OEFtHkkbLMhWNS6G', 'userc@dummy.com', 1, 1), -- password: passwordC
(11, 'userD', '$2a$10$KMDg2GWWFSrpFXc/Ec3juuidEPOouEnJTojY9peT1e51aY4WISedO', 'userd@dummy.com', 1, 1), -- password: passwordD
(12, 'userE', '$2a$10$91w0QFMjV6tSxfQOChsAIOshmk43YRE7iEslUBJ376Mm41BgcxzTW', 'usere@dummy.com', 1, 1), -- password: passwordE
(13, 'userF', '$2a$10$KKnM6RxQtiF87y.rsl0ETeHFkFwvjaziEGwGf89PkT/r0OW2PyTOO', 'userf@dummy.com', 1, 1), -- password: passwordF
(14, 'userG', '$2a$10$0I4lOgzhQ3hEge/iuPTLUOYmOUoOj2oLe/4VNfFOojngr0QSCG5Ae', 'userg@dummy.com', 1, 1), -- password: passwordG
(15, 'userH', '$2a$10$RQzPf2SgKNtrDZOVUjNJPuQmwd3QjUSewWW4vKWxQtgeK4U9m.8RO', 'userh@dummy.com', 1, 1); -- password: passwordH

ALTER TABLE `users` ENABLE KEYS;
UNLOCK TABLES;