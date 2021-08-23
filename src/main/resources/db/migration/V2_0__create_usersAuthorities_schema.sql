
create table IF NOT EXISTS user_authorities
(
    user_id     bigint not null
        constraint users_pkey
            references users,
    authorities varchar(255)
);

