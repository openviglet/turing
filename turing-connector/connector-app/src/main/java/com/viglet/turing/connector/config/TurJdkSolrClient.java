package com.viglet.turing.connector.config;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpJdkSolrClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TurJdkSolrClient {
    private final String solrEndpoint;

    public TurJdkSolrClient(@Value("${turing.solr.endpoint:http://localhost:8983}") String solrEndpoint) {
        this.solrEndpoint = solrEndpoint;
    }

    @Bean
    public SolrClient solrClient() {
        return new HttpJdkSolrClient.Builder("%s/solr".formatted(solrEndpoint))
                .build();
    }
}
