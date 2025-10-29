package com.viglet.turing.connector.plugin.aem.service;

import static com.viglet.turing.connector.aem.commons.TurAemConstants.AEM;
import static com.viglet.turing.connector.aem.commons.TurAemConstants.CONTENT_FRAGMENT;
import static com.viglet.turing.connector.aem.commons.TurAemConstants.CQ_PAGE;
import static com.viglet.turing.connector.aem.commons.TurAemConstants.DAM_ASSET;
import static com.viglet.turing.connector.aem.commons.TurAemConstants.STATIC_FILE;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import com.viglet.turing.connector.aem.commons.TurAemCommonsUtils;
import com.viglet.turing.connector.aem.commons.TurAemObject;
import com.viglet.turing.connector.aem.commons.bean.TurAemTargetAttrValueMap;
import com.viglet.turing.connector.aem.commons.mappers.TurAemModel;
import com.viglet.turing.connector.plugin.aem.TurAemAttrProcess;
import com.viglet.turing.connector.plugin.aem.context.TurAemSession;

@Service
public class TurAemService {
    private final TurAemAttrProcess turAemAttrProcess;

    public TurAemService(TurAemAttrProcess turAemAttrProcess) {
        this.turAemAttrProcess = turAemAttrProcess;
    }

    public String getProviderName() {
        return AEM;
    }

    public @NotNull TurAemTargetAttrValueMap getTargetAttrValueMap(TurAemSession turAemSession,
            TurAemObject aemObject, TurAemModel turAemModel) {
        TurAemTargetAttrValueMap turAemTargetAttrValueMap =
                turAemAttrProcess.prepareAttributeDefs(turAemSession, aemObject);
        turAemTargetAttrValueMap.merge(TurAemCommonsUtils.runCustomClassFromContentType(turAemModel,
                aemObject, turAemSession.getConfiguration()));
        return turAemTargetAttrValueMap;
    }

    public boolean isNotValidType(TurAemModel turAemModel, TurAemObject aemObject, String type) {
        return !isPage(type) && !isContentFragment(turAemModel, type, aemObject)
                && !isStaticFile(turAemModel, type);
    }

    public boolean isPage(String type) {
        return type.equals(CQ_PAGE);
    }

    public boolean isStaticFile(TurAemModel turAemModel, String type) {
        return isAsset(turAemModel, type) && turAemModel.getSubType().equals(STATIC_FILE);
    }

    public boolean isContentFragment(TurAemModel turAemModel, String type, TurAemObject aemObject) {
        return isAsset(turAemModel, type) && turAemModel.getSubType().equals(CONTENT_FRAGMENT)
                && aemObject.isContentFragment();
    }

    public boolean isAsset(TurAemModel turAemModel, String type) {
        return type.equals(DAM_ASSET) && StringUtils.isNotEmpty(turAemModel.getSubType());
    }
}
