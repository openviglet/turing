/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.viglet.turing.searchengine.spi;

import com.viglet.turing.searchengine.TurSNSiteCustomSortMissingEnum;
import com.viglet.turing.searchengine.TurSNSiteCustomSortOrderEnum;

/**
 * T1310 — one level of a site's custom sort, as the engines read it.
 *
 * @param fieldName the field to sort on
 * @param order     ascending or descending
 * @param missing   where documents without the field go; {@code null} leaves the
 *                  engine's own placement (T1104)
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
public record TurSECustomSortItem(String fieldName, TurSNSiteCustomSortOrderEnum order,
        TurSNSiteCustomSortMissingEnum missing) {

    /** Whether this level sorts descending. */
    public boolean descending() {
        return order == TurSNSiteCustomSortOrderEnum.DESC;
    }
}
