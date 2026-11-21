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
package com.viglet.turing.sn.kb;

import java.util.Locale;

import com.viglet.turing.sn.persistence.model.TurSNSite;

/**
 * T1322i — the recognition dictionary a site's entity indexing reads, declared
 * by sn and implemented by kb, which owns the thesauri it is compiled from (ADR
 * 0005 section 6: kb sits above sn).
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
public interface TurSNRecognitionDictionaries {

    /**
     * The compiled dictionary of the thesauri {@code site} has enabled for
     * {@code locale}; empty when it has none.
     */
    TurRecognitionDictionary getDictionary(TurSNSite site, Locale locale);
}
