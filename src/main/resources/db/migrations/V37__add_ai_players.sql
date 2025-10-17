insert into `users` (`User_ID`, `Username`, `Password`, `Email`)
values (5, 'AI_Attacker', 'AI_ATTACKER_INACCESSIBLE', 'aiattacker@dummy.com'),
       (6, 'AI_Defender', 'AI_DEFENDER_INACCESSIBLE', 'aidefender@dummy.com'),
       (7, 'AI_PLAYER', 'AI_PLAYER_INACCESSIBLE', 'aiplayer@dummy.com');

insert into settings values ('LLM_INTERVAL_SECONDS', 'INT_VALUE', NULL, 20, NULL),
                            ('LLM_NORMAL_PROMPT_NUMBER_OF_TRIES', 'INT_VALUE', NULL, 3, NULL),
                            ('LLM_EQUIVALENCE_DUEL_NUMBER_OF_TRIES', 'INT_VALUE', NULL, 0, NULL);

create table if not exists `llm_models` (
                                            model_name varchar(50),
                                            type enum ('OPENAI', 'OLLAMA', 'DEFAULT'),

                                            defender_prompt varchar(1000),
                                            defender_dependencies bool,
                                            defender_dependencies_prompt varchar(1000),
                                            defender_method_focus bool,
                                            defender_method_focus_prompt varchar(1000),

                                            attacker_prompt varchar(1000),
                                            attacker_dependencies bool,
                                            attacker_dependencies_prompt varchar(1000),
                                            attacker_resolve_equivalence_prompt varchar(1000),

                                            active bool,
                                            UNIQUE (model_name, type)
);
