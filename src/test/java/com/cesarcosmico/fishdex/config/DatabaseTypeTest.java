package com.cesarcosmico.fishdex.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DatabaseTypeTest {

    @Test
    void parsesKnownTypesIgnoringCase() {
        assertEquals(DatabaseType.SQLITE, DatabaseType.parse(null));
        assertEquals(DatabaseType.MYSQL, DatabaseType.parse(" MySQL "));
        assertEquals(DatabaseType.MARIADB, DatabaseType.parse("mariadb"));
    }

    @Test
    void rejectsUnknownTypesInsteadOfFallingBackToSqlite() {
        assertThrows(IllegalArgumentException.class, () -> DatabaseType.parse("mysq"));
    }
}
