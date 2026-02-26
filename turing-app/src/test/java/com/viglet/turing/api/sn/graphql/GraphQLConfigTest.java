package com.viglet.turing.api.sn.graphql;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

import graphql.schema.idl.RuntimeWiring;

class GraphQLConfigTest {

    @Test
    void testConfigure() {
        GraphQLConfig config = new GraphQLConfig();
        RuntimeWiring.Builder builder = RuntimeWiring.newRuntimeWiring();

        assertDoesNotThrow(() -> config.configure(builder));
    }
}
