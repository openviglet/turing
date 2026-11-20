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

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Table;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;

/**
 * T1317 — every table is named {@code <module>_<entity>}: the module is the
 * package under {@code com.viglet.turing} that owns the entity ({@code se} for
 * {@code searchengine}), so a table's owner reads from the schema alone. Covers
 * entity tables, join tables and element-collection tables, and checks that the
 * master changelog leaves each of them in place on a fresh database.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
class TurTableNamingConventionTest {

    private static final String BASE_PACKAGE = "com.viglet.turing";
    private static final Map<String, String> PREFIX_BY_MODULE = Map.of("searchengine", "se");

    /** Table name to the entity that maps it, for every table the model names. */
    private static Map<String, Class<?>> mappedTables() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
        Map<String, Class<?>> tables = new TreeMap<>();
        for (var candidate : scanner.findCandidateComponents(BASE_PACKAGE)) {
            Class<?> entity = Class.forName(candidate.getBeanClassName());
            Table table = entity.getAnnotation(Table.class);
            if (table != null && !table.name().isBlank()) {
                tables.put(table.name(), entity);
            }
            for (Field field : entity.getDeclaredFields()) {
                JoinTable joinTable = field.getAnnotation(JoinTable.class);
                if (joinTable != null && !joinTable.name().isBlank()) {
                    tables.put(joinTable.name(), entity);
                }
                CollectionTable collectionTable = field.getAnnotation(CollectionTable.class);
                if (collectionTable != null && !collectionTable.name().isBlank()) {
                    tables.put(collectionTable.name(), entity);
                }
            }
        }
        return tables;
    }

    private static String prefixOf(Class<?> entity) {
        String module = entity.getPackageName().substring(BASE_PACKAGE.length() + 1).split("\\.")[0];
        return PREFIX_BY_MODULE.getOrDefault(module, module);
    }

    @Test
    void everyTableIsNamedAfterItsModule() throws Exception {
        Map<String, Class<?>> tables = mappedTables();
        assertThat(tables).as("the scan found the model").hasSizeGreaterThan(100);
        List<String> offenders = new ArrayList<>();
        tables.forEach((table, entity) -> {
            String prefix = prefixOf(entity);
            if (!table.equals(prefix) && !table.startsWith(prefix + "_")) {
                offenders.add(table + " (" + entity.getSimpleName() + ", want " + prefix + "_...)");
            }
        });
        assertThat(offenders).as("tables outside the <module>_<entity> rule").isEmpty();
    }

    @Test
    void theMasterChangelogLeavesEveryMappedTableInPlace() throws Exception {
        String url = "jdbc:h2:mem:naming_" + System.nanoTime()
                + ";DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false;CASE_INSENSITIVE_IDENTIFIERS=true";
        Connection migrating = DriverManager.getConnection(url, "sa", "");
        Database database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(new JdbcConnection(migrating));
        try (Liquibase liquibase = new Liquibase("db/changelog/db.changelog-master.yaml",
                new ClassLoaderResourceAccessor(), database)) {
            liquibase.update("");
        }
        try (Connection connection = DriverManager.getConnection(url, "sa", "")) {
            List<String> missing = new ArrayList<>();
            for (String table : mappedTables().keySet()) {
                if (!exists(connection, table)) {
                    missing.add(table);
                }
            }
            assertThat(missing).as("mapped tables the changelog does not create").isEmpty();
        }
    }

    private static boolean exists(Connection connection, String table) throws Exception {
        for (String name : List.of(table, table.toUpperCase(), table.toLowerCase())) {
            try (ResultSet rs = connection.getMetaData().getTables(null, null, name, null)) {
                if (rs.next()) {
                    return true;
                }
            }
        }
        return false;
    }
}
