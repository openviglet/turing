package com.viglet.turing.connector.plugin.aem.context;

import java.util.List;
import java.util.Optional;

import com.viglet.turing.client.sn.job.TurSNAttributeSpec;
import com.viglet.turing.connector.aem.commons.bean.TurAemEvent;
import com.viglet.turing.connector.aem.commons.context.TurAemConfiguration;
import com.viglet.turing.connector.aem.commons.mappers.TurAemContentMapping;
import com.viglet.turing.connector.aem.commons.mappers.TurAemModel;
import com.viglet.turing.connector.commons.TurConnectorSession;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TurAemSession extends TurConnectorSession {
    private TurAemConfiguration configuration;
    private Optional<TurAemModel> model;
    private TurAemContentMapping contentMapping;
    private TurAemEvent event;
    private boolean standalone;
    private boolean indexChildren;
    private List<TurSNAttributeSpec> attributeSpecs;
}
