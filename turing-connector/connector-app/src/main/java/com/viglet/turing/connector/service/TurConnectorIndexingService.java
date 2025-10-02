package com.viglet.turing.connector.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
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
import com.viglet.turing.connector.persistence.repository.TurConnectorIndexingRepository;

@Service
public class TurConnectorIndexingService {
    private final TurConnectorIndexingRepository turConnectorIndexingRepository;

    public TurConnectorIndexingService(
            TurConnectorIndexingRepository turConnectorIndexingRepository) {
        this.turConnectorIndexingRepository = turConnectorIndexingRepository;
    }

    public List<String> findByDependencies(String source, String provider,
            List<String> referenceIds) {
        return turConnectorIndexingRepository.findObjectIdsByDependencies(source, provider,
                referenceIds);
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

    public List<TurConnectorIndexingModel> findAllByProviderAndObjectIdIn(String provider,
            Collection<String> objectIds) {
        return turConnectorIndexingRepository.findAllByProviderAndObjectIdIn(provider, objectIds);
    }

    public void deleteByProviderAndSourceAndObjectIdIn(String provider, String source,
            Collection<String> objectIds) {
        turConnectorIndexingRepository.deleteByProviderAndSourceAndObjectIdIn(provider, source,
                objectIds);

    }

    public void deleteByProviderAndSource(String provider, String source) {
        turConnectorIndexingRepository.deleteByProviderAndSource(provider, source);
    }

    public void deleteContentsToBeDeIndexed(TurConnectorSession session) {
        turConnectorIndexingRepository.deleteBySourceAndProviderAndTransactionIdNot(
                session.getSource(), session.getProviderName(), session.getTransactionId());
    }

    public void update(TurJobItemWithSession turSNJobItemWithSession,
            TurConnectorIndexingModel indexing) {
        turConnectorIndexingRepository.save(updateTurConnectorIndexing(indexing,
                turSNJobItemWithSession, TurIndexingStatus.IGNORED));
    }

    public void update(TurJobItemWithSession turSNJobItemWithSession,
            List<TurConnectorIndexingModel> turConnectorIndexingList, TurIndexingStatus status) {
        List<TurConnectorIndexingModel> managedList = turConnectorIndexingList
                .stream().map(
                        indexing -> turConnectorIndexingRepository.findById(indexing.getId())
                                .map(managed -> updateTurConnectorIndexing(managed,
                                        turSNJobItemWithSession, status))
                                .orElse(null))
                .filter(Objects::nonNull).toList();
        turConnectorIndexingRepository.saveAll(managedList);
    }

    public void save(TurJobItemWithSession turSNJobItemWithSession, TurIndexingStatus status) {
        turConnectorIndexingRepository
                .save(createTurConnectorIndexing(turSNJobItemWithSession, status));
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
            TurConnectorIndexingModel turConnectorIndexing,
            TurJobItemWithSession turSNJobItemWithSession, TurIndexingStatus status) {
        turConnectorIndexing.setChecksum(turSNJobItemWithSession.turSNJobItem().getChecksum())
                .setTransactionId(turSNJobItemWithSession.session().getTransactionId())
                .setModificationDate(new Date()).setStatus(status)
                .setStandalone(turSNJobItemWithSession.standalone())
                .setSites(turSNJobItemWithSession.turSNJobItem().getSiteNames())
                .setDependencies(new HashSet<TurConnectorDependencyModel>(turSNJobItemWithSession
                        .dependencies().stream().map(dep -> TurConnectorDependencyModel.builder()
                                .objectId(dep).reference(turConnectorIndexing).build())
                        .toList()));
        return turConnectorIndexing;
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
        turConnectorIndexingModel
                .setDependencies(new HashSet<>(turSNJobItemWithSession
                        .dependencies().stream().map(dep -> TurConnectorDependencyModel.builder()
                                .objectId(dep).reference(turConnectorIndexingModel).build())
                        .toList()));
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
                .map(indexing -> getConnectorIndexing(indexing)).forEach(dtoList::add);
        return dtoList;
    }

    public TurConnectorIndexing getConnectorIndexing(TurConnectorIndexingModel indexing) {
        return TurConnectorIndexing.builder().checksum(indexing.getChecksum())
                .created(indexing.getCreated()).environment(indexing.getEnvironment())
                .id(indexing.getId()).locale(indexing.getLocale())
                .modificationDate(indexing.getModificationDate()).source(indexing.getSource())
                .objectId(indexing.getObjectId()).sites(indexing.getSites())
                .status(indexing.getStatus()).transactionId(indexing.getTransactionId()).build();
    }
}
