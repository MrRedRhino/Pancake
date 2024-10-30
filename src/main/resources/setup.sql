-- CREATE DATABASE pancake;

CREATE TABLE IF NOT EXISTS users
(
    id            bigint,
    name          text,
    password_hash bytea
);

CREATE TABLE IF NOT EXISTS permissions
(
    user_id              bigint,
    admin                boolean,
    manage_server_status boolean,
    see_log              boolean,
    see_past_logs        boolean
);

CREATE TABLE IF NOT EXISTS jobs
(
    id        bigint,
    name      text,
    server_id bigint,
    config    text,
    primary key (id, server_id)
);

CREATE TABLE IF NOT EXISTS job_logs
(
    job_id     bigint,
    started_at timestamp,
    log        text,
    success    boolean
);

