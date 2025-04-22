create table if not exists whitelist(
    game_id int(11) not null ,
    user_id int(11) not null,
    foreign key (game_id) references games(ID) on delete cascade,
    foreign key (user_id) references users(User_ID) on delete cascade);

alter table games
    add column if not exists invite_only bool default false,
    add column if not exists may_choose_role bool default true;
