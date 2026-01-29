insert into `users` (`User_ID`, `Username`, `Password`, `Email`)
values (5, 'AI_Attacker', 'AI_ATTACKER_INACCESSIBLE', 'aiattacker@dummy.com'),
       (6, 'AI_Defender', 'AI_DEFENDER_INACCESSIBLE', 'aidefender@dummy.com'),
       (7, 'AI_PLAYER', 'AI_PLAYER_INACCESSIBLE', 'aiplayer@dummy.com');

insert into settings
values ('LLM_INTERVAL_SECONDS', 'INT_VALUE', NULL, 20, NULL),
       ('LLM_NORMAL_PROMPT_NUMBER_OF_TRIES', 'INT_VALUE', NULL, 3, NULL),
       ('LLM_EQUIVALENCE_DUEL_NUMBER_OF_TRIES', 'INT_VALUE', NULL, 0, NULL);

create table if not exists `llm_models`
(
    model_name            varchar(50),
    type                  enum ('OPENAI', 'OLLAMA', 'DEFAULT'),
    defender_dependencies bool,
    defender_method_focus bool,
    attacker_dependencies bool,

    active                bool,
    PRIMARY KEY (model_name, type)
);

create table if not exists `llm_prompts`
(
    model_name  varchar(50),
    model_type  enum ('OPENAI', 'OLLAMA', 'DEFAULT'),
    prompt_type varchar(50) not null,
    prompt      text,

    foreign key (model_name, model_type) references llm_models (model_name, type),
    unique (model_name, model_type, prompt_type)

);

create table if not exists `llm_conversations`
(
    Conversation_ID int(11)      not null primary key auto_increment,
    Strategy        varchar(100) not null,
    Type            varchar(20)  not null,
    Game_ID         int(11)      not null,
    User_ID         int(11)      not null,
    Mutant_ID       int(11),
    Test_ID         int(11),
    Is_Active       bool         not null,
    Is_Success      bool         not null,

    foreign key (Game_ID) references games (ID),
    foreign key (User_ID) references users (User_ID),
    foreign key (Mutant_ID) references mutants (Mutant_ID),
    foreign key (Test_ID) references tests (Test_id)
);

create table if not exists `llm_messages`
(
    Conversation_ID       int(11)                              not null,
    Index_in_conversation int(11)                              not null,
    Message_type          enum ('SYSTEM', 'USER', 'AI')        not null,
    Input_tokens          int(11),
    Output_tokens         int(11),
    timestamp             timestamp                            not null,
    Model_name            varchar(50)                          not null,
    Model_type            enum ('OPENAI', 'OLLAMA', 'DEFAULT') not null,
    Content               text                                 not null,


    primary key (Conversation_ID, Index_in_conversation),
    foreign key (Conversation_ID) references llm_conversations (Conversation_ID),
    foreign key (Model_name, Model_type) references llm_models (model_name, type)
);

create table if not exists experiment
(
    Experiment_type        varchar(50),
    Experiment_name        varchar(50),
    Experiment_tag         varchar(50),
    Experiment_description varchar(200),
    Int_value              int(11),
    UNIQUE (Experiment_type, Experiment_name, Experiment_tag, Int_value)
);
