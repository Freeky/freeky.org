-- table declarations :
create table User (
    name varchar(20) not null,
    email varchar(128) not null,
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
    rEditBlog boolean not null,
    id bigint primary key not null auto_increment,
    rAdministrateUsers boolean not null,
    rManageImages boolean not null,
    rEditStaticPages boolean not null,
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
    modified datetime not null,
    id bigint primary key not null auto_increment
  );
create table StaticPage (
    name varchar(64) not null,
    lastModified datetime not null,
    description varchar(256) not null,
    id bigint primary key not null auto_increment,
    content text not null,
    keywords varchar(512) not null,
    title varchar(128) not null
  );
-- indexes on StaticPage
create unique index idx2e0405b4 on StaticPage (name);
create table Blog (
    published datetime,
    text text not null,
    modified datetime not null,
    id bigint primary key not null auto_increment,
    authorId bigint not null,
    title varchar(256) not null,
    created datetime not null
  );
create table Image (
    name varchar(256) not null,
    mimeType varchar(64) not null,
    id bigint primary key not null auto_increment,
    uploaded datetime not null,
    uploaderId bigint not null,
    secure varchar(16) not null
  );
-- indexes on Image
create unique index idx1cd20498 on Image (secure);
-- foreign key constraints :
alter table User add constraint UserFK1 foreign key (accounttypeId) references AccountType(id);
alter table LoginAttempt add constraint LoginAttemptFK2 foreign key (userId) references User(id);
alter table Blog add constraint BlogFK3 foreign key (authorId) references User(id);
alter table Image add constraint ImageFK4 foreign key (uploaderId) references User(id);
