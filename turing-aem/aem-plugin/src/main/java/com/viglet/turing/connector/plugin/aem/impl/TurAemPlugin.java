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

package com.viglet.turing.connector.plugin.aem.impl;

import java.util.List;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import com.viglet.turing.connector.commons.plugin.TurConnectorPlugin;
import com.viglet.turing.connector.plugin.aem.TurAemPluginProcess;
import com.viglet.turing.connector.plugin.aem.persistence.repository.TurAemSourceRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Primary
@Component("aem")
public class TurAemPlugin implements TurConnectorPlugin {
    private final TurAemPluginProcess turAemPluginProcess;
    private final TurAemSourceRepository turAemSourceRepository;

    public TurAemPlugin(TurAemPluginProcess turAemPluginProcess,
            TurAemSourceRepository turAemSourceRepository) {
        this.turAemPluginProcess = turAemPluginProcess;
        this.turAemSourceRepository = turAemSourceRepository;
    }

    @Override
    public void crawl() {
        turAemSourceRepository.findAll().forEach(turAemPluginProcess::indexAll);
    }

    @Override
    public void indexAll(String source) {
        turAemPluginProcess.indexAllByNameAsync(source);
    }

    public void indexById(String source, List<String> contentId) {
        turAemPluginProcess.sentToIndexStandalone(source, contentId, false);
    }

    @Override
    public String getProviderName() {
        return TurAemPluginProcess.getProviderName();
    }
}
