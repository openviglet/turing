/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.shared.exception;

/**
 * T1319 — the optimistic-lock check every console save runs before it applies:
 * a save that names a version other than the stored one was made from a copy
 * someone else has changed since, and is refused with a 409 instead of
 * overwriting that change. A save that names no version (an older client)
 * passes, as it did before the aggregate had one.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
public final class TurStaleVersion {

    private TurStaleVersion() {
    }

    /**
     * Throws {@link TurConflictException} when {@code incoming} and
     * {@code stored} are both set and differ.
     *
     * @param what how the message names the record, e.g. "LLM instance"
     */
    public static void refuse(Long incoming, Long stored, String what) {
        if (incoming != null && stored != null && !incoming.equals(stored)) {
            throw new TurConflictException("This " + what
                    + " was changed by someone else since you opened it. Reload it and try again.");
        }
    }
}
