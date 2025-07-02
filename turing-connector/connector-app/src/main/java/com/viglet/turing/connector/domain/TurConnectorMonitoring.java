package com.viglet.turing.connector.domain;

import com.viglet.turing.connector.persistence.model.TurConnectorIndexingModel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Builder
@Getter
@Setter
public class TurConnectorMonitoring {
    private List<String> sources;
    private List<TurConnectorIndexingModel> indexing;
}
