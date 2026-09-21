package org.subs.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class SwitchClient {
    private final RestClient restClient;

    public SwitchClient(@Qualifier("switchRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public void encender(String idSwitch) {
        restClient.post().uri("/switches").body(new SwitchRequest(idSwitch, true)).retrieve().toBodilessEntity();
    }

    public void apagar(String idSwitch) {
        restClient.post().uri("/switches").body(new SwitchRequest(idSwitch, false)).retrieve().toBodilessEntity();
    }

    private record SwitchRequest(String id, boolean encendido) {}
}