package com.viglet.turing.api.sn.graphql;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.scalars.ExtendedScalars;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.schema.idl.TypeRuntimeWiring;

class GraphQLSchemaContractTest {

    @Test
    void testFieldsSubSelectionSucceeds() throws Exception {
        GraphQL graphQL = createGraphQL();

        ExecutionResult result = graphQL.execute("""
                query {
                  siteSearch(siteName: UNKNOWN, searchParams: {}) {
                    results {
                      document {
                        fields {
                          title
                          url
                        }
                      }
                    }
                  }
                }
                """);

        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void testSearchParamsAcceptsFl() throws Exception {
        GraphQL graphQL = createGraphQL();

        ExecutionResult result = graphQL.execute("""
                query {
                  siteSearch(siteName: UNKNOWN, searchParams: { fl: [\"title\", \"url\"] }) {
                    results {
                      document {
                        fields {
                          title
                        }
                      }
                    }
                  }
                }
                """);

        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void testSiteNamesQueryExists() throws Exception {
        GraphQL graphQL = createGraphQL();

        ExecutionResult result = graphQL.execute("""
                query {
                  siteNames
                }
                """);

        assertTrue(result.getErrors().isEmpty());
    }

    private GraphQL createGraphQL() throws Exception {
        String schemaSdl = StreamUtils.copyToString(
                new ClassPathResource("graphql/schema.graphqls").getInputStream(),
                StandardCharsets.UTF_8);

        TypeDefinitionRegistry typeDefinitionRegistry = new SchemaParser().parse(schemaSdl);
        RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring()
                .scalar(ExtendedScalars.Json)
                .type(TypeRuntimeWiring.newTypeWiring("Query")
                        .dataFetcher("siteNames", env -> java.util.List.of())
                        .dataFetcher("siteSearch", env -> Map.of()))
                .build();

        GraphQLSchema graphQLSchema = new SchemaGenerator()
                .makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);

        return GraphQL.newGraphQL(graphQLSchema).build();
    }
}
