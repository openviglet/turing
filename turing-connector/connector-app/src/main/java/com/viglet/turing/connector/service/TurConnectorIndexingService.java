package com.viglet.turing.connector.service;

import com.viglet.turing.client.sn.job.TurSNJobItem;
import com.viglet.turing.commons.indexing.TurIndexingStatus;
import com.viglet.turing.connector.domain.TurSNSiteLocale;
import com.viglet.turing.connector.commons.plugin.TurConnectorSession;
import com.viglet.turing.connector.commons.plugin.domain.TurConnectorIndexing;
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
        turConnectorIndexingRepository.deleteByObjectIdAndSourceAndEnvironment(turSNJobItem.getId(),
                session.getSource(), turSNJobItem.getEnvironment());
    }


    public void update(TurSNJobItem turSNJobItem, TurConnectorSession session, boolean standalone,
                       TurConnectorIndexingModel indexing) {
        turConnectorIndexingRepository.save(updateTurConnectorIndexing(indexing,
                turSNJobItem, session, TurIndexingStatus.IGNORED, standalone));
    }
    public void update(TurSNJobItem turSNJobItem, TurConnectorSession session,
                       List<TurConnectorIndexingModel> turConnectorIndexingList,
                       TurIndexingStatus status, boolean standalone) {
        turConnectorIndexingRepository.save(updateTurConnectorIndexing(turConnectorIndexingList.getFirst(),
                turSNJobItem, session, status, standalone));
    }
    public void save(TurSNJobItem turSNJobItem, TurConnectorSession session, TurIndexingStatus status, boolean standalone) {
        turConnectorIndexingRepository.save(createTurConnectorIndexing(turSNJobItem, session,
                status, standalone));
    }

    public boolean exists(TurSNJobItem turSNJobItem, TurConnectorSession session) {
        return turConnectorIndexingRepository.existsByObjectIdAndSourceAndEnvironment(turSNJobItem.getId(),
                session.getSource(), turSNJobItem.getEnvironment());
    }

    public Optional<List<TurConnectorIndexingModel>> getList(TurSNJobItem turSNJobItem, TurConnectorSession session) {
        return turConnectorIndexingRepository.findByObjectIdAndSourceAndEnvironment(turSNJobItem.getId(),
                session.getSource(), turSNJobItem.getEnvironment());
    }

    public List<TurConnectorIndexingModel> getShouldBeDeIndexedList(TurConnectorSession session) {
        return turConnectorIndexingRepository.findContentsShouldBeDeIndexed(session.getSource(),
                session.getTransactionId());
    }

    public Optional<List<TurConnectorIndexingModel>>  findAll() {
        return turConnectorIndexingRepository.findAllByOrderByModificationDateDesc(Limit.of(50));
    }

    public List<String> getAllSources() {
        return turConnectorIndexingRepository.findAllSources();
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
                .build();
    }
    public boolean isChecksumDifferent(TurSNJobItem turSNJobItem, TurConnectorSession session) {
        return turConnectorIndexingRepository.existsByObjectIdAndSourceAndEnvironmentAndChecksumNot(
                turSNJobItem.getId(), session.getSource(), turSNJobItem.getEnvironment(),
                turSNJobItem.getChecksum());
    }
    public  Optional<List<TurConnectorIndexingModel>> getBySource(String source) {
        return turConnectorIndexingRepository.findAllBySourceOrderByModificationDateDesc(source, Limit.of(50));
    }

    public List<String> getSitesBySource(String source) {
        return turConnectorIndexingRepository.distinctSitesBySource(source);
    }

    public List<String> getEnvironmentBySite(String site) {
        return turConnectorIndexingRepository.distinctEnvironmentBySite(site);
    }

    public List<String> getObjectIdList(String source, String environment, TurSNSiteLocale siteLocale) {
        return turConnectorIndexingRepository
                .findAllObjectIdsBySourceAndLocaleAndEnvironment(source, siteLocale.getLanguage(),
                        environment);
    }
    public Collection<String> validateObjectIdList(String source, String environment, TurSNSiteLocale siteLocale,
                                              List<String> objectIdList) {
        return turConnectorIndexingRepository
                .distinctObjectIdBySourceAndLocaleAndEnvironmentAndIdIn(source, siteLocale.getLanguage(),
                        environment, objectIdList);
    }

    public List<TurConnectorIndexing> getIndexingItem(String objectId, String source) {
        List<TurConnectorIndexing> dtoList = new ArrayList<>();
        turConnectorIndexingRepository.findByObjectIdAndSource(objectId, source)
                .ifPresent(indexingList -> indexingList.stream()
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
                        .forEach(dtoList::add));
        return dtoList;
    }

}
