package com.viglet.turing.connector.domain;

import com.viglet.turing.connector.persistence.model.TurConnectorIndexingModel;
import lombok.*;

import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TurConnectorMonitoring {
    private List<String> sources;
    private List<TurConnectorIndexingModel> indexing;
}
