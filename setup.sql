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
    rAdministrateForums boolean not null,
    name varchar(20) not null,
    rLogin boolean not null,
    description varchar(1000),
    rEditBlog boolean not null,
    id bigint primary key not null auto_increment,
    rAdministrateUsers boolean not null,
    rUseForum boolean not null,
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
create table Forum (
    name varchar(256) not null,
    description varchar(1024),
    parentid bigint,
    last_post_id bigint,
    id bigint primary key not null auto_increment,
    ordering int not null
  );
create table ForumTopic (
    closed datetime,
    userid bigint not null,
    forumid bigint not null,
    id bigint primary key not null auto_increment,
    views int not null,
    replies int not null,
    time datetime not null,
    title varchar(256) not null,
    isSticky boolean not null
  );
create table ForumPost (
    edit_time datetime,
    subject varchar(256) not null,
    edit_count int,
    text text not null,
    userid bigint not null,
    forumid bigint not null,
    id bigint primary key not null auto_increment,
    textile boolean not null,
    time datetime not null,
    edit_user_id bigint,
    topicid bigint not null
  );
create table AccountTypeAssignation (
    allocated bigint not null,
    allocator bigint not null
  );
-- indexes on AccountTypeAssignation
create index idxd2570cde on AccountTypeAssignation (allocator);
create table ForumModerator (
    userId bigint not null,
    forumId bigint not null
  );
-- indexes on ForumModerator
create index idx64a608ba on ForumModerator (forumId);
create table ForumReadAccess (
    accountTypeId bigint not null,
    forumId bigint not null
  );
-- indexes on ForumReadAccess
create index idx6a1508db on ForumReadAccess (forumId);
create table ForumWriteAccess (
    accountTypeId bigint not null,
    forumId bigint not null
  );
-- indexes on ForumWriteAccess
create index idx7657096a on ForumWriteAccess (forumId);
-- foreign key constraints :
alter table User add constraint UserFK3 foreign key (accounttypeId) references AccountType(id);
alter table LoginAttempt add constraint LoginAttemptFK4 foreign key (userId) references User(id);
alter table Blog add constraint BlogFK5 foreign key (authorId) references User(id);
alter table Image add constraint ImageFK6 foreign key (uploaderId) references User(id);
alter table Forum add constraint ForumFK7 foreign key (last_post_id) references ForumPost(id);
alter table Forum add constraint ForumFK8 foreign key (parentid) references Forum(id);
alter table ForumTopic add constraint ForumTopicFK9 foreign key (forumid) references Forum(id);
alter table ForumTopic add constraint ForumTopicFK10 foreign key (userid) references User(id);
alter table ForumPost add constraint ForumPostFK11 foreign key (topicid) references ForumTopic(id);
alter table ForumPost add constraint ForumPostFK12 foreign key (forumid) references Forum(id);
alter table ForumPost add constraint ForumPostFK13 foreign key (userid) references User(id);
alter table ForumPost add constraint ForumPostFK14 foreign key (edit_user_id) references User(id);
alter table AccountTypeAssignation add constraint AccountTypeAssignationFK1 foreign key (allocator) references AccountType(id);
alter table AccountTypeAssignation add constraint AccountTypeAssignationFK2 foreign key (allocated) references AccountType(id);
alter table ForumModerator add constraint ForumModeratorFK15 foreign key (forumId) references Forum(id);
alter table ForumModerator add constraint ForumModeratorFK16 foreign key (userId) references User(id);
alter table ForumReadAccess add constraint ForumReadAccessFK17 foreign key (forumId) references Forum(id);
alter table ForumReadAccess add constraint ForumReadAccessFK18 foreign key (accountTypeId) references AccountType(id);
alter table ForumWriteAccess add constraint ForumWriteAccessFK19 foreign key (forumId) references Forum(id);
alter table ForumWriteAccess add constraint ForumWriteAccessFK20 foreign key (accountTypeId) references AccountType(id);
-- composite key indexes :
alter table AccountTypeAssignation add constraint AccountTypeAssignationCPK unique(allocator,allocated);
alter table ForumModerator add constraint ForumModeratorCPK unique(forumId,userId);
alter table ForumReadAccess add constraint ForumReadAccessCPK unique(forumId,accountTypeId);
alter table ForumWriteAccess add constraint ForumWriteAccessCPK unique(forumId,accountTypeId);
