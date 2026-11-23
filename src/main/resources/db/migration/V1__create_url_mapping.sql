CREATE TABLE url_mapping (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    short_code    VARCHAR(7)    NOT NULL COLLATE utf8mb4_bin, -- case-sensitive Base62; length = ShortCodes.MAX_LENGTH
    long_url      VARCHAR(2048) NOT NULL,
    long_url_hash CHAR(64)      NOT NULL,                      -- hex digest of long_url
    user_id       VARCHAR(64)   NULL,
    is_custom     BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    expires_at    TIMESTAMP(3)  NULL,                          -- NULL = never expires
    PRIMARY KEY (id),
    UNIQUE KEY uk_url_mapping_short_code (short_code),
    KEY idx_url_mapping_long_url_hash (long_url_hash),
    KEY idx_url_mapping_user_id (user_id),
    KEY idx_url_mapping_expires_at (expires_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
