--liquibase formatted sql

--changeset claude:20261103090000-1
-- "Angemeldet bleiben": Spring Security's persistent remember-me tokens. HTTP sessions live only in
-- the app's memory and die with every restart/deploy; a remember-me cookie (30 days) re-authenticates
-- silently. The cookie only carries a random series + token, the mapping lives here; the token is
-- rotated on every use, so a stolen cookie that was already used is detected and the series
-- revoked. Logout, password changes and user deletion remove a user's rows. Layout = Spring's
-- JdbcTokenRepositoryImpl schema, accessed through Ebean (EbeanPersistentTokenRepository).
CREATE TABLE persistent_logins (
    series VARCHAR(64) NOT NULL PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    token VARCHAR(64) NOT NULL,
    last_used DATETIME NOT NULL,
    INDEX idx_persistent_logins_username (username)
);
