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

package com.viglet.turing.api.sn.console;

import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.viglet.turing.exchange.sn.TurSNSiteExport;
import com.viglet.turing.persistence.model.nlp.TurNLPInstance;
import com.viglet.turing.persistence.model.se.TurSEInstance;
import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.model.sn.locale.TurSNSiteLocale;
import com.viglet.turing.persistence.repository.sn.TurSNSiteRepository;
import com.viglet.turing.sn.template.TurSNTemplate;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/api/sn")
@Tag(name = "Semantic Navigation Site", description = "Semantic Navigation Site API")
@ComponentScan("com.viglet.turing")
public class TurSNSiteAPI {
	private static final Log logger = LogFactory.getLog(TurSNSiteAPI.class);
	private static final String DEFAULT_LANGUAGE = "en_US";
	@Autowired
	private TurSNSiteRepository turSNSiteRepository;
	@Autowired
	private TurSNSiteExport turSNSiteExport;
	@Autowired
	private TurSNTemplate turSNTemplate;

	@Operation(summary = "Semantic Navigation Site List")
	@GetMapping
	public List<TurSNSite> turSNSiteList() {
		return this.turSNSiteRepository.findAll();
	}

	@Operation(summary = "Semantic Navigation Site structure")
	@GetMapping("/structure")
	public TurSNSite turSNSiteStructure() {

		TurSNSiteLocale turSNSiteLocale = new TurSNSiteLocale();
		turSNSiteLocale.setLanguage(DEFAULT_LANGUAGE);
		turSNSiteLocale.setTurNLPInstance(new TurNLPInstance());

		TurSNSite turSNSite = new TurSNSite();

		turSNSite.setTurSEInstance(new TurSEInstance());
		turSNSite.addTurSNSiteLocale(turSNSiteLocale);

		return turSNSite;
	}
	
	@Operation(summary = "Show a Semantic Navigation Site")
	@GetMapping("/{id}")
	public TurSNSite turSNSiteGet(@PathVariable String id) {
		return this.turSNSiteRepository.findById(id).orElse(new TurSNSite());
	}

	@Operation(summary = "Update a Semantic Navigation Site")
	@PutMapping("/{id}")
	public TurSNSite turSNSiteUpdate(@PathVariable String id, @RequestBody TurSNSite turSNSite) {
		return this.turSNSiteRepository.findById(id).map(turSNSiteEdit -> {
			turSNSiteEdit.setName(turSNSite.getName());
			turSNSiteEdit.setDescription(turSNSite.getDescription());
			turSNSiteEdit.setTurSEInstance(turSNSite.getTurSEInstance());
			turSNSiteEdit.setThesaurus(turSNSite.getThesaurus());

			// UI
			turSNSiteEdit.setFacet(turSNSite.getFacet());
			turSNSiteEdit.setHl(turSNSite.getHl());
			turSNSiteEdit.setHlPost(turSNSite.getHlPost());
			turSNSiteEdit.setHlPre(turSNSite.getHlPre());
			turSNSiteEdit.setItemsPerFacet(turSNSite.getItemsPerFacet());
			turSNSiteEdit.setSpellCheck(turSNSite.getSpellCheck());
			turSNSiteEdit.setSpellCheckFixes(turSNSite.getSpellCheckFixes());
			turSNSiteEdit.setMlt(turSNSite.getMlt());
			turSNSiteEdit.setRowsPerPage(turSNSite.getRowsPerPage());
			turSNSiteEdit.setDefaultTitleField(turSNSite.getDefaultTitleField());
			turSNSiteEdit.setDefaultTextField(turSNSite.getDefaultTextField());
			turSNSiteEdit.setDefaultDescriptionField(turSNSite.getDefaultDescriptionField());
			turSNSiteEdit.setDefaultDateField(turSNSite.getDefaultDateField());
			turSNSiteEdit.setDefaultImageField(turSNSite.getDefaultImageField());
			turSNSiteEdit.setDefaultURLField(turSNSite.getDefaultURLField());

			turSNSiteRepository.save(turSNSiteEdit);
			return turSNSiteEdit;
		}).orElse(new TurSNSite());

	}

	@Transactional
	@Operation(summary = "Delete a Semantic Navigation Site")
	@DeleteMapping("/{id}")
	public boolean turSNSiteDelete(@PathVariable String id) {
		turSNSiteRepository.delete(id);
		return true;
	}

	@Operation(summary = "Create a Semantic Navigation Site")
	@PostMapping
	public TurSNSite turSNSiteAdd(@RequestBody TurSNSite turSNSite) {
		turSNSiteRepository.save(turSNSite);
		turSNTemplate.defaultSNUI(turSNSite);
		turSNTemplate.createSEFields(turSNSite);
		return turSNSite;

	}

	@ResponseBody
	@GetMapping(value = "/export", produces = "application/zip")
	public StreamingResponseBody turSNSiteExport(HttpServletResponse response) {

		try {
			return turSNSiteExport.exportObject(response);
		} catch (Exception e) {
			logger.error(e);
		}
		return null;

	}

}