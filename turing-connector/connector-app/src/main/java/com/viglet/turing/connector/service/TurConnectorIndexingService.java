package com.viglet.turing.connector.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import com.viglet.turing.client.sn.job.TurSNJobItem;
import com.viglet.turing.commons.indexing.TurIndexingStatus;
import com.viglet.turing.connector.commons.TurConnectorSession;
import com.viglet.turing.connector.commons.domain.TurConnectorIndexing;
import com.viglet.turing.connector.commons.domain.TurJobItemWithSession;
import com.viglet.turing.connector.domain.TurSNSiteLocale;
import com.viglet.turing.connector.persistence.model.TurConnectorDependencyModel;
import com.viglet.turing.connector.persistence.model.TurConnectorIndexingModel;
import com.viglet.turing.connector.persistence.repository.TurConnectorDependencyRepository;
import com.viglet.turing.connector.persistence.repository.TurConnectorIndexingRepository;

@Service
public class TurConnectorIndexingService {
    private final TurConnectorIndexingRepository turConnectorIndexingRepository;
    private final TurConnectorDependencyRepository turConnectorDependencyRepository;

    public TurConnectorIndexingService(
            TurConnectorIndexingRepository turConnectorIndexingRepository,
            TurConnectorDependencyRepository turConnectorDependencyRepository) {
        this.turConnectorIndexingRepository = turConnectorIndexingRepository;
        this.turConnectorDependencyRepository = turConnectorDependencyRepository;
    }

    public void delete(TurJobItemWithSession turSNJobItemWithSession) {
        TurSNJobItem turSNJobItem = turSNJobItemWithSession.turSNJobItem();
        TurConnectorSession session = turSNJobItemWithSession.session();
        turConnectorIndexingRepository.deleteByObjectIdAndSourceAndEnvironmentAndProvider(
                turSNJobItem.getId(), session.getSource(), turSNJobItem.getEnvironment(),
                session.getProviderName());

    }

    public void deleteByProvider(String provider) {
        turConnectorIndexingRepository.deleteByProvider(provider);
    }

    public void deleteContentsToBeDeIndexed(TurConnectorSession session) {
        turConnectorIndexingRepository.deleteBySourceAndProviderAndTransactionIdNot(
                session.getSource(), session.getProviderName(), session.getTransactionId());
    }

    public void update(TurJobItemWithSession turSNJobItemWithSession,
            TurConnectorIndexingModel indexing) {
        TurConnectorIndexingModel turConnectorIndexingModel =
                turConnectorIndexingRepository.save(updateTurConnectorIndexing(indexing,
                        turSNJobItemWithSession.turSNJobItem(), turSNJobItemWithSession.session(),
                        TurIndexingStatus.IGNORED, turSNJobItemWithSession.standalone()));
        updateDependencies(turSNJobItemWithSession, turConnectorIndexingModel);
    }

    private void updateDependencies(TurJobItemWithSession turSNJobItemWithSession,
            TurConnectorIndexingModel turConnectorIndexingModel) {
        deleteDependencies(turConnectorIndexingModel);
        saveDependencies(turSNJobItemWithSession, turConnectorIndexingModel);
    }

    public void update(TurJobItemWithSession turSNJobItemWithSession,
            List<TurConnectorIndexingModel> turConnectorIndexingList, TurIndexingStatus status) {
        turConnectorIndexingList.forEach(indexing -> {
            turConnectorIndexingRepository.findById(indexing.getId()).ifPresent(managedIndexing -> {
                TurConnectorIndexingModel turConnectorIndexingModel =
                        turConnectorIndexingRepository.save(updateTurConnectorIndexing(
                                managedIndexing, turSNJobItemWithSession.turSNJobItem(),
                                turSNJobItemWithSession.session(), status,
                                turSNJobItemWithSession.standalone()));
                updateDependencies(turSNJobItemWithSession, turConnectorIndexingModel);
            });
        });
    }

    public void save(TurJobItemWithSession turSNJobItemWithSession, TurIndexingStatus status) {
        TurConnectorIndexingModel turConnectorIndexingModel = turConnectorIndexingRepository
                .save(createTurConnectorIndexing(turSNJobItemWithSession, status));
        saveDependencies(turSNJobItemWithSession, turConnectorIndexingModel);
    }

    private void deleteDependencies(TurConnectorIndexingModel turConnectorIndexingModel) {
        turConnectorDependencyRepository.deleteAllByReferenceId(turConnectorIndexingModel.getId());
    }

    private void saveDependencies(TurJobItemWithSession turSNJobItemWithSession,
            TurConnectorIndexingModel turConnectorIndexingModel) {
        turConnectorDependencyRepository
                .saveAll(turSNJobItemWithSession.dependencies().stream().map(dependency -> {
                    return TurConnectorDependencyModel.builder().objectId(dependency)
                            .reference(turConnectorIndexingModel).build();
                }).toList());
    }

    public boolean exists(TurJobItemWithSession turSNJobItemWithSession) {
        TurSNJobItem turSNJobItem = turSNJobItemWithSession.turSNJobItem();
        TurConnectorSession session = turSNJobItemWithSession.session();
        return turConnectorIndexingRepository.existsByObjectIdAndSourceAndEnvironmentAndProvider(
                turSNJobItem.getId(), session.getSource(), turSNJobItem.getEnvironment(),
                session.getProviderName());
    }

    public List<TurConnectorIndexingModel> getList(TurJobItemWithSession turSNJobItemWithSession) {
        TurSNJobItem turSNJobItem = turSNJobItemWithSession.turSNJobItem();
        TurConnectorSession session = turSNJobItemWithSession.session();
        return turConnectorIndexingRepository.findByObjectIdAndSourceAndEnvironmentAndProvider(
                turSNJobItem.getId(), session.getSource(), turSNJobItem.getEnvironment(),
                session.getProviderName());
    }

    public List<TurConnectorIndexingModel> getShouldBeDeIndexedList(TurConnectorSession session) {
        return turConnectorIndexingRepository
                .findBySourceAndProviderAndTransactionIdNotAndStandalone(session.getSource(),
                        session.getProviderName(), session.getTransactionId(), false);
    }

    public List<TurConnectorIndexingModel> findAll() {
        return turConnectorIndexingRepository.findAllByOrderByModificationDateDesc(Limit.of(50));
    }

    public List<String> getAllSources(String provider) {
        return turConnectorIndexingRepository.findAllSources(provider);
    }

    private static TurConnectorIndexingModel updateTurConnectorIndexing(
            TurConnectorIndexingModel turConnectorIndexing, TurSNJobItem turSNJobItem,
            TurConnectorSession turConnectorSession, TurIndexingStatus status, boolean standalone) {
        return turConnectorIndexing.setChecksum(turSNJobItem.getChecksum())
                .setTransactionId(turConnectorSession.getTransactionId())
                .setModificationDate(new Date()).setStatus(status).setStandalone(standalone)
                .setSites(turSNJobItem.getSiteNames());
    }

    private TurConnectorIndexingModel createTurConnectorIndexing(
            TurJobItemWithSession turSNJobItemWithSession, TurIndexingStatus status) {
        TurSNJobItem turSNJobItem = turSNJobItemWithSession.turSNJobItem();
        TurConnectorSession turConnectorSession = turSNJobItemWithSession.session();
        TurConnectorIndexingModel turConnectorIndexingModel = TurConnectorIndexingModel.builder()
                .objectId(turSNJobItem.getId()).source(turConnectorSession.getSource())
                .transactionId(turConnectorSession.getTransactionId())
                .locale(turSNJobItem.getLocale()).checksum(turSNJobItem.getChecksum())
                .created(new Date()).modificationDate(new Date()).sites(turSNJobItem.getSiteNames())
                .environment(turSNJobItem.getEnvironment()).status(status)
                .standalone(turSNJobItemWithSession.standalone())
                .provider(turConnectorSession.getProviderName()).build();

        return turConnectorIndexingModel;
    }

    public boolean isChecksumDifferent(TurJobItemWithSession turSNJobItemWithSession) {
        TurSNJobItem turSNJobItem = turSNJobItemWithSession.turSNJobItem();
        TurConnectorSession session = turSNJobItemWithSession.session();
        return turConnectorIndexingRepository.existsByObjectIdAndSourceAndEnvironmentAndChecksumNot(
                turSNJobItem.getId(), session.getSource(), turSNJobItem.getEnvironment(),
                turSNJobItem.getChecksum());
    }

    public List<TurConnectorIndexingModel> getBySourceAndProvider(String source, String provider) {
        return turConnectorIndexingRepository.findAllBySourceAndProviderOrderByModificationDateDesc(
                source, provider, Limit.of(50));
    }

    public List<String> getSites(String source, String provider) {
        return turConnectorIndexingRepository.distinctSites(source, provider);
    }

    public List<String> getEnvironment(String site, String provider) {
        return turConnectorIndexingRepository.distinctEnvironment(site, provider);
    }

    public List<String> getObjectIdList(String source, String environment,
            TurSNSiteLocale siteLocale, String provider) {
        return turConnectorIndexingRepository.findAllObjectIds(source, siteLocale.getLanguage(),
                environment, provider);
    }

    public Collection<String> validateObjectIdList(String source, String environment,
            TurSNSiteLocale siteLocale, String provider, List<String> objectIdList) {
        return turConnectorIndexingRepository.distinctObjectId(source, siteLocale.getLanguage(),
                environment, provider, objectIdList);
    }

    public List<TurConnectorIndexing> getIndexingItem(String objectId, String source,
            String provider) {
        List<TurConnectorIndexing> dtoList = new ArrayList<>();
        turConnectorIndexingRepository
                .findByObjectIdAndSourceAndProvider(objectId, source, provider).stream()
                .map(indexing -> TurConnectorIndexing.builder().checksum(indexing.getChecksum())
                        .created(indexing.getCreated()).environment(indexing.getEnvironment())
                        .id(indexing.getId()).locale(indexing.getLocale())
                        .modificationDate(indexing.getModificationDate())
                        .source(indexing.getSource()).objectId(indexing.getObjectId())
                        .sites(indexing.getSites()).status(indexing.getStatus())
                        .transactionId(indexing.getTransactionId()).build())
                .forEach(dtoList::add);
        return dtoList;
    }
}
