ALTER TABLE killmapjob
ADD COLUMN Classroom_ID int(11) NULL DEFAULT NULL;

ALTER TABLE killmapjob
ADD CONSTRAINT fk_killmapjob_classroom_id FOREIGN KEY (Classroom_ID) REFERENCES classrooms (ID);

ALTER TABLE killmapjob
ADD CONSTRAINT class_id_unique UNIQUE (Class_ID);

ALTER TABLE killmapjob
ADD CONSTRAINT game_id_unique UNIQUE (Game_ID);

ALTER TABLE killmapjob
ADD CONSTRAINT classroom_id_unique UNIQUE (Classroom_ID);
