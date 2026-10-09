package org.iiss.grupo_2.sub.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.iiss.grupo_2.sub.dto.Switch;

import java.net.URI;
import java.net.URISyntaxException;

@Service
public class SwitchClient {
    private final RestClient restClient;

    public SwitchClient(@Qualifier("switchRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public void encender(String uriSwitch) throws URISyntaxException {
        URI uri = new URI(uriSwitch);
        restClient.post().uri(uri).body(new SwitchRequest(true)).retrieve().body(Switch.class);
    }

    public void apagar(String uriSwitch) throws URISyntaxException {
        URI uri = new URI(uriSwitch);
        restClient.post().uri(uri).body(new SwitchRequest(false)).retrieve().body(Switch.class);
    }

    private record SwitchRequest(boolean encendido) {}
}
