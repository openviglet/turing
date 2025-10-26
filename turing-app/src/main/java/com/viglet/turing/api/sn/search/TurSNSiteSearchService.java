package com.viglet.turing.api.sn.search;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.viglet.turing.commons.sn.TurSNConfig;
import com.viglet.turing.commons.sn.bean.TurSNSearchLatestRequestBean;
import com.viglet.turing.commons.sn.bean.TurSNSearchParams;
import com.viglet.turing.commons.sn.bean.TurSNSiteLocaleBean;
import com.viglet.turing.commons.sn.bean.TurSNSitePostParamsBean;
import com.viglet.turing.commons.sn.bean.TurSNSiteSearchBean;
import com.viglet.turing.commons.sn.search.TurSNFilterQueryOperator;
import com.viglet.turing.commons.sn.search.TurSNSiteSearchContext;
import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.repository.sn.field.TurSNSiteFieldExtRepository;
import com.viglet.turing.sn.TurSNSearchProcess;
import com.viglet.turing.sn.TurSNUtils;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class TurSNSiteSearchService {

    @Value("${turing.search.cache.enabled:false}")
    private boolean searchCacheEnabled;
    private final TurSNSearchProcess turSNSearchProcess;
    private final TurSNSiteSearchCachedAPI turSNSiteSearchCachedAPI;
    private final TurSNSiteFieldExtRepository turSNSiteFieldExtRepository;

    public TurSNSiteSearchService(TurSNSearchProcess turSNSearchProcess,
            TurSNSiteSearchCachedAPI turSNSiteSearchCachedAPI,
            TurSNSiteFieldExtRepository turSNSiteFieldExtRepository) {
        this.turSNSearchProcess = turSNSearchProcess;
        this.turSNSiteSearchCachedAPI = turSNSiteSearchCachedAPI;
        this.turSNSiteFieldExtRepository = turSNSiteFieldExtRepository;
    }

    public Locale convertToLocale(String localeRequest) {
        return LocaleUtils.toLocale(localeRequest);
    }

    public TurSNSiteSearchContext getTurSNSiteSearchContext(
            TurSNSearchParams turSNSearchParams, HttpServletRequest request, TurSNSite site) {
        TurSNConfig turSNConfig = getTurSNConfig(site);
        TurSNSiteSearchContext turSNSiteSearchContext = TurSNUtils.getTurSNSiteSearchContext(turSNConfig,
                site.getName(), turSNSearchParams, request);
        return turSNSiteSearchContext;
    }

    public ResponseEntity<TurSNSiteSearchBean> executeGetSearch(String siteName,
            TurSNSearchParams turSNSearchParams,
            HttpServletRequest request,
            TurSNSite site) {

        TurSNSiteSearchContext searchContext = getTurSNSiteSearchContext(turSNSearchParams, request, site);
        return searchCacheEnabled
                ? executeSearchWithCache(request, searchContext)
                : executeSearch(searchContext);
    }

    public <T> ResponseEntity<T> notFoundResponse() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    public ResponseEntity<TurSNSiteSearchBean> executeSearch(TurSNSiteSearchContext turSNSiteSearchContext) {
        return new ResponseEntity<>(
                turSNSearchProcess.search(turSNSiteSearchContext), HttpStatus.OK);
    }

    public ResponseEntity<TurSNSiteSearchBean> executeSearchWithCache(HttpServletRequest request,
            TurSNSiteSearchContext turSNSiteSearchContext) {
        return new ResponseEntity<>(
                turSNSiteSearchCachedAPI.searchCached(
                        TurSNUtils.getCacheKey(turSNSiteSearchContext.getSiteName(), request),
                        turSNSiteSearchContext),
                HttpStatus.OK);
    }

    public ResponseEntity<TurSNSiteSearchBean> executePostSearch(
            TurSNSearchParams turSNSearchParams,
            TurSNSitePostParamsBean turSNSitePostParamsBean,
            HttpServletRequest request,
            TurSNSite site) {
        if (StringUtils.isNotBlank(turSNSitePostParamsBean.getLocale())) {
            turSNSearchParams.setLocale(
                    convertToLocale(turSNSitePostParamsBean.getLocale()));
        }

        turSNSitePostParamsBean.setTargetingRules(
                turSNSearchProcess.requestTargetingRules(turSNSitePostParamsBean.getTargetingRules()));
        TurSNSiteSearchContext searchContext = TurSNUtils.getTurSNSiteSearchContext(
                getTurSNConfig(site), site.getName(), turSNSearchParams,
                turSNSitePostParamsBean, request);
        if (searchCacheEnabled) {
            return executeSearchWithCache(request, searchContext);
        } else {
            return executeSearch(searchContext);
        }
    }

    public TurSNConfig getTurSNConfig(TurSNSite turSNSite) {
        TurSNConfig turSNConfig = new TurSNConfig();
        turSNConfig.setHlEnabled(TurSNUtils.isTrue(turSNSite.getHl()) && hasSiteHLFields(turSNSite));
        return turSNConfig;
    }

    public boolean hasSiteHLFields(TurSNSite turSNSite) {
        return !CollectionUtils.isEmpty(turSNSiteFieldExtRepository
                .findByTurSNSiteAndHlAndEnabled(turSNSite, 1, 1));
    }

    public String isLatestImpersonate(
            Optional<TurSNSearchLatestRequestBean> turSNSearchLatestRequestBean,
            Principal principal) {
        if (turSNSearchLatestRequestBean.isPresent()
                && turSNSearchLatestRequestBean.get().getUserId() != null) {
            return turSNSearchLatestRequestBean.get().getUserId();
        } else {
            return principal.getName();
        }
    }

    public static List<TurSNSiteLocaleBean> getTurSNSiteLocaleBeans() {
        return new ArrayList<>();
    }

    public Locale determineLocale(TurSNSitePostParamsBean turSNSitePostParamsBean, Locale locale) {
        return StringUtils.isNotBlank(turSNSitePostParamsBean.getLocale())
                ? convertToLocale(turSNSitePostParamsBean.getLocale())
                : locale;
    }

    public void setSearchParams(TurSNSearchParams turSNSearchParams, List<String> filterQueriesDefault,
            List<String> filterQueriesAnd, List<String> filterQueriesOr,
            TurSNFilterQueryOperator fqOperator, TurSNFilterQueryOperator fqItemOperator,
            Locale locale) {
        if (filterQueriesDefault != null)
            turSNSearchParams.setFq(filterQueriesDefault);
        if (filterQueriesAnd != null)
            turSNSearchParams.setFqAnd(filterQueriesAnd);
        if (filterQueriesOr != null)
            turSNSearchParams.setFqOr(filterQueriesOr);
        if (fqOperator != null)
            turSNSearchParams.setFqOp(fqOperator);
        if (fqItemOperator != null)
            turSNSearchParams.setFqiOp(fqItemOperator);
        if (locale != null)
            turSNSearchParams.setLocale(locale);
    }

}
