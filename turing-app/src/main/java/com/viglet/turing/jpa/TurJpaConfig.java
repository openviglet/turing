package com.viglet.turing.jpa;

import com.viglet.turing.properties.TurConfigProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import com.viglet.turing.spring.security.TurAuditorAwareImpl;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class TurJpaConfig {
    private final TurConfigProperties turConfigProperties;

    @Autowired
    public TurJpaConfig(TurConfigProperties turConfigProperties) {
        this.turConfigProperties = turConfigProperties;
    }

    @Bean
    AuditorAware<String> auditorAware() {
        return new TurAuditorAwareImpl(turConfigProperties);
    }
}