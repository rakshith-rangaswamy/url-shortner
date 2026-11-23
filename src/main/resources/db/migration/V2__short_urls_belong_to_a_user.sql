-- ADR-0020: every short URL belongs to exactly one user, and reuse is per user.

-- Rows created before userId was required are assigned to 'anonymous' so their links keep working.
UPDATE url_mapping SET user_id = 'anonymous' WHERE user_id IS NULL;

ALTER TABLE url_mapping MODIFY user_id VARCHAR(64) NOT NULL;

-- Reuse lookup is now (user_id, long_url_hash); the single-column indexes are no longer needed.
ALTER TABLE url_mapping
    ADD KEY idx_url_mapping_user_hash (user_id, long_url_hash),
    DROP KEY idx_url_mapping_long_url_hash,
    DROP KEY idx_url_mapping_user_id;
