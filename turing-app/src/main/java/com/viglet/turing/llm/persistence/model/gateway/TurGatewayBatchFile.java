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
package com.viglet.turing.llm.persistence.model.gateway;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

import com.viglet.core.jpa.VigletAssignableUuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * T1341 — a JSONL file a gateway client uploaded with {@code POST /v1/files}
 * ({@code purpose=batch}), kept until a {@code POST /v1/batches} names it. Only
 * input files are stored: a batch's output is read back from the vendor when the
 * client asks for it. Owned by the gateway key that uploaded it ({@code keyId},
 * {@code null} in open mode); another key cannot read or batch it.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@Getter
@Setter
@Entity
@Table(name = "llm_gateway_batch_file")
public class TurGatewayBatchFile implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @VigletAssignableUuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    @Column(name = "keyId", length = 36)
    private String keyId;

    @Column(name = "filename", length = 255)
    private String filename;

    @Column(name = "purpose", nullable = false, length = 32)
    private String purpose;

    @Column(name = "bytes", nullable = false)
    private long bytes;

    @Column(name = "lineCount", nullable = false)
    private int lineCount;

    @Column(name = "content", nullable = false, columnDefinition = "longtext")
    private String content;

    @Column(name = "createdAt", nullable = false)
    private Instant createdAt = Instant.now();
}
