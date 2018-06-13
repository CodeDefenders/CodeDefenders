LOCK TABLES `classes` WRITE;
ALTER TABLE `classes` DISABLE KEYS;

INSERT INTO `classes` (Class_ID, Name, JavaFile, ClassFile, Alias, AiPrepared, RequireMocking) VALUES

(221, 'Lift', '/var/lib/codedefenders/sources/Lift/Lift.java', '/var/lib/codedefenders/sources/Lift/Lift.class', 'Lift', 0, 0);

ALTER TABLE `classes` ENABLE KEYS;
UNLOCK TABLES;