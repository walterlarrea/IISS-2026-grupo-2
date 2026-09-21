package org.subs.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.subs.controllertemp.Habitacion;

@Service
public class RoomClient {
    private final RestClient restClient;

    public RoomClient(@Qualifier("roomRestClient") RestClient restClient) {this.restClient = restClient;}

    public Habitacion obtenerHabPorTermo(String idTermo) {
        return restClient.get().uri("/habitaciones/termostato/{idTermostato}", idTermo).retrieve().body(Habitacion.class);
    }
}
