/*
 *
 * Copyright (C) 2016-2024 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If
 * not, see <https://www.gnu.org/licenses/>.
 */

package com.viglet.turing.connector.aem.commons.context;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;

import com.viglet.turing.connector.aem.commons.TurAemCommonsUtils;
import com.viglet.turing.connector.aem.commons.bean.TurAemEnv;
import com.viglet.turing.connector.aem.commons.config.IAemConfiguration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class TurAemConfiguration {
  private String id;
  private String url;
  private String username;
  private String password;
  private String rootPath;
  private String contentType;
  private String subType;

  private String siteName;
  private Locale defaultLocale;
  private String providerName;
  private String authorURLPrefix;
  private String publishURLPrefix;
  private String oncePattern;
  private boolean author;
  private boolean publish;
  private String authorSNSite;
  private String publishSNSite;

  private String turSNSite;
  private TurAemEnv environment;
  @Builder.Default
  private Collection<TurAemLocalePathContext> localePaths = new HashSet<>();

  public TurAemConfiguration(TurAemConfiguration turAemConfiguration) {
    this.id = turAemConfiguration.getId();
    this.url = turAemConfiguration.getUrl();
    this.username = turAemConfiguration.getUsername();
    this.password = turAemConfiguration.getPassword();
    this.rootPath = turAemConfiguration.getRootPath();
    this.contentType = turAemConfiguration.getContentType();
    this.subType = turAemConfiguration.getSubType();

    this.siteName = turAemConfiguration.getSiteName();
    this.defaultLocale = turAemConfiguration.getDefaultLocale();
    this.providerName = turAemConfiguration.getProviderName();
    this.authorURLPrefix = turAemConfiguration.getAuthorURLPrefix();
    this.publishURLPrefix = turAemConfiguration.getPublishURLPrefix();
    this.oncePattern = turAemConfiguration.getOncePattern();
    this.localePaths = turAemConfiguration.getLocalePaths();
    this.author = turAemConfiguration.isAuthor();
    this.publish = turAemConfiguration.isPublish();
    this.authorSNSite = turAemConfiguration.getAuthorSNSite();
    this.publishSNSite = turAemConfiguration.getPublishSNSite();

    this.turSNSite = turAemConfiguration.getTurSNSite();
    this.environment = turAemConfiguration.getEnvironment();
  }

  public TurAemConfiguration(IAemConfiguration iaemConfiguration) {

    this.id = iaemConfiguration.getCmsGroup();
    this.contentType = iaemConfiguration.getCmsContentType();
    this.defaultLocale = iaemConfiguration.getDefaultLocale();
    this.rootPath = iaemConfiguration.getCmsRootPath();
    this.url = iaemConfiguration.getCmsHost();
    this.authorURLPrefix = iaemConfiguration.getAuthorURLPrefix();
    this.publishURLPrefix = iaemConfiguration.getPublishURLPrefix();
    this.subType = iaemConfiguration.getCmsSubType();
    this.oncePattern = iaemConfiguration.getOncePatternPath();
    this.providerName = iaemConfiguration.getProviderName();
    this.password = iaemConfiguration.getCmsPassword();
    this.username = iaemConfiguration.getCmsUsername();
    this.localePaths = iaemConfiguration.getLocales();
    this.author = iaemConfiguration.isAuthor();
    this.publish = iaemConfiguration.isPublish();
    this.authorSNSite = iaemConfiguration.getAuthorSNSite();
    this.publishSNSite = iaemConfiguration.getPublishSNSite();

    TurAemCommonsUtils.getInfinityJson(iaemConfiguration.getCmsRootPath(), this, false)
        .flatMap(infinityJson -> TurAemCommonsUtils.getSiteName(this,
            infinityJson))
        .ifPresent(this::setSiteName);
  }

  public String getUrlPrefix() {
    return getEnvironment().equals(TurAemEnv.AUTHOR) ? getAuthorURLPrefix()
        : getPublishURLPrefix();
  }
}
