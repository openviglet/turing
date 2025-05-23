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

package com.viglet.turing.commons.sn.bean;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Documents of results of Turing ES Semantic Navigation response.
 * 
 * @author Alexandre Oliveira
 * 
 * @since 0.3.4
 */

@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public class TurSNSiteSearchDocumentBean implements Serializable {
	
	private String source;
	private boolean elevate;
	private List<TurSNSiteSearchDocumentMetadataBean> metadata;
	private Map<String,Object> fields;

}
