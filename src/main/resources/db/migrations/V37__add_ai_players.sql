insert into `users` (`User_ID`, `Username`, `Password`, `Email`)
values (5, 'AI_Attacker', 'AI_ATTACKER_INACCESSIBLE', 'aiattacker@dummy.com'),
       (6, 'AI_Defender', 'AI_DEFENDER_INACCESSIBLE', 'aidefender@dummy.com'),
       (7, 'AI_PLAYER', 'AI_PLAYER_INACCESSIBLE', 'aiplayer@dummy.com');

create table if not exists `llm_models` (
                                            model_name varchar(50) UNIQUE,
                                            type enum ('OPENAI', 'OLLAMA'),

                                            defender_prompt varchar(1000),
                                            defender_dependencies bool,
                                            defender_dependencies_prompt varchar(1000),
                                            defender_method_focus bool,
                                            defender_method_focus_prompt varchar(1000),

                                            attacker_prompt varchar(1000),
                                            attacker_dependencies bool,
                                            attacker_dependencies_prompt varchar(1000),
                                            attacker_method_focus bool,
                                            attacker_method_focus_prompt varchar(1000),

                                            active bool
);
