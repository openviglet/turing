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

package com.viglet.turing.connector.scheduled;

import com.google.inject.Inject;
import com.viglet.turing.connector.commons.plugin.TurConnectorPlugin;
import com.viglet.turing.connector.service.TurConnectorConfigVarService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TurConnectorScheduledTasks {
    private final TurConnectorPlugin turConnectorPlugin;
    private final TurConnectorConfigVarService configVarService;

    @Inject
    public TurConnectorScheduledTasks(TurConnectorPlugin turConnectorPlugin,
                                      TurConnectorConfigVarService configVarService) {
        this.turConnectorPlugin = turConnectorPlugin;
        this.configVarService = configVarService;
    }

    @Scheduled(cron = "${turing.connector.cron:-}", zone="${turing.connector.cron.zone:UTC}")
    public void executeWebCrawler() {
        if (configVarService.hasNotFirstTime()) {
            log.info("This is the first time, waiting next schedule.");
        } else {
            turConnectorPlugin.crawl();
        }
    }


}
