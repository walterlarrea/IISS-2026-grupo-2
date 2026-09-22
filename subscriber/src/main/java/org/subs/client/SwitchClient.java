package org.subs.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.subs.classes.Switch;

import java.net.URI;
import java.net.URISyntaxException;

@Service
public class SwitchClient {
    private final RestClient restClient;

    public SwitchClient(@Qualifier("switchRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public void encender(String idSwitch) throws URISyntaxException {
        URI uri = new URI(idSwitch);
        restClient.post().uri(uri).body(new SwitchRequest(true)).retrieve().body(Switch.class);
    }

    public void apagar(String idSwitch) throws URISyntaxException {
        URI uri = new URI(idSwitch);
        restClient.post().uri(uri).body(new SwitchRequest(false)).retrieve().body(Switch.class);
    }

    private record SwitchRequest(boolean encendido) {}
}