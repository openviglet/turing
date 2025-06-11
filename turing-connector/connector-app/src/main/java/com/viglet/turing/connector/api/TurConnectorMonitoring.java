package com.viglet.turing.connector.api;

import com.viglet.turing.connector.persistence.model.TurConnectorIndexing;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Builder
@Getter
@Setter
public class TurConnectorMonitoring {
    private List<String> sources;
    private List<TurConnectorIndexing> indexing;
}
