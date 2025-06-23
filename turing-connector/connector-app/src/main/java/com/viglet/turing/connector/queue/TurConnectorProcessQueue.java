/*
 *
 * Copyright (C) 2016-2024 the original author or authors.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.viglet.turing.connector.queue;

import com.viglet.turing.client.auth.credentials.TurApiKeyCredentials;
import com.viglet.turing.client.sn.TurSNServer;
import com.viglet.turing.client.sn.job.TurSNJobAction;
import com.viglet.turing.client.sn.job.TurSNJobItem;
import com.viglet.turing.client.sn.job.TurSNJobItems;
import com.viglet.turing.client.sn.job.TurSNJobUtils;
import com.viglet.turing.commons.indexing.TurIndexingStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.viglet.turing.connector.TurConnectorConstants.CONNECTOR_INDEXING_QUEUE;
import static com.viglet.turing.connector.logging.TurConnectorLoggingUtils.setSuccessStatus;

@Component
@Slf4j
public class TurConnectorProcessQueue {
    private final String turingUrl;
    private final String turingApiKey;

    public TurConnectorProcessQueue(@Value("${turing.url}") String turingUrl,
                                    @Value("${turing.apiKey}") String turingApiKey) {
        this.turingUrl = turingUrl;
        this.turingApiKey = turingApiKey;
    }

    @JmsListener(destination = CONNECTOR_INDEXING_QUEUE)
    @Transactional
    public void receiveAndSendToTuring(TurSNJobItems turSNJobItems) {
        List<String> sites = new ArrayList<>();
        List<Locale> locales = new ArrayList<>();
        if (turSNJobItems == null || turSNJobItems.getTuringDocuments().isEmpty()) {
            log.info("Job is empty, no action.");
            return;
        }
        log.info("Processing job from queue");
        for (TurSNJobItem turSNJobItem : turSNJobItems) {
            if (!locales.contains(turSNJobItem.getLocale())) {
                locales.add(turSNJobItem.getLocale());
            }
            turSNJobItem.getSiteNames().forEach(site -> {
                if (!sites.contains(site)) {
                    sites.add(site);
                }
            });
            log.debug("Processing {} job item", turSNJobItem.getId());
            setSuccessStatus(turSNJobItem, TurIndexingStatus.RECEIVED_AND_SENT_TO_TURING);
        }
        if (locales.isEmpty()) {
            turSNJobItems.add(new TurSNJobItem(TurSNJobAction.COMMIT, sites));
        } else {
            locales.forEach(locale ->
                    turSNJobItems.add(new TurSNJobItem(TurSNJobAction.COMMIT, sites, locale)));
        }
        TurSNJobUtils.importItems(turSNJobItems, getTurSNServer(), false);
    }

    private TurSNServer getTurSNServer() {
        return new TurSNServer(URI.create(turingUrl), null,
                new TurApiKeyCredentials(turingApiKey));
    }
}
