create table if not exists whitelist(
    game_id int(11) not null ,
    user_id int(11) not null,
    type enum('defender', 'attacker', 'flex', 'choice') default 'choice' not null,
    foreign key (game_id) references games(ID) on delete cascade,
    foreign key (user_id) references users(User_ID) on delete cascade);

alter table games
    add column invite_only bool default false,
    add column may_choose_role bool default true;

create table if not exists invitation_links
(
    invitation_id int(11) not null primary key auto_increment,
    game_id       int(11),
    foreign key (game_id) references games (ID) on delete cascade
);
