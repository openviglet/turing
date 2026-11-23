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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;

/**
 * T1321 — the per-module schema baselines on the engines H2 is too permissive to
 * stand in for: the master changelog applies from scratch on MySQL 8 and on
 * PostgreSQL 16, leaves every table the entities map in place, and a second run is a
 * no-op. Skipped when Docker is not available.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@Testcontainers(disabledWithoutDocker = true)
class TurBaselineChangelogIT {

    private static final String CHANGELOG = "db/changelog/db.changelog-master.yaml";

    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0")
            .withDatabaseName("turing_it").withUsername("turing").withPassword("turing");

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("turing_it").withUsername("turing").withPassword("turing");

    @Test
    void theBaselinesApplyOnMySql() throws Exception {
        assertBaselinesApply(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
    }

    @Test
    void theBaselinesApplyOnPostgreSql() throws Exception {
        assertBaselinesApply(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private static void assertBaselinesApply(String url, String user, String password) throws Exception {
        update(url, user, password);
        update(url, user, password);
        try (Connection connection = DriverManager.getConnection(url, user, password)) {
            List<String> missing = new ArrayList<>();
            for (String table : TurTableNamingConventionTest.mappedTables().keySet()) {
                if (!exists(connection, table)) {
                    missing.add(table);
                }
            }
            assertThat(missing).as("mapped tables the baselines do not create").isEmpty();
        }
    }

    private static void update(String url, String user, String password) throws Exception {
        Connection connection = DriverManager.getConnection(url, user, password);
        Database database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(new JdbcConnection(connection));
        try (Liquibase liquibase = new Liquibase(CHANGELOG, new ClassLoaderResourceAccessor(), database)) {
            liquibase.update("");
        }
    }

    private static boolean exists(Connection connection, String table) throws Exception {
        for (String name : List.of(table, table.toLowerCase())) {
            try (ResultSet rs = connection.getMetaData().getTables(connection.getCatalog(), null, name,
                    new String[] {"TABLE"})) {
                if (rs.next()) {
                    return true;
                }
            }
        }
        return false;
    }
}
