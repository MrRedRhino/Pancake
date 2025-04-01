-- CREATE DATABASE pancake;

CREATE TABLE IF NOT EXISTS users
(
    id               bigint,
    name             text, -- TODO unique key
    password_hash    bytea,
    is_administrator boolean,
    manage_users     boolean,
    manage_servers   boolean
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

CREATE TABLE IF NOT EXISTS backups
(
    id          bigint primary key,
    server_id   bigint,
    name        text,
    created_at  timestamp,
    directory   text,
    incremental boolean,
    completed   boolean
);

CREATE TABLE IF NOT EXISTS backup_files
(
    backup_id bigint,
    hash      text,
    path      text
);

CREATE TABLE IF NOT EXISTS backup_file_meta
(
    hash                  bytea primary key,
    storage_configuration bigint,
    url                   text
);

CREATE TABLE IF NOT EXISTS servers
(
    id                      long primary key,
    name                    text,
    path                    text,
    start_command           text,
    mod_platform_priorities text,
    show_plugin_folder      boolean,
    show_mods_folder        boolean,
    show_datapacks_folder boolean,
    loader                text,
    game_version          text,
    loader_version        text
);

CREATE TABLE IF NOT EXISTS sessions
(
    token      text primary key,
    user_id    long,
    user_agent text,
    created_at timestamp,
    expires_at timestamp
);

CREATE TABLE IF NOT EXISTS sessions
(
    token      text primary key,
    user_id    long,
    user_agent text,
    created_at timestamp,
    expires_at timestamp
);

CREATE TABLE IF NOT EXISTS addon_meta
(
    hash           text,
    platform       text,
    found          boolean,
    id             text,
    icon_url       text,
    name           text,
    author         text,
    version_string text,
    page_url       text,
    version_uri    text,
    update_uri     text,
    fetched_at     timestamp,
    primary key (hash, platform)
);
