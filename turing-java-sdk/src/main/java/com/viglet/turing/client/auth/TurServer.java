package com.viglet.turing.client.auth;

import com.viglet.turing.client.auth.credentials.TurApiKeyCredentials;
import lombok.Getter;

import java.net.URI;

@Getter
public class TurServer {

    private static final String PROVIDER_NAME_DEFAULT = "turing-java-sdk";

    private final URI serverUrl;

    private final String apiKey;

    private final String providerName;

    public TurServer(URI serverUrl, TurApiKeyCredentials apiKeyCredentials) {
        super();
        this.serverUrl = serverUrl;
        this.apiKey = apiKeyCredentials.getApiKey();
        this.providerName = PROVIDER_NAME_DEFAULT;
    }
}
