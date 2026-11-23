package com.urlshortener.database;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;

/** Recognises a unique-key violation (MySQL error 1062) anywhere in an exception's cause chain. */
public final class DuplicateKeys {

    private static final int MYSQL_DUPLICATE_ENTRY = 1062;

    private DuplicateKeys() {
    }

    public static boolean isDuplicateKey(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof SQLIntegrityConstraintViolationException) {
                return true;
            }
            if (t instanceof SQLException sql && sql.getErrorCode() == MYSQL_DUPLICATE_ENTRY) {
                return true;
            }
            if (t.getCause() == t) {
                break;
            }
        }
        return false;
    }
}
