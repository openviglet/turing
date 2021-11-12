/*
 * Copyright (C) 2016-2021 the original author or authors. 
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.viglet.turing.api.sn.queue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import com.viglet.turing.api.sn.job.TurSNJob;
import com.viglet.turing.api.sn.job.TurSNJobAction;
import com.viglet.turing.api.sn.job.TurSNJobItem;
import com.viglet.turing.nlp.TurNLP;
import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.model.sn.TurSNSiteFieldExt;
import com.viglet.turing.persistence.model.sn.locale.TurSNSiteLocale;
import com.viglet.turing.persistence.repository.sn.TurSNSiteFieldExtRepository;
import com.viglet.turing.persistence.repository.sn.TurSNSiteRepository;
import com.viglet.turing.persistence.repository.sn.locale.TurSNSiteLocaleRepository;
import com.viglet.turing.solr.TurSolr;
import com.viglet.turing.solr.TurSolrInstance;
import com.viglet.turing.solr.TurSolrInstanceProcess;
import com.viglet.turing.thesaurus.TurThesaurusProcessor;

@Component
public class TurSNProcessQueue {
	private static final Logger logger = LogManager.getLogger(TurSNProcessQueue.class);
	@Autowired
	private TurSolr turSolr;
	@Autowired
	private TurSNSiteRepository turSNSiteRepository;
	@Autowired
	private TurSNSiteLocaleRepository turSNSiteLocaleRepository;
	@Autowired
	private TurNLP turNLP;
	@Autowired
	private TurThesaurusProcessor turThesaurusProcessor;
	@Autowired
	private TurSNSiteFieldExtRepository turSNSiteFieldExtRepository;
	@Autowired
	private TurSolrInstanceProcess turSolrInstanceProcess;

	public static final String INDEXING_QUEUE = "indexing.queue";

	@JmsListener(destination = INDEXING_QUEUE)
	public void receiveIndexingQueue(TurSNJob turSNJob) {

		this.turSNSiteRepository.findById(turSNJob.getSiteId()).ifPresent(turSNSite -> {
			for (TurSNJobItem turSNJobItem : turSNJob.getTurSNJobItems()) {
				logger.debug("receiveQueue TurSNJobItem: {}", turSNJobItem.toString());
				if (turSNJobItem.getTurSNJobAction().equals(TurSNJobAction.CREATE)) {
					this.indexing(turSNJobItem, turSNSite);

				} else if (turSNJobItem.getTurSNJobAction().equals(TurSNJobAction.DELETE)) {
					this.desindexing(turSNJobItem, turSNSite);
				}

				processQueueInfo(turSNSite, turSNJobItem);
			}
		});

	}

	private void processQueueInfo(TurSNSite turSNSite, TurSNJobItem turSNJobItem) {
		if (turSNSite != null && turSNJobItem != null && turSNJobItem.getAttributes() != null
				&& turSNJobItem.getAttributes().containsKey("id")) {
			String action = null;
			if (turSNJobItem.getTurSNJobAction().equals(TurSNJobAction.CREATE)) {
				action = "Indexed";
			} else if (turSNJobItem.getTurSNJobAction().equals(TurSNJobAction.DELETE)) {
				action = "Deindexed";
			}
			logger.info("{} the Object ID '{}' of '{}' SN Site.", action, turSNJobItem.getAttributes().get("id"),
					turSNSite.getName());

		}
	}

	public void desindexing(TurSNJobItem turSNJobItem, TurSNSite turSNSite) {
		logger.debug("Deindexing");
		TurSolrInstance turSolrInstance = turSolrInstanceProcess.initSolrInstance(turSNSite, turSNJobItem.getLocale());
		if (turSNJobItem.getAttributes().containsKey("id")) {
			turSolr.desindexing(turSolrInstance, (String) turSNJobItem.getAttributes().get("id"));
		} else if (turSNJobItem.getAttributes().containsKey("type")) {
			turSolr.desindexingByType(turSolrInstance, (String) turSNJobItem.getAttributes().get("type"));
		}
	}

	public void indexing(TurSNJobItem turSNJobItem, TurSNSite turSNSite) {
		logger.debug("Indexing");
		Map<String, Object> consolidateResults = new HashMap<>();

		// SE
		for (Entry<String, Object> attribute : turSNJobItem.getAttributes().entrySet()) {
			if (logger.isDebugEnabled())
				logger.debug("SE Consolidate Value: {}", attribute.getValue());
			if (attribute.getValue() != null) {
				if (logger.isDebugEnabled())
					logger.debug("SE Consolidate Class: {}", attribute.getValue().getClass().getName());
				consolidateResults.put(attribute.getKey(), attribute.getValue());
			}
		}

		// NLP
		boolean nlp = true;
		TurSNSiteLocale turSNSiteLocale = turSNSiteLocaleRepository.findByTurSNSiteAndLanguage(turSNSite,
				turSNJobItem.getLocale());

		if (turSNSiteLocale != null && turSNSiteLocale.getTurNLPInstance() != null) {
			if (logger.isDebugEnabled())
				logger.debug("It is using NLP to process attributes");
			nlp = true;
		} else {
			if (logger.isDebugEnabled())
				logger.debug("It is not using NLP to process attributes");
			nlp = false;
		}

		if (nlp) {
			List<TurSNSiteFieldExt> turSNSiteFieldsExt = turSNSiteFieldExtRepository
					.findByTurSNSiteAndNlpAndEnabled(turSNSite, 1, 1);

			// Convert List to HashMap
			Map<String, TurSNSiteFieldExt> turSNSiteFieldsExtMap = new HashMap<>();
			for (TurSNSiteFieldExt turSNSiteFieldExt : turSNSiteFieldsExt) {
				turSNSiteFieldsExtMap.put(turSNSiteFieldExt.getName().toLowerCase(), turSNSiteFieldExt);
			}

			// Select only fields that is checked as NLP. These attributes will be processed
			// by NLP
			HashMap<String, Object> nlpAttributes = new HashMap<>();
			for (Entry<String, Object> attribute : turSNJobItem.getAttributes().entrySet()) {
				if (turSNSiteFieldsExtMap.containsKey(attribute.getKey().toLowerCase())) {
					nlpAttributes.put(attribute.getKey(), attribute.getValue());
				}
			}

			turNLP.startup(turSNSiteLocale.getTurNLPInstance(), nlpAttributes);
			Map<String, Object> nlpResultsPreffix = new HashMap<>();

			// Copy NLP attributes to consolidateResults
			for (Entry<String, Object> nlpResultPreffix : nlpResultsPreffix.entrySet()) {
				consolidateResults.put(nlpResultPreffix.getKey(), nlpResultPreffix.getValue());
			}
		}

		// Thesaurus
		boolean thesaurus = false;
		if (turSNSite.getThesaurus() < 1) {
			logger.debug("It is not using Thesaurus to process attributes");
			thesaurus = false;
		} else {
			logger.debug("It is using Thesaurus to process attributes");
			thesaurus = true;
		}
		if (thesaurus) {
			turThesaurusProcessor.startup();
			Map<String, Object> thesaurusResults = turThesaurusProcessor.detectTerms(turSNJobItem.getAttributes());

			logger.debug("thesaurusResults.size(): {}", thesaurusResults.size());
			for (Entry<String, Object> thesaurusResult : thesaurusResults.entrySet()) {
				logger.debug("thesaurusResult Key: {}", thesaurusResult.getKey());
				logger.debug("thesaurusResult Value: {}", thesaurusResult.getValue());
				consolidateResults.put(thesaurusResult.getKey(), thesaurusResult.getValue());
			}
		}

		// Remove Duplicate Terms
		Map<String, Object> attributesWithUniqueTerms = this.removeDuplicateTerms(consolidateResults);

		// SE
		TurSolrInstance turSolrInstance = turSolrInstanceProcess.initSolrInstance(turSNSite, turSNJobItem.getLocale());
		turSolr.indexing(turSolrInstance, turSNSite, attributesWithUniqueTerms);
	}

	public Map<String, Object> removeDuplicateTerms(Map<String, Object> attributes) {
		Map<String, Object> attributesWithUniqueTerms = new HashMap<String, Object>();
		if (attributes != null) {
			for (Entry<String, Object> attribute : attributes.entrySet()) {
				if (attribute.getValue() != null) {

					logger.debug("removeDuplicateTerms: attribute Value: {}", attribute.getValue().toString());
					logger.debug("removeDuplicateTerms: attribute Class: {}",
							attribute.getValue().getClass().getName());
					if (attribute.getValue() instanceof ArrayList) {

						ArrayList<?> nlpAttributeArray = (ArrayList<?>) attribute.getValue();
						if (!nlpAttributeArray.isEmpty()) {
							List<String> list = new ArrayList<>();
							for (Object nlpAttributeItem : nlpAttributeArray) {
								list.add((String) nlpAttributeItem);
							}
							Set<String> termsUnique = new HashSet<>(list);
							List<Object> arrayValue = new ArrayList<>();
							arrayValue.addAll(termsUnique);
							attributesWithUniqueTerms.put(attribute.getKey(), arrayValue);
							termsUnique.forEach(term -> logger.debug(
									"removeDuplicateTerms: attributesWithUniqueTerms Array Value: {}", (String) term));

						}
					} else {

						attributesWithUniqueTerms.put(attribute.getKey(), attribute.getValue());
					}
				}
			}
			logger.debug("removeDuplicateTerms: attributesWithUniqueTerms: {}", attributesWithUniqueTerms.toString());

		}
		return attributesWithUniqueTerms;
	}
}
