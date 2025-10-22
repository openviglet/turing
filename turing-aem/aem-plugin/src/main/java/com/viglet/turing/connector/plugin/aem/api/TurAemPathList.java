package com.viglet.turing.connector.plugin.aem.api;

import java.util.List;
import com.viglet.turing.connector.aem.commons.bean.TurAemEvent;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TurAemPathList {
    private List<String> paths;
    private TurAemEvent event;
    private Boolean recursive;
}
