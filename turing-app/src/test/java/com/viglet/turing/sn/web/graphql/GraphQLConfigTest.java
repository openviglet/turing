package com.viglet.turing.sn.web.graphql;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

import com.viglet.turing.sn.domain.TurSNSiteRepositoryPort;
import com.viglet.turing.sn.persistence.repository.field.TurSNSiteFieldExtRepository;

import graphql.schema.idl.RuntimeWiring;

class GraphQLConfigTest {

    @Test
    void testConfigure() {
        TurSNSiteRepositoryPort siteRepositoryPort = mock(TurSNSiteRepositoryPort.class);
        TurSNSiteFieldExtRepository fieldExtRepository = mock(TurSNSiteFieldExtRepository.class);
        GraphQLConfig config = new GraphQLConfig(siteRepositoryPort, fieldExtRepository);
        RuntimeWiring.Builder builder = RuntimeWiring.newRuntimeWiring();

        assertDoesNotThrow(() -> config.configure(builder));
        assertNotNull(config.dynamicSearchDocumentFieldsCustomizer());
    }
}
