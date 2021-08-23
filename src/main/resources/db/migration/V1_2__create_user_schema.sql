create table IF NOT EXISTS users
(
    id  bigint not null
        constraint users_pkey
            primary key ,
    password    varchar(255),
    username    varchar(255)
        constraint uk
            unique,
    userinfo_id bigint
        constraint userinfo_pkey
            references userinfo
);