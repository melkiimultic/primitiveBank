
create table IF NOT EXISTS accounts
(
    id      bigint not null
        constraint accounts_pkey
            primary key,
    balance numeric(10, 2) default 0.00,
    user_id bigint
        constraint users_pkey
            references users
);
