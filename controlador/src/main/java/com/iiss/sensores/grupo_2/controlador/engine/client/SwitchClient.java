package com.iiss.sensores.grupo_2.controlador.engine.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.URISyntaxException;

@Service
public class SwitchClient {
    private final RestClient restClient;
    private final String apiKey;

    public SwitchClient(
            @Qualifier("switchRestClient") RestClient restClient,
            @Value("${SIMULATOR_API_KEY:}") String apiKey
    ) {
        this.restClient = restClient;
        this.apiKey = apiKey;
    }

    public void encender(String uriSwitch) throws URISyntaxException {
        comandar(uriSwitch, "ON");
    }

    public void apagar(String uriSwitch) throws URISyntaxException {
        comandar(uriSwitch, "OFF");
    }

    private void comandar(String uriSwitch, String accion) throws URISyntaxException {
        var request = restClient.post()
                .uri(new URI(uriSwitch))
                .body(new SwitchRequest(accion));
        if (!apiKey.isBlank()) {
            request.header("X-API-Key", apiKey);
        }
        request.retrieve().body(SwitchResponse.class);
    }

    private record SwitchRequest(String accion) {}
}
