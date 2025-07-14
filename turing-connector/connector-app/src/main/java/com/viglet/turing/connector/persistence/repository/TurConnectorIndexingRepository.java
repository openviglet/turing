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


    boolean existsByObjectIdAndSourceAndEnvironmentAndProvider(String objectId, String source, String environment,
                                                               String provider);

    boolean existsByObjectIdAndSourceAndEnvironmentAndChecksumNot(String objectId, String source, String environment,
                                                                  String checksum);

    List<TurConnectorIndexingModel> findBySourceAndProviderAndTransactionIdNotAndStandalone(String source,
                                                                                 String provider,
                                                                                 String transactionId,
                                                                                 boolean standalone);

    List<TurConnectorIndexingModel> findByObjectIdAndSourceAndEnvironmentAndProvider(String objectId, String source,
                                                                                     String environment, String provider);

    List<TurConnectorIndexingModel> findByObjectIdAndSourceAndProvider(String objectId, String source, String provider);

    List<TurConnectorIndexingModel> findAllBySourceAndProviderOrderByModificationDateDesc(String source,
                                                                                          String provider, Limit limit);

    List<TurConnectorIndexingModel> findAllByOrderByModificationDateDesc(Limit limit);

    @Transactional
    void deleteByObjectIdAndSourceAndEnvironmentAndProvider(String objectId, String source, String environment,
                                                            String provider);

    @Transactional
    void deleteByProvider(String provider);

    @Transactional
    void deleteBySourceAndProviderAndTransactionIdNot(String source, String provider, String transactionId);

    @Query("SELECT DISTINCT i.source FROM TurConnectorIndexingModel i WHERE i.provider = :provider")
    List<String> findAllSources( @Param("provider") String provider);

    @Query("SELECT DISTINCT i.objectId FROM TurConnectorIndexingModel i WHERE i.source = :source AND " +
            "i.locale = :locale AND i.environment IN :environment AND i.provider = :provider")
    List<String> findAllObjectIds(@Param("source") String source,
                                                                 @Param("locale") Locale locale,
                                                                 @Param("environment") String environment,
                                                                 @Param("provider") String provider);

    @Query("SELECT DISTINCT i.sites FROM TurConnectorIndexingModel i WHERE i.source = :source AND " +
            "i.provider = :provider")
    List<String> distinctSites(@Param("source") String source,
                                                  @Param("provider") String provider);

    @Query("SELECT DISTINCT i.environment FROM TurConnectorIndexingModel i WHERE :site MEMBER OF i.sites AND " +
            "i.provider = :provider")
    List<String> distinctEnvironment(@Param("site") String site,
                                                      @Param("provider") String provider);

    @Query("SELECT DISTINCT i.objectId FROM TurConnectorIndexingModel i WHERE i.source = :source AND i.locale = :locale AND " +
            "i.environment IN :environment AND i.provider = :provider AND i.objectId IN :ids")
    List<String> distinctObjectId(@Param("source") String source,
                                                                        @Param("locale") Locale locale,
                                                                        @Param("environment") String environment,
                                                                                   @Param("provider") String provider,
                                                                        @Param("ids") List<String> ids);


}
