/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.viglet.turing.sn.entity;

import com.viglet.turing.persistence.model.llm.TurLLMInstance;

/**
 * T1228 — the seam between the indexer and the model: the entities an LLM finds
 * in a document's text, unchecked. The indexer checks every value against the
 * text, so an implementation may return anything the model said.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.5
 */
public interface TurSNEntityExtractor {

    /**
     * @param siteName the site the document is indexed into, which the spend is
     *                 attributed to
     * @return the entities the model recorded, {@link TurSNEntities#NONE} when it
     *         recorded none
     */
    TurSNEntities extract(String siteName, TurLLMInstance llm, String text);
}
