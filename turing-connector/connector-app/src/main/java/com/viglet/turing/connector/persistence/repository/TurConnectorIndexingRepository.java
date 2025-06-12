/*
 *
 * Copyright (C) 2016-2024 the original author or authors.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.viglet.turing.connector.persistence.repository;

import com.viglet.turing.connector.persistence.model.TurConnectorIndexing;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface TurConnectorIndexingRepository extends JpaRepository<TurConnectorIndexing, String> {

    Optional<List<TurConnectorIndexing>> findBySourceAndTransactionIdNot(String source, String transactionId);

    default Optional<List<TurConnectorIndexing>> findContentsShouldBeDeIndexed(String source, String transactionId) {
        return findBySourceAndTransactionIdNot(source, transactionId);
    }

    boolean existsByObjectIdAndSourceAndEnvironment(String objectId, String source, String environment);

    boolean existsByObjectIdAndSourceAndEnvironmentAndChecksumNot(String objectId, String source, String environment,
                                                                String checksum);

    Optional<List<TurConnectorIndexing>> findByObjectIdAndSourceAndEnvironment(String objectId, String source,
                                                                             String environment);

    Optional<List<TurConnectorIndexing>> findByObjectIdAndSource(String objectId, String source);
    void deleteByObjectIdAndSourceAndEnvironment(String objectId, String source, String environment);

    void deleteBySourceAndTransactionIdNot(String source, String transactionId);

    Optional<List<TurConnectorIndexing>> findAllBySourceOrderByModificationDateDesc(String source, Limit limit);
    Optional<List<TurConnectorIndexing>>  findAllByOrderByModificationDateDesc(Limit limit);

    @Transactional
    void deleteBySourceAndObjectId(String name, String objectId);

    @Transactional
    default void deleteContentsWereDeIndexed(String source, String deltaId) {
        deleteBySourceAndTransactionIdNot(source, deltaId);
    }

    @Query("SELECT DISTINCT i.source FROM TurConnectorIndexing i")
    List<String> findAllSources();
}
