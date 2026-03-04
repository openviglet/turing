package com.viglet.turing.api.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.viglet.turing.persistence.model.integration.TurIntegrationInstance;
import com.viglet.turing.persistence.repository.integration.TurIntegrationInstanceRepository;

import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
class TurIntegrationAPITest {

    @Mock
    private TurIntegrationInstanceRepository turIntegrationInstanceRepository;

    @Mock
    private CloseableHttpClient proxyHttpClient;

    @InjectMocks
    private TurIntegrationAPI turIntegrationAPI;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    void testIndexAnyRequest_Found() {
        TurIntegrationInstance instance = new TurIntegrationInstance();
        instance.setId("1");
        instance.setEndpoint("http://example.com/api/v2/integration/1");

        when(turIntegrationInstanceRepository.findById("1")).thenReturn(Optional.of(instance));

        request.setRequestURI("/api/v2/integration/1");
        request.setMethod("GET");

        turIntegrationAPI.indexAnyRequest(request, response, "1");

        verify(turIntegrationInstanceRepository, times(1)).findById("1");
    }

    @Test
    void testIndexAnyRequest_NotFound() {
        when(turIntegrationInstanceRepository.findById("1")).thenReturn(Optional.empty());

        turIntegrationAPI.indexAnyRequest(request, response, "1");

        verify(turIntegrationInstanceRepository, times(1)).findById("1");
        verifyNoInteractions(proxyHttpClient);
    }

    @Test
    void testProxy_InvalidPath_DirectoryTraversal() throws Exception {
        TurIntegrationInstance instance = new TurIntegrationInstance();
        instance.setId("1");
        instance.setEndpoint("http://example.com");

        request.setRequestURI("/api/v2/integration/1/../../secret");
        request.setMethod("GET");

        turIntegrationAPI.proxy(instance, request, response);

        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
        assertEquals("{\"error\": \"Forbidden proxy path\"}", response.getContentAsString());
    }

    @Test
    void testProxy_InvalidPath_NotApiV2() throws Exception {
        TurIntegrationInstance instance = new TurIntegrationInstance();
        instance.setId("1");
        instance.setEndpoint("http://example.com");

        request.setRequestURI("/api/v1/integration/1/something");
        request.setMethod("GET");

        turIntegrationAPI.proxy(instance, request, response);

        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
        assertEquals("{\"error\": \"Forbidden proxy path\"}", response.getContentAsString());
    }

    @Test
    void testProxy_DifferentHost() throws Exception {
        TurIntegrationInstance instance = new TurIntegrationInstance();
        instance.setId("1");
        // baseUri
        instance.setEndpoint("http://example.com");

        // Request URI changes the host if manipulated, but here baseUri is used to
        // resolve.
        // Actually the code tests baseUri vs fullUri. fullUri is
        // baseUri.resolve(relativePath)
        // If relativePath is an absolute URL, it could overwrite host.
        request.setRequestURI("http://malicious.com/api/v2/integration/1/something");
        request.setMethod("GET");

        turIntegrationAPI.proxy(instance, request, response);

        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
        assertEquals("{\"error\": \"Forbidden proxy target\"}", response.getContentAsString());
    }
}
