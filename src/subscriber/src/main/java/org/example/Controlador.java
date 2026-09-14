package org.example;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class Controlador {

    private static final Logger logger = LoggerFactory.getLogger(Controlador.class);

    private final HttpClient httpClient;
    private final String roomsApiUrl;

    public Controlador(String roomsApiUrl) {
        this.httpClient = HttpClient.newHttpClient();
        this.roomsApiUrl = roomsApiUrl;
    }

    public Orden controlar(Temperatura temperatura) {

        String idTermostato = String.valueOf(temperatura.id());

        try {

            Document habitacion = buscarHabitacion(idTermostato);

            double temperaturaEsperada =
                    habitacion.getDouble("temperaturaEsperada");

            double temperaturaActual = temperatura.tC();

            if (temperaturaActual < temperaturaEsperada) {

                logger.info(
                        "Termostato {}: {}°C < {}°C -> ON",
                        idTermostato,
                        temperaturaActual,
                        temperaturaEsperada
                );

                return Orden.ON;
            }

            if (temperaturaActual > temperaturaEsperada) {

                logger.info(
                        "Termostato {}: {}°C > {}°C -> OFF",
                        idTermostato,
                        temperaturaActual,
                        temperaturaEsperada
                );

                return Orden.OFF;
            }

            return Orden.OFF;

        } catch (Exception e) {

            logger.error(
                    "Error al controlar el termostato {}",
                    idTermostato,
                    e
            );

            return Orden.OFF;
        }
    }

    private Document buscarHabitacion(String idTermostato)
            throws Exception {

        String url =
                roomsApiUrl + "/habitaciones/termostato/" + idTermostato;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response =
                httpClient.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

        if (response.statusCode() != 200) {
            throw new Exception(
                    "No se encontró habitación para el termostato "
                            + idTermostato
            );
        }

        return Document.parse(response.body());
    }
}
