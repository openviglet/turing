package com.viglet.turing.connector.plugin.aem.service;

import static com.viglet.turing.connector.aem.commons.TurAemConstants.AEM;

import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.viglet.turing.connector.aem.commons.TurAemCommonsUtils;
import com.viglet.turing.connector.aem.commons.config.IAemConfiguration;
import com.viglet.turing.connector.aem.commons.context.TurAemSourceContext;
import com.viglet.turing.connector.commons.TurConnectorSession;
import com.viglet.turing.connector.plugin.aem.conf.AemPluginHandlerConfiguration;
import com.viglet.turing.connector.plugin.aem.persistence.model.TurAemPluginSystem;
import com.viglet.turing.connector.plugin.aem.persistence.model.TurAemSource;
import com.viglet.turing.connector.plugin.aem.persistence.repository.TurAemPluginSystemRepository;
import com.viglet.turing.connector.plugin.aem.persistence.repository.TurAemSourceRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TurAemSourceService {
    private final TurAemSourceRepository turAemSourceRepository;
    private final TurAemPluginSystemRepository turAemPluginSystemRepository;

    public TurAemSourceService(TurAemSourceRepository turAemSourceRepository,
            TurAemPluginSystemRepository turAemPluginSystemRepository) {
        this.turAemSourceRepository = turAemSourceRepository;
        this.turAemPluginSystemRepository = turAemPluginSystemRepository;
    }

    public List<TurAemSource> getAllSources() {
        return turAemSourceRepository.findAll();
    }

    public Optional<TurAemSource> getTurAemSourceByName(String source) {
        return turAemSourceRepository.findByName(source);
    }

    public Optional<TurAemSource> getTurAemSourceById(String id) {
        return turAemSourceRepository.findById(id);
    }

    public TurConnectorSession getTurConnectorSession(TurAemSource turAemSource) {
        return new TurConnectorSession(turAemSource.getName(), null, getProviderName(),
                turAemSource.getDefaultLocale());
    }

    public static String getProviderName() {
        return AEM;
    }

    public boolean isOnce(TurAemSourceContext turAemSourceContext) {
        return turAemPluginSystemRepository
                .findByConfig(TurAemCommonsUtils.configOnce(turAemSourceContext))
                .map(TurAemPluginSystem::isBooleanValue).orElse(false);
    }

    private TurAemSourceContext getTurAemSourceContext(IAemConfiguration config) {
        TurAemSourceContext turAemSourceContext = TurAemSourceContext.builder()
                .id(config.getCmsGroup()).contentType(config.getCmsContentType())
                .defaultLocale(config.getDefaultLocale())
                .rootPath(config.getCmsRootPath()).url(config.getCmsHost())
                .authorURLPrefix(config.getAuthorURLPrefix())
                .publishURLPrefix(config.getPublishURLPrefix())
                .subType(config.getCmsSubType())
                .oncePattern(config.getOncePatternPath())
                .providerName(config.getProviderName())
                .password(config.getCmsPassword()).username(config.getCmsUsername())
                .localePaths(config.getLocales()).build();
        TurAemCommonsUtils
                .getInfinityJson(config.getCmsRootPath(), turAemSourceContext,
                        false)
                .flatMap(infinityJson -> TurAemCommonsUtils
                        .getSiteName(turAemSourceContext, infinityJson))
                .ifPresent(turAemSourceContext::setSiteName);
        if (log.isDebugEnabled()) {
            log.debug("TurAemSourceContext: {}", turAemSourceContext);
        }
        return turAemSourceContext;
    }

    public TurAemSourceContext getTurAemSourceContext(TurAemSource turAemSource) {
        return getTurAemSourceContext(new AemPluginHandlerConfiguration(
                turAemSource));
    }

    public boolean isPublish(TurAemSource turAemSource) {
        return turAemSource.isPublish()
                && StringUtils.isNotEmpty(turAemSource.getPublishSNSite());
    }

    public boolean isAuthor(TurAemSource turAemSource) {
        return turAemSource.isAuthor()
                && StringUtils.isNotEmpty(turAemSource.getAuthorSNSite());
    }
}
