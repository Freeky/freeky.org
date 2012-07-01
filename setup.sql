-- table declarations :
create table User (
    name varchar(20) not null,
    email varchar(256) not null,
    id bigint primary key not null auto_increment,
    passwordhash varchar(30) not null,
    passwordsalt varchar(20) not null,
    registrationdate datetime not null,
    accounttypeId bigint not null
  );
-- indexes on User
create unique index idx1108036e on User (name);
create unique index idx14c903d5 on User (email);
create table AccountType (
    name varchar(20) not null,
    rLogin boolean not null,
    description varchar(1000),
    id bigint primary key not null auto_increment,
    rAdministrateUsers boolean not null,
    rEditProjects boolean not null,
    rSendMail boolean not null
  );
create table LoginAttempt (
    ip varchar(39) not null,
    success boolean not null,
    validated boolean not null,
    id bigint primary key not null auto_increment,
    time datetime not null,
    userId bigint not null
  );
-- indexes on LoginAttempt
create index idx3be206b5 on LoginAttempt (time);
create index idx4a810772 on LoginAttempt (userId);
create table Project (
    name varchar(128) not null,
    description varchar(256) not null,
    text text not null,
    id bigint primary key not null auto_increment
  );
-- foreign key constraints :
alter table User add constraint UserFK1 foreign key (accounttypeId) references AccountType(id);
alter table LoginAttempt add constraint LoginAttemptFK2 foreign key (userId) references User(id);
