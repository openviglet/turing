package com.viglet.turing.aem.server.core.services;

import com.viglet.turing.aem.server.config.TurAemIndexerConfig;
import lombok.Getter;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.Designate;

@Component(immediate = true)
@Designate(ocd = TurAemIndexerConfig.class)
public class TurAemIndexerServiceImpl implements TurAemIndexerService {

    @Getter
    private TurAemIndexerConfig config;

    @Activate
    public void activate(TurAemIndexerConfig config) {
        this.config = config;
    }
}
