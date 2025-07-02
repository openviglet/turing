package com.viglet.turing.connector.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Builder
@Getter
@Setter
public class TurConnectorValidateDifference {
    private Map<String, List<String>> missing;
    private Map<String, List<String>> extra;
}
