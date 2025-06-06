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

package com.viglet.turing.connector.aem.sample.ext;

import com.viglet.turing.connector.aem.commons.TurAemCommonsUtils;
import com.viglet.turing.connector.aem.commons.TurAemObject;
import com.viglet.turing.connector.aem.commons.bean.TurAemTargetAttrValueMap;
import com.viglet.turing.connector.aem.commons.context.TurAemSourceContext;
import com.viglet.turing.connector.aem.commons.ext.TurAemExtContentInterface;
import com.viglet.turing.connector.aem.sample.beans.TurAemSampleModel;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TurAemExtSampleModelJson implements TurAemExtContentInterface {
    public static final String FRAGMENT_PATH = "fragmentPath";
    public static final String MODEL_JSON_EXTENSION = ".model.json";

    @Override
    public TurAemTargetAttrValueMap consume(TurAemObject aemObject, TurAemSourceContext turAemSourceContext) {
        log.debug("Executing TurAemExtSampleModelJson");
        String url = turAemSourceContext.getUrl() + aemObject.getPath() + MODEL_JSON_EXTENSION;
        TurAemTargetAttrValueMap attrValues = new TurAemTargetAttrValueMap();
        return TurAemCommonsUtils.getResponseBody(url, turAemSourceContext, TurAemSampleModel.class, false)
                .map(model -> {
                    getFragmentData(attrValues, model);
                    return attrValues;
                }).orElseGet(TurAemTargetAttrValueMap::new);
    }

    private static void getFragmentData(TurAemTargetAttrValueMap attrValues, TurAemSampleModel model) {
        attrValues.addWithSingleValue(FRAGMENT_PATH, model.getFragmentPath(), true);
    }
}
