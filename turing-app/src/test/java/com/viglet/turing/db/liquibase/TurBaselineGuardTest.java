/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.db.liquibase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.jupiter.api.Test;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;

/**
 * T1321 / ADR 0005 section 2 — the schema starts from the per-module baselines. A
 * fresh database takes them and a second run changes nothing; a database built by
 * the pre-baseline history (its DATABASECHANGELOG still lists the old
 * {@code db/changelog/v*.yaml} files) is refused before any baseline runs, with a
 * message naming the export and import path.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
class TurBaselineGuardTest {

    private static final String CHANGELOG = "db/changelog/db.changelog-master.yaml";

    /** The identifier flags mirror {@code application.yaml}'s H2 URL. */
    private static String freshUrl() {
        return "jdbc:h2:mem:baseline_" + System.nanoTime()
                + ";DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false;CASE_INSENSITIVE_IDENTIFIERS=true";
    }

    @Test
    void aFreshDatabaseTakesTheBaselinesAndASecondRunChangesNothing() throws Exception {
        String url = freshUrl();
        update(url);
        int applied = count(url, "select count(*) from DATABASECHANGELOG");
        update(url);

        assertThat(applied).as("the baselines ran").isGreaterThan(100);
        assertThat(count(url, "select count(*) from DATABASECHANGELOG")).isEqualTo(applied);
        assertThat(count(url, "select count(*) from tenant where id = 'DEFAULT'"))
                .as("the default tenant is seeded").isEqualTo(1);
        assertThat(count(url, "select count(*) from llm_price")).as("the price catalog is seeded")
                .isPositive();
    }

    @Test
    void aDatabaseFromThePreBaselineHistoryIsRefused() throws Exception {
        String url = freshUrl();
        update(url);
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
                Statement statement = connection.createStatement()) {
            statement.executeUpdate("insert into DATABASECHANGELOG (ID, AUTHOR, FILENAME, DATEEXECUTED,"
                    + " ORDEREXECUTED, EXECTYPE) values ('1', 'turing', 'db/changelog/v2026.3.1.160.yaml',"
                    + " CURRENT_TIMESTAMP, 999, 'EXECUTED')");
        }

        assertThatThrownBy(() -> update(url))
                .hasStackTraceContaining("before the 2026.3 schema baseline")
                .hasStackTraceContaining("import the export");
    }

    private static void update(String url) throws Exception {
        Connection connection = DriverManager.getConnection(url, "sa", "");
        Database database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(new JdbcConnection(connection));
        try (Liquibase liquibase = new Liquibase(CHANGELOG, new ClassLoaderResourceAccessor(), database)) {
            liquibase.update("");
        }
    }

    private static int count(String url, String sql) throws Exception {
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
                Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        }
    }
}
