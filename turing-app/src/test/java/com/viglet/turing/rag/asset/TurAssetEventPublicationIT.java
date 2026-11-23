/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.rag.asset;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.viglet.turing.storage.asset.TurAssetEvent;
import com.viglet.turing.testutil.AbstractTuringSpringIT;

/**
 * T1324 — the event publication registry on the real schema: an asset event published
 * in a transaction is stored in {@code EVENT_PUBLICATION} (the table the Liquibase
 * changeset creates for Spring Modulith's JPA entity) and marked complete once rag's
 * listener has handled it.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
class TurAssetEventPublicationIT extends AbstractTuringSpringIT {

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void anAssetEventIsStoredWithItsTransactionAndCompletedByTheListener() throws Exception {
        String objectName = "t1324/" + UUID.randomUUID() + ".pdf";

        new TransactionTemplate(transactionManager).executeWithoutResult(
                status -> publisher.publishEvent(TurAssetEvent.deleted(objectName, null)));

        long deadline = System.currentTimeMillis() + 15_000;
        Integer completed = 0;
        while (System.currentTimeMillis() < deadline) {
            completed = jdbc.queryForObject("select count(*) from EVENT_PUBLICATION where completionDate"
                    + " is not null and listenerId like ? and serializedEvent like ?", Integer.class,
                    "%TurAssetEventListener%", "%" + objectName + "%");
            if (completed != null && completed > 0) {
                break;
            }
            Thread.sleep(200);
        }

        assertThat(jdbc.queryForObject("select count(*) from EVENT_PUBLICATION where serializedEvent like ?",
                Integer.class, "%" + objectName + "%")).as("the publication was stored").isEqualTo(1);
        assertThat(completed).as("the listener completed it").isEqualTo(1);
    }
}
