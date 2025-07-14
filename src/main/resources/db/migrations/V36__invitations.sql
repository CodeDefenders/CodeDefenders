create table if not exists whitelist(
    Game_Id int(11) not null ,
    User_Id int(11) not null,
    Type enum('DEFENDER', 'ATTACKER', 'FLEX', 'CHOICE') default 'CHOICE' not null,
    foreign key (Game_id) references games(ID) on delete cascade,
    foreign key (User_id) references users(User_ID) on delete cascade);

alter table games
    add column Invite_Only bool default false,
    add column May_Choose_Role bool default true;

create table if not exists invitation_links
(
    Invitation_Id int(11) not null primary key auto_increment,
    Game_Id       int(11),
    Creator_Id    int(11),
    foreign key (Game_Id) references games (ID) on delete cascade,
    foreign key (Creator_Id) references users (User_ID) on delete cascade
);
