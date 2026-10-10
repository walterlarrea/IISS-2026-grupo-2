package com.iiss.sensores.grupo_2.sub.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import com.iiss.sensores.grupo_2.sub.classes.Room;
import org.springframework.web.client.RestClient;

@Service
public class RoomClient {
    private final RestClient restClient;

    public RoomClient(@Qualifier("roomRestClient") RestClient restClient) {this.restClient = restClient;}

    public Room obtenerHabPorTermo(String idTermo) {
        return restClient.get().uri("/habitaciones/termostato/{idTermostato}", idTermo).retrieve().body(Room.class);
    }
}
