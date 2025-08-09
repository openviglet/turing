package com.viglet.turing.connector.service;

import com.viglet.turing.client.sn.job.TurSNJobItem;
import com.viglet.turing.commons.indexing.TurIndexingStatus;
import com.viglet.turing.connector.domain.TurSNSiteLocale;
import com.viglet.turing.connector.commons.TurConnectorSession;
import com.viglet.turing.connector.commons.domain.TurConnectorIndexing;
import com.viglet.turing.connector.persistence.model.TurConnectorIndexingModel;
import com.viglet.turing.connector.persistence.repository.TurConnectorIndexingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TurConnectorIndexingService {
    private final TurConnectorIndexingRepository turConnectorIndexingRepository;

    @Autowired
    public TurConnectorIndexingService(TurConnectorIndexingRepository turConnectorIndexingRepository) {
        this.turConnectorIndexingRepository = turConnectorIndexingRepository;
    }

    public void delete(TurConnectorSession session, TurSNJobItem turSNJobItem) {
        turConnectorIndexingRepository.deleteByObjectIdAndSourceAndEnvironmentAndProvider(turSNJobItem.getId(),
                session.getSource(), turSNJobItem.getEnvironment(), session.getProviderName());
    }

    public void deleteByProvider(String provider) {
        turConnectorIndexingRepository.deleteByProvider(provider);
    }

    public void deleteContentsToBeDeIndexed(TurConnectorSession session) {
        turConnectorIndexingRepository.deleteBySourceAndProviderAndTransactionIdNot(session.getSource(),
                session.getProviderName(), session.getTransactionId());
    }

    public void update(TurSNJobItem turSNJobItem, TurConnectorSession session, boolean standalone,
                       TurConnectorIndexingModel indexing) {
        turConnectorIndexingRepository.save(updateTurConnectorIndexing(indexing,
                turSNJobItem, session, TurIndexingStatus.IGNORED, standalone));
    }
    public void update(TurSNJobItem turSNJobItem, TurConnectorSession session,
                       List<TurConnectorIndexingModel> turConnectorIndexingList,
                       TurIndexingStatus status, boolean standalone) {
        turConnectorIndexingList.forEach( indexing ->
                turConnectorIndexingRepository.save(updateTurConnectorIndexing(indexing,
                turSNJobItem, session, status, standalone)));

    }
    public void save(TurSNJobItem turSNJobItem, TurConnectorSession session, TurIndexingStatus status,
                     boolean standalone) {
        turConnectorIndexingRepository.save(createTurConnectorIndexing(turSNJobItem, session,
                status, standalone));
    }

    public boolean exists(TurSNJobItem turSNJobItem, TurConnectorSession session) {
        return turConnectorIndexingRepository.existsByObjectIdAndSourceAndEnvironmentAndProvider(turSNJobItem.getId(),
                session.getSource(), turSNJobItem.getEnvironment(), session.getProviderName());
    }

    public List<TurConnectorIndexingModel> getList(TurSNJobItem turSNJobItem, TurConnectorSession session) {
        return turConnectorIndexingRepository.findByObjectIdAndSourceAndEnvironmentAndProvider(turSNJobItem.getId(),
                session.getSource(), turSNJobItem.getEnvironment(), session.getProviderName());
    }

    public List<TurConnectorIndexingModel> getShouldBeDeIndexedList(TurConnectorSession session) {
        return turConnectorIndexingRepository.findBySourceAndProviderAndTransactionIdNotAndStandalone(session.getSource(),
                session.getProviderName(), session.getTransactionId(), false);
    }

    public List<TurConnectorIndexingModel>  findAll() {
        return turConnectorIndexingRepository.findAllByOrderByModificationDateDesc(Limit.of(50));
    }

    public List<String> getAllSources(String provider) {
        return turConnectorIndexingRepository.findAllSources(provider);
    }


    private static TurConnectorIndexingModel updateTurConnectorIndexing(TurConnectorIndexingModel turConnectorIndexing,
                                                                        TurSNJobItem turSNJobItem,
                                                                        TurConnectorSession turConnectorSession,
                                                                        TurIndexingStatus status,
                                                                        boolean standalone) {
        return turConnectorIndexing
                .setChecksum(turSNJobItem.getChecksum())
                .setTransactionId(turConnectorSession.getTransactionId())
                .setModificationDate(new Date())
                .setStatus(status)
                .setStandalone(standalone)
                .setSites(turSNJobItem.getSiteNames());
    }

    private TurConnectorIndexingModel createTurConnectorIndexing(TurSNJobItem turSNJobItem,
                                                                 TurConnectorSession turConnectorSession,
                                                                 TurIndexingStatus status,
                                                                 boolean standalone) {
        return TurConnectorIndexingModel.builder()
                .objectId(turSNJobItem.getId())
                .source(turConnectorSession.getSource())
                .transactionId(turConnectorSession.getTransactionId())
                .locale(turSNJobItem.getLocale())
                .checksum(turSNJobItem.getChecksum())
                .created(new Date())
                .modificationDate(new Date())
                .sites(turSNJobItem.getSiteNames())
                .environment(turSNJobItem.getEnvironment())
                .status(status)
                .standalone(standalone)
                .provider(turConnectorSession.getProviderName())
                .build();
    }
    public boolean isChecksumDifferent(TurSNJobItem turSNJobItem, TurConnectorSession session) {
        return turConnectorIndexingRepository.existsByObjectIdAndSourceAndEnvironmentAndChecksumNot(
                turSNJobItem.getId(), session.getSource(), turSNJobItem.getEnvironment(),
                turSNJobItem.getChecksum());
    }
    public  List<TurConnectorIndexingModel> getBySourceAndProvider(String source, String provider) {
        return turConnectorIndexingRepository.findAllBySourceAndProviderOrderByModificationDateDesc(source,
                provider, Limit.of(50));
    }

    public List<String> getSites(String source, String provider) {
        return turConnectorIndexingRepository.distinctSites(source, provider);
    }

    public List<String> getEnvironment(String site, String provider) {
        return turConnectorIndexingRepository.distinctEnvironment(site, provider);
    }

    public List<String> getObjectIdList(String source, String environment, TurSNSiteLocale siteLocale, String provider) {
        return turConnectorIndexingRepository
                .findAllObjectIds(source, siteLocale.getLanguage(),
                        environment, provider);
    }
    public Collection<String> validateObjectIdList(String source, String environment, TurSNSiteLocale siteLocale,
                                              String provider,
                                              List<String> objectIdList) {
        return turConnectorIndexingRepository
                .distinctObjectId(source, siteLocale.getLanguage(),
                        environment, provider, objectIdList);
    }

    public List<TurConnectorIndexing> getIndexingItem(String objectId, String source, String provider) {
        List<TurConnectorIndexing> dtoList = new ArrayList<>();
        turConnectorIndexingRepository.findByObjectIdAndSourceAndProvider(objectId, source, provider)
                .stream()
                        .map(indexing -> TurConnectorIndexing.builder()
                                .checksum(indexing.getChecksum())
                                .created(indexing.getCreated())
                                .environment(indexing.getEnvironment())
                                .id(indexing.getId())
                                .locale(indexing.getLocale())
                                .modificationDate(indexing.getModificationDate())
                                .source(indexing.getSource())
                                .objectId(indexing.getObjectId())
                                .sites(indexing.getSites())
                                .status(indexing.getStatus())
                                .transactionId(indexing.getTransactionId())
                                .build())
                        .forEach(dtoList::add);
        return dtoList;
    }

}
