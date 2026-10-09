package org.iiss.grupo_2.sub.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.iiss.grupo_2.sub.dto.Room;

@Service
public class RoomClient {
    private final RestClient restClient;

    public RoomClient(@Qualifier("roomRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public Room obtenerHabPorTermo(String idTermo) {
        return restClient.get()
                .uri("/habitaciones/termostato/{idTermostato}", idTermo)
                .retrieve()
                .body(Room.class);
    }
}
