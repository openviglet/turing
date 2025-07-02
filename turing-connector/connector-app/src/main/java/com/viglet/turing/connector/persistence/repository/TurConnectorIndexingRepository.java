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

import com.viglet.turing.connector.persistence.model.TurConnectorIndexingModel;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public interface TurConnectorIndexingRepository extends JpaRepository<TurConnectorIndexingModel, String> {

    List<TurConnectorIndexingModel> findBySourceAndTransactionIdNotAndStandalone(String source,
                                                                                 String transactionId,
                                                                                 boolean standalone);

    default List<TurConnectorIndexingModel> findContentsShouldBeDeIndexed(String source, String transactionId) {
        return findBySourceAndTransactionIdNotAndStandalone(source, transactionId, false);
    }

    boolean existsByObjectIdAndSourceAndEnvironment(String objectId, String source, String environment);

    boolean existsByObjectIdAndSourceAndEnvironmentAndChecksumNot(String objectId, String source, String environment,
                                                                String checksum);

    Optional<List<TurConnectorIndexingModel>> findByObjectIdAndSourceAndEnvironment(String objectId, String source,
                                                                                    String environment);

    Optional<List<TurConnectorIndexingModel>> findByObjectIdAndSource(String objectId, String source);

    @Transactional
    void deleteByObjectIdAndSourceAndEnvironment(String objectId, String source, String environment);

    @Transactional
    void deleteBySourceAndTransactionIdNot(String source, String transactionId);

    Optional<List<TurConnectorIndexingModel>> findAllBySourceOrderByModificationDateDesc(String source, Limit limit);
    Optional<List<TurConnectorIndexingModel>>  findAllByOrderByModificationDateDesc(Limit limit);

    @Transactional
    default void deleteContentsWereDeIndexed(String source, String deltaId) {
        deleteBySourceAndTransactionIdNot(source, deltaId);
    }

    @Query("SELECT DISTINCT i.source FROM TurConnectorIndexingModel i")
    List<String> findAllSources();

    @Query("SELECT DISTINCT i.objectId FROM TurConnectorIndexingModel i WHERE i.source = :source AND i.locale = :locale AND i.environment IN :environment")
    List<String> findAllObjectIdsBySourceAndLocaleAndEnvironment(@Param("source") String source,
                                                             @Param("locale") Locale locale,
                                                             @Param("environment") String environment);

    @Query("SELECT DISTINCT i.sites FROM TurConnectorIndexingModel i WHERE i.source = :source")
    List<String> distinctSitesBySource(@Param("source") String source);

    @Query("SELECT DISTINCT i.environment FROM TurConnectorIndexingModel i WHERE :site MEMBER OF i.sites")
    List<String> distinctEnvironmentBySite(@Param("site") String site);

    @Query("SELECT DISTINCT i.objectId FROM TurConnectorIndexingModel i WHERE i.source = :source AND i.locale = :locale AND " +
            "i.environment IN :environment AND i.objectId IN :ids" )
    List<String> distinctObjectIdBySourceAndLocaleAndEnvironmentAndIdIn(@Param("source") String source,
                                                          @Param("locale") Locale locale,
                                                          @Param("environment") String environment,
                                                          @Param("ids") List<String> ids);

}
