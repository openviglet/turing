package com.viglet.turing.connector.plugin.aem.api;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TurAemPathList {
    private List<String> paths;
    private Boolean recursive;
}
