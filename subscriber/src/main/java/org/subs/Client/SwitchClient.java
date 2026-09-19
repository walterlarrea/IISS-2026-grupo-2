package org.subs.Client;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class SwitchClient {
    private final RestClient restClient;

    public SwitchClient() {
        this.restClient = RestClient.builder().baseUrl("http://switches-api:8080").build();
    }

    public void encender(String idSwitch) {
        restClient.post().uri("/switches").body(new SwitchRequest(idSwitch, true)).retrieve().toBodilessEntity();
    }

    public void apagar(String idSwitch) {
        restClient.post().uri("/switches").body(new SwitchRequest(idSwitch, false)).retrieve().toBodilessEntity();
    }

    private record SwitchRequest(String id, boolean encendido) {}
}