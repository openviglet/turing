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

package com.viglet.turing.connector.commons.plugin;

import com.viglet.turing.client.sn.job.TurSNJobItem;
import com.viglet.turing.connector.commons.plugin.dto.TurConnectorIndexingDTO;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public interface TurConnectorContext {
    void addJobItem(TurSNJobItem turSNJobItem, TurConnectorSession session, boolean standalone);
    void finishIndexing(TurConnectorSession session, boolean standalone);
    List<TurConnectorIndexingDTO> getIndexingItem(String objectId, String source);
    Optional<TurConnectorIndexingDTO> getIndexingItem(String objectId, String source, String environment, Locale locale);
}
