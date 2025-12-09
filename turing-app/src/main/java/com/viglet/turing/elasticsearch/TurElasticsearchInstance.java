/*
 * Copyright (C) 2016-2022 the original author or authors.
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.viglet.turing.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.net.URL;

/**
 * Elasticsearch instance holder
 *
 * @author Alexandre Oliveira
 * @since 2025.4.4
 */
@Slf4j
@Getter
@Setter
public class TurElasticsearchInstance {
    private ElasticsearchClient client;
    private String index;
    private URL elasticsearchUrl;

    public TurElasticsearchInstance(ElasticsearchClient client, URL elasticsearchUrl, String index) {
        this.client = client;
        this.elasticsearchUrl = elasticsearchUrl;
        this.index = index;
    }

    public void close() {
        try {
            if (client != null) {
                client._transport().close();
                client = null;
            }
        } catch (Exception e) {
            log.error("Error closing Elasticsearch client: {}", e.getMessage(), e);
        }
    }
}
