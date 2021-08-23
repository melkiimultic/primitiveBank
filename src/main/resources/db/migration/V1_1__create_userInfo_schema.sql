create table IF NOT EXISTS userinfo
(
    id  bigint not null
        constraint userinfo_pkey
            primary key,
    firstname varchar(255),
    last_name varchar(255)
);