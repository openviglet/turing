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

package com.viglet.turing.sn.manifest;

/**
 * A custom facet an SN manifest removes (T1357): the one named {@code name} on
 * {@code field}, which the client provisioned in an earlier manifest and no
 * longer declares. A facet the site does not have is not an error, so the same
 * manifest can be posted again. Nothing else is removed: a facet neither
 * declared nor retired stays as the console made it.
 *
 * @param field the site field the facet filters (required)
 * @param name  the facet name (required)
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
public record TurSNManifestRetiredCustomFacet(String field, String name) {
}
