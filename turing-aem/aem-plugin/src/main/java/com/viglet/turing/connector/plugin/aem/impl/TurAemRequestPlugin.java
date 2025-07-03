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

package com.viglet.turing.connector.plugin.aem.impl;

import com.viglet.turing.connector.commons.plugin.TurConnectorRequestPlugin;
import com.viglet.turing.connector.plugin.aem.TurAemPluginStandalone;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Primary
@Component("aem-request")
public class TurAemRequestPlugin implements TurConnectorRequestPlugin {
    private final TurAemPluginStandalone turAemPluginStandalone;

    @Autowired
    public TurAemRequestPlugin(TurAemPluginStandalone turAemPluginStandalone) {
        this.turAemPluginStandalone = turAemPluginStandalone;
    }


    @Override
    public void sentToIndexByIdList(String source, List<String> idList) {
        turAemPluginStandalone.sentToIndexStandalone(source, idList);
    }

}