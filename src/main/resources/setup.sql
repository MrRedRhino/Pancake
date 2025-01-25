-- CREATE DATABASE pancake;

CREATE TABLE IF NOT EXISTS users
(
    id               bigint,
    name             text, -- TODO unique key
    password_hash    bytea,
    is_administrator boolean
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
    id         bigint primary key,
    server_id  bigint,
    created_at timestamp,
    directory  text
);

CREATE TABLE IF NOT EXISTS backup_files
(
    backup_id bigint,
    hash      text,
    path      text
);

CREATE TABLE IF NOT EXISTS backup_file_meta
(
    hash         text primary key,
    storage_type text,
    url          text
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
    show_datapacks_folder   boolean
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

CREATE TABLE IF NOT EXISTS addons_version_cache
(
    file_path         text primary key,
    icon_url          text,
    name              text,
    author            text,
    version_string    text,
    last_update_check timestamp,
    version_data      text
);
