package com.viglet.turing.aem.server.config;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Turing Indexer Configuration")
public @interface TurAemIndexerConfig {
    @AttributeDefinition(name = "Enabled", description = "Enables to run the Turing Event Handler")
    boolean enabled() default false;

    @AttributeDefinition(name = "Turing API - Host")
    String host() default "";

    @AttributeDefinition(name = "Turing Connector Config Name")
    String configName() default "";
}
