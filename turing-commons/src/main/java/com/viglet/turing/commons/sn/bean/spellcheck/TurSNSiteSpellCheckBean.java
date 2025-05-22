/*
 * Copyright (C) 2016-2021 the original author or authors. 
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.viglet.turing.commons.sn.bean.spellcheck;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.viglet.turing.commons.se.result.spellcheck.TurSESpellCheckResult;
import com.viglet.turing.commons.sn.search.TurSNSiteSearchContext;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * Spell Check of Turing ES Semantic Navigation response.
 * 
 * @author Alexandre Oliveira
 * 
 * @since 0.3.5
 */

@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TurSNSiteSpellCheckBean implements Serializable {

	private boolean correctedText;
	private boolean usingCorrectedText;
	private TurSNSiteSpellCheckText original;
	private TurSNSiteSpellCheckText corrected;

	public TurSNSiteSpellCheckBean() {
		super();
	}
	
	public TurSNSiteSpellCheckBean(TurSNSiteSearchContext context, TurSESpellCheckResult turSESpellCheckResult) {
		super();
		this.correctedText = turSESpellCheckResult.isCorrected();
		this.original = new TurSNSiteSpellCheckText(context.getUri(), context.getTurSEParameters().getQuery(), true);
		this.corrected = new TurSNSiteSpellCheckText(context.getUri(), turSESpellCheckResult.getCorrectedText(), false);
		this.usingCorrectedText = turSESpellCheckResult.isUsingCorrected();
	}

}