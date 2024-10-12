-- CREATE DATABASE pancake;

CREATE TABLE IF NOT EXISTS users (
    id bigint,
    name text,
    password_hash bytea -- binary
);

CREATE TABLE IF NOT EXISTS permissions (
    user_id bigint,
    admin boolean,
    manage_server_status boolean,
    see_log boolean,
    see_past_logs boolean
);
