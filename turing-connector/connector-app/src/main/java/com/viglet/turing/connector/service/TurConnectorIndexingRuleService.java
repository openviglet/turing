package com.viglet.turing.connector.service;

import com.viglet.turing.connector.commons.plugin.TurConnectorIndexingRuleType;
import com.viglet.turing.connector.commons.plugin.TurConnectorSession;
import com.viglet.turing.connector.persistence.model.TurConnectorIndexingRuleModel;
import com.viglet.turing.connector.persistence.repository.TurConnectorIndexingRuleRepository;
import com.viglet.turing.spring.utils.TurPersistenceUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class TurConnectorIndexingRuleService {
    private final TurConnectorIndexingRuleRepository turConnectorIndexingRuleRepository;

    @Autowired
    public TurConnectorIndexingRuleService(TurConnectorIndexingRuleRepository turConnectorIndexingRuleRepository) {
        this.turConnectorIndexingRuleRepository = turConnectorIndexingRuleRepository;
    }

    public Set<TurConnectorIndexingRuleModel> getIndexingRules(TurConnectorSession turConnectorSession) {
        return turConnectorIndexingRuleRepository
                .findBySourceAndRuleType(turConnectorSession.getSource(), TurConnectorIndexingRuleType.IGNORE);
    }

    public Set<TurConnectorIndexingRuleModel> getBySource(String source) {
        return turConnectorIndexingRuleRepository
                .findBySource(TurPersistenceUtils.orderByNameIgnoreCase(), source);
    }

    public List<TurConnectorIndexingRuleModel> getAll() {
        return turConnectorIndexingRuleRepository.findAll();
    }
    public Optional<TurConnectorIndexingRuleModel> getById(String id) {
        return turConnectorIndexingRuleRepository.findById(id);
    }

    public TurConnectorIndexingRuleModel update(String id, TurConnectorIndexingRuleModel turConnectorIndexingRule) {
        return turConnectorIndexingRuleRepository.findById(id).map(edit -> {
            edit.setName(turConnectorIndexingRule.getName());
            edit.setDescription(turConnectorIndexingRule.getDescription());
            edit.setAttribute(turConnectorIndexingRule.getAttribute());
            edit.setRuleType(turConnectorIndexingRule.getRuleType());
            edit.setSource(turConnectorIndexingRule.getSource());
            edit.setValues(turConnectorIndexingRule.getValues());
            edit.setLastModifiedDate(new Date());
            return turConnectorIndexingRuleRepository.save(edit);
        }).orElse(new TurConnectorIndexingRuleModel());
    }


    public void deleteById(String id) {
        turConnectorIndexingRuleRepository.deleteById(id);
    }

    public TurConnectorIndexingRuleModel save(TurConnectorIndexingRuleModel turConnectorIndexingRule) {
        return turConnectorIndexingRuleRepository.save(turConnectorIndexingRule);
    }

}
