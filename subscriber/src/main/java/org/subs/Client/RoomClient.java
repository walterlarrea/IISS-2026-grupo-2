package org.subs.Client;

import org.springframework.stereotype.Service;
import org.subs.controllertemp.Habitacion;
import org.springframework.web.client.RestClient;

@Service
public class RoomClient {
    private final RestClient restClient;

    public RoomClient(RestClient restClient) {this.restClient = restClient;}

    public Habitacion obtenerHabPorTermo(String idTermo) {
        return restClient.get().uri("/habitaciones/termostato/{idTermostato}", idTermo).retrieve().body(Habitacion.class);
    }
}
