package com.viglet.turing.connector.spring;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "turing.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class TurSchedulerConfiguration {
}
