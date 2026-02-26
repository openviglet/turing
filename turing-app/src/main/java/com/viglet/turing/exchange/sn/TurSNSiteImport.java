/*
 * Copyright (C) 2016-2022 the original author or authors. 
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.viglet.turing.exchange.sn;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.viglet.turing.exchange.TurExchange;
import com.viglet.turing.persistence.model.se.TurSEInstance;
import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.model.sn.field.TurSNSiteField;
import com.viglet.turing.persistence.model.sn.field.TurSNSiteFieldExt;
import com.viglet.turing.persistence.model.sn.field.TurSNSiteFieldExtFacet;
import com.viglet.turing.persistence.model.sn.genai.TurSNSiteGenAi;
import com.viglet.turing.persistence.model.sn.locale.TurSNSiteLocale;
import com.viglet.turing.persistence.model.sn.ranking.TurSNRankingCondition;
import com.viglet.turing.persistence.model.sn.ranking.TurSNRankingExpression;
import com.viglet.turing.persistence.model.sn.spotlight.TurSNSiteSpotlight;
import com.viglet.turing.persistence.model.sn.spotlight.TurSNSiteSpotlightDocument;
import com.viglet.turing.persistence.model.sn.spotlight.TurSNSiteSpotlightTerm;
import com.viglet.turing.persistence.repository.se.TurSEInstanceRepository;
import com.viglet.turing.persistence.repository.se.TurSEVendorRepository;
import com.viglet.turing.persistence.repository.sn.TurSNSiteRepository;
import com.viglet.turing.persistence.repository.sn.field.TurSNSiteFieldExtRepository;
import com.viglet.turing.persistence.repository.sn.field.TurSNSiteFieldRepository;
import com.viglet.turing.persistence.repository.sn.genai.TurSNSiteGenAiRepository;
import com.viglet.turing.persistence.repository.sn.locale.TurSNSiteLocaleRepository;
import com.viglet.turing.persistence.repository.sn.ranking.TurSNRankingConditionRepository;
import com.viglet.turing.persistence.repository.sn.ranking.TurSNRankingExpressionRepository;
import com.viglet.turing.persistence.repository.sn.spotlight.TurSNSiteSpotlightRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class TurSNSiteImport {
	private final TurSNSiteRepository turSNSiteRepository;
	private final TurSEInstanceRepository turSEInstanceRepository;
	private final TurSEVendorRepository turSEVendorRepository;
	private final TurSNSiteFieldRepository turSNSiteFieldRepository;
	private final TurSNSiteFieldExtRepository turSNSiteFieldExtRepository;
	private final TurSNSiteLocaleRepository turSNSiteLocaleRepository;
	private final TurSNSiteSpotlightRepository turSNSiteSpotlightRepository;
	private final TurSNRankingExpressionRepository turSNRankingExpressionRepository;
	private final TurSNRankingConditionRepository turSNRankingConditionRepository;
	private final TurSNSiteGenAiRepository turSNSiteGenAiRepository;

	public TurSNSiteImport(TurSNSiteRepository turSNSiteRepository,
			TurSEInstanceRepository turSEInstanceRepository,
			TurSEVendorRepository turSEVendorRepository,
			TurSNSiteFieldRepository turSNSiteFieldRepository,
			TurSNSiteFieldExtRepository turSNSiteFieldExtRepository,
			TurSNSiteLocaleRepository turSNSiteLocaleRepository,
			TurSNSiteSpotlightRepository turSNSiteSpotlightRepository,
			TurSNRankingExpressionRepository turSNRankingExpressionRepository,
			TurSNRankingConditionRepository turSNRankingConditionRepository,
			TurSNSiteGenAiRepository turSNSiteGenAiRepository) {
		this.turSNSiteRepository = turSNSiteRepository;
		this.turSEInstanceRepository = turSEInstanceRepository;
		this.turSEVendorRepository = turSEVendorRepository;
		this.turSNSiteFieldRepository = turSNSiteFieldRepository;
		this.turSNSiteFieldExtRepository = turSNSiteFieldExtRepository;
		this.turSNSiteLocaleRepository = turSNSiteLocaleRepository;
		this.turSNSiteSpotlightRepository = turSNSiteSpotlightRepository;
		this.turSNRankingExpressionRepository = turSNRankingExpressionRepository;
		this.turSNRankingConditionRepository = turSNRankingConditionRepository;
		this.turSNSiteGenAiRepository = turSNSiteGenAiRepository;
	}

	@Transactional
	public void importSNSite(TurExchange turExchange) {
		for (TurSNSiteExchange turSNSiteExchange : turExchange.getSnSites()) {
			// Delete existing site if present (handles re-import with same ID)
			turSNSiteRepository.findById(turSNSiteExchange.getId()).ifPresent(existing -> {
				log.info("SN Site already exists, deleting for re-import: {} ({})",
						existing.getName(), existing.getId());
				turSNSiteRepository.delete(existing);
				turSNSiteRepository.flush();
			});

			TurSNSite turSNSite = createSNSiteFromExchange(turSNSiteExchange);

			if (turSNSite.getTurSEInstance() == null) {
				log.error("Cannot import SN Site '{}' ({}): no SE Instance available. " +
						"Please create a Search Engine instance first.",
						turSNSiteExchange.getName(), turSNSiteExchange.getId());
				continue;
			}

			// Save GenAi configuration (preserves original ID)
			saveGenAi(turSNSiteExchange, turSNSite);

			TurSNSite savedSite = turSNSiteRepository.saveAndFlush(turSNSite);
			log.info("Saved SN Site: {} ({})", savedSite.getName(), savedSite.getId());

			log.info("Exchange fields: {}, fieldExts: {}, locales: {}, spotlights: {}, rankings: {}",
					turSNSiteExchange.getTurSNSiteFields() != null ? turSNSiteExchange.getTurSNSiteFields().size()
							: "null",
					turSNSiteExchange.getTurSNSiteFieldExts() != null ? turSNSiteExchange.getTurSNSiteFieldExts().size()
							: "null",
					turSNSiteExchange.getTurSNSiteLocales() != null ? turSNSiteExchange.getTurSNSiteLocales().size()
							: "null",
					turSNSiteExchange.getTurSNSiteSpotlights() != null
							? turSNSiteExchange.getTurSNSiteSpotlights().size()
							: "null",
					turSNSiteExchange.getTurSNRankingExpressions() != null
							? turSNSiteExchange.getTurSNRankingExpressions().size()
							: "null");

			saveSNSiteFields(turSNSiteExchange, savedSite);
			saveSNSiteFieldExts(turSNSiteExchange, savedSite);
			saveSNSiteLocales(turSNSiteExchange, savedSite);
			saveSNSiteSpotlights(turSNSiteExchange, savedSite);
			saveSNRankingExpressions(turSNSiteExchange, savedSite);

			log.info("Imported SN Site: {} ({})", savedSite.getName(), savedSite.getId());
		}
	}

	private TurSNSite createSNSiteFromExchange(TurSNSiteExchange turSNSiteExchange) {
		TurSNSite turSNSite = new TurSNSite();
		turSNSite.setId(turSNSiteExchange.getId());
		turSNSite.setName(turSNSiteExchange.getName());
		turSNSite.setDescription(turSNSiteExchange.getDescription());
		turSNSite.setRowsPerPage(turSNSiteExchange.getRowsPerPage());
		turSNSite.setWildcardNoResults(turSNSiteExchange.getWildcardNoResults());
		turSNSite.setWildcardAlways(turSNSiteExchange.getWildcardAlways());
		turSNSite.setExactMatch(turSNSiteExchange.getExactMatch());
		turSNSite.setFacet(boolToInteger(turSNSiteExchange.isFacet()));
		turSNSite.setItemsPerFacet(turSNSiteExchange.getItemsPerFacet());
		turSNSite.setHl(boolToInteger(turSNSiteExchange.getHl()));
		turSNSite.setHlPre(turSNSiteExchange.getHlPre());
		turSNSite.setHlPost(turSNSiteExchange.getHlPost());
		turSNSite.setMlt(boolToInteger(turSNSiteExchange.isMlt()));
		turSNSite.setFacetType(turSNSiteExchange.getFacetType());
		turSNSite.setFacetItemType(turSNSiteExchange.getFacetItemType());
		turSNSite.setFacetSort(turSNSiteExchange.getFacetSort());
		turSNSite.setThesaurus(boolToInteger(turSNSiteExchange.isThesaurus()));
		turSNSite.setDefaultField(turSNSiteExchange.getDefaultField());
		turSNSite.setExactMatchField(turSNSiteExchange.getExactMatchField());
		turSNSite.setDefaultTitleField(turSNSiteExchange.getDefaultTitleField());
		turSNSite.setDefaultTextField(turSNSiteExchange.getDefaultTextField());
		turSNSite.setDefaultDescriptionField(turSNSiteExchange.getDefaultDescriptionField());
		turSNSite.setDefaultDateField(turSNSiteExchange.getDefaultDateField());
		turSNSite.setDefaultImageField(turSNSiteExchange.getDefaultImageField());
		turSNSite.setDefaultURLField(turSNSiteExchange.getDefaultURLField());
		turSNSite.setSpellCheck(turSNSiteExchange.getSpellCheck());
		turSNSite.setSpellCheckFixes(turSNSiteExchange.getSpellCheckFixes());
		turSNSite.setSpotlightWithResults(turSNSiteExchange.getSpotlightWithResults());

		// Resolve SE Instance: try the exported ID first, then fall back to any
		// available instance
		resolveSEInstance(turSNSiteExchange, turSNSite);

		return turSNSite;
	}

	private void resolveSEInstance(TurSNSiteExchange turSNSiteExchange, TurSNSite turSNSite) {
		if (turSNSiteExchange.getTurSEInstance() != null) {
			turSEInstanceRepository.findById(turSNSiteExchange.getTurSEInstance())
					.ifPresentOrElse(
							turSNSite::setTurSEInstance,
							() -> {
								log.warn("SE Instance '{}' from export not found. " +
										"Falling back to first available SE Instance.",
										turSNSiteExchange.getTurSEInstance());
								assignFirstAvailableSEInstance(turSNSite);
							});
		} else {
			log.warn("No SE Instance specified in export for SN Site '{}'. " +
					"Falling back to first available SE Instance.",
					turSNSiteExchange.getName());
			assignFirstAvailableSEInstance(turSNSite);
		}
	}

	private void assignFirstAvailableSEInstance(TurSNSite turSNSite) {
		turSEInstanceRepository.findAll().stream().findFirst()
				.ifPresentOrElse(
						seInstance -> {
							log.info("Using SE Instance '{}' ({}) as fallback.",
									seInstance.getTitle(), seInstance.getId());
							turSNSite.setTurSEInstance(seInstance);
						},
						() -> {
							log.warn("No SE Instance available. Creating a default Solr instance.");
							TurSEInstance newInstance = createDefaultSEInstance();
							if (newInstance != null) {
								turSNSite.setTurSEInstance(newInstance);
							} else {
								log.error("Failed to create default SE Instance. " +
										"Cannot import SN Site without a Search Engine instance.");
							}
						});
	}

	private TurSEInstance createDefaultSEInstance() {
		return turSEVendorRepository.findById("SOLR")
				.or(() -> turSEVendorRepository.findAll().stream().findFirst())
				.map(vendor -> {
					TurSEInstance seInstance = new TurSEInstance();
					seInstance.setTitle(vendor.getTitle());
					seInstance.setDescription("Auto-created during import");
					seInstance.setTurSEVendor(vendor);
					seInstance.setHost("localhost");
					seInstance.setPort(8983);
					seInstance.setEnabled(1);
					TurSEInstance saved = turSEInstanceRepository.save(seInstance);
					log.info("Created default SE Instance '{}' ({}) with vendor '{}'.",
							saved.getTitle(), saved.getId(), vendor.getTitle());
					return saved;
				})
				.orElseGet(() -> {
					log.error("No SE Vendor available in the system. Cannot create SE Instance.");
					return null;
				});
	}

	private void saveGenAi(TurSNSiteExchange turSNSiteExchange, TurSNSite turSNSite) {
		TurSNSiteGenAi genAi = turSNSiteExchange.getTurSNSiteGenAi();
		if (genAi != null) {
			// Clear references to nested entities that may not exist on target system
			// The LLM and Store instances need to be configured separately after import
			genAi.setTurLLMInstance(null);
			genAi.setTurStoreInstance(null);
			TurSNSiteGenAi savedGenAi = turSNSiteGenAiRepository.save(genAi);
			turSNSite.setTurSNSiteGenAi(savedGenAi);
			log.info("Saved GenAi configuration (LLM and Store instances will need to be configured after import).");
		}
	}

	private void saveSNSiteFields(TurSNSiteExchange turSNSiteExchange, TurSNSite turSNSite) {
		if (turSNSiteExchange.getTurSNSiteFields() != null) {
			for (TurSNSiteField field : turSNSiteExchange.getTurSNSiteFields()) {
				field.setTurSNSite(turSNSite);
				turSNSiteFieldRepository.save(field);
			}
		}
	}

	private void saveSNSiteFieldExts(TurSNSiteExchange turSNSiteExchange, TurSNSite turSNSite) {
		if (turSNSiteExchange.getTurSNSiteFieldExts() != null) {
			for (TurSNSiteFieldExt fieldExt : turSNSiteExchange.getTurSNSiteFieldExts()) {
				fieldExt.setTurSNSite(turSNSite);
				if (fieldExt.getFacetLocales() != null) {
					for (TurSNSiteFieldExtFacet facet : fieldExt.getFacetLocales()) {
						facet.setTurSNSiteFieldExt(fieldExt);
					}
				}
				turSNSiteFieldExtRepository.save(fieldExt);
			}
		}
	}

	private void saveSNSiteLocales(TurSNSiteExchange turSNSiteExchange, TurSNSite turSNSite) {
		if (turSNSiteExchange.getTurSNSiteLocales() != null) {
			for (TurSNSiteLocale locale : turSNSiteExchange.getTurSNSiteLocales()) {
				locale.setTurSNSite(turSNSite);
				turSNSiteLocaleRepository.save(locale);
			}
		}
	}

	private void saveSNSiteSpotlights(TurSNSiteExchange turSNSiteExchange, TurSNSite turSNSite) {
		if (turSNSiteExchange.getTurSNSiteSpotlights() != null) {
			for (TurSNSiteSpotlight spotlight : turSNSiteExchange.getTurSNSiteSpotlights()) {
				spotlight.setTurSNSite(turSNSite);
				if (spotlight.getTurSNSiteSpotlightTerms() != null) {
					for (TurSNSiteSpotlightTerm term : spotlight.getTurSNSiteSpotlightTerms()) {
						term.setTurSNSiteSpotlight(spotlight);
					}
				}
				if (spotlight.getTurSNSiteSpotlightDocuments() != null) {
					for (TurSNSiteSpotlightDocument doc : spotlight.getTurSNSiteSpotlightDocuments()) {
						doc.setTurSNSiteSpotlight(spotlight);
					}
				}
				turSNSiteSpotlightRepository.save(spotlight);
			}
		}
	}

	private void saveSNRankingExpressions(TurSNSiteExchange turSNSiteExchange, TurSNSite turSNSite) {
		if (turSNSiteExchange.getTurSNRankingExpressions() != null) {
			for (TurSNRankingExpression expr : turSNSiteExchange.getTurSNRankingExpressions()) {
				expr.setTurSNSite(turSNSite);
				turSNRankingExpressionRepository.save(expr);
				if (expr.getTurSNRankingConditions() != null) {
					for (TurSNRankingCondition condition : expr.getTurSNRankingConditions()) {
						condition.setTurSNRankingExpression(expr);
						turSNRankingConditionRepository.save(condition);
					}
				}
			}
		}
	}

	private Integer boolToInteger(boolean bool) {
		return bool ? 1 : 0;
	}
}
