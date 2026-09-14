create table if not exists rejected_submissions
(
    Reject_ID       int auto_increment,
    Player_ID       int                     not null,
    Submission_type enum ('TEST', 'MUTANT') not null,
    Timestamp       timestamp default current_timestamp,
    foreign key (Player_ID) references players (ID),
    primary key (Reject_ID)
);

create table if not exists rejection_reasons
(
    Reject_ID            int not null,
    General_description  text,
    Detailed_description text,
    Validation_message   text,
    Reason               text,
    foreign key (Reject_ID) references rejected_submissions (Reject_ID)
)
