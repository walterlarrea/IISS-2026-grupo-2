package com.iiss.sensores.grupo_2.controlador.engine.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.iiss.sensores.grupo_2.controlador.engine.model.SwitchEstadoResponse;
import com.iiss.sensores.grupo_2.enums.EstadoSwitch;
import com.iiss.sensores.grupo_2.modelos.input.Habitacion;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;

@Service
public class SwitchClient {
    private final RestClient restClient;
    private final String apiKey;

    public SwitchClient(@Qualifier("switchRestClient") RestClient restClient, @Value("${SWITCHES_API_KEY:}") String apiKey) {
        //ejecuta las órdenes contra el servicio de switches que esté configurado.
        this.restClient = restClient;
        this.apiKey = apiKey;
    }

    public void encender(Habitacion habitacion) throws URISyntaxException {
        comandar(habitacion, EstadoAccionSwitch.ON);
    }

    public void apagar(Habitacion habitacion) throws URISyntaxException {
        comandar(habitacion, EstadoAccionSwitch.OFF);
    }

    public SwitchEstadoResponse consultarEstado(Habitacion habitacion) {
        var request = restClient.get()
                .uri(URI.create(componerUrlGetSwitch(habitacion)));
        if (!apiKey.isBlank()) {
            request.header("X-API-Key", apiKey);
        }
        SwitchResponse response = request.retrieve().body(SwitchResponse.class);
        if (response == null || response.estado() == null) {
            throw new IllegalStateException("La API de switches devolvió una respuesta sin estado");
        }
        
        EstadoSwitch estadoSwitch = switch (response.estado()) {
            case EstadoRespuestaSwitch.ON -> EstadoSwitch.ENCENDIDO;
            case EstadoRespuestaSwitch.OFF -> EstadoSwitch.APAGADO;
            case EstadoRespuestaSwitch.DESCONOCIDO -> EstadoSwitch.APAGADO;
            default -> throw new IllegalStateException(
                    "La API de switches devolvió un estado no soportado: " + response.estado()
            );
        };

        return new SwitchEstadoResponse(
            response.idSwitch(),
            estadoSwitch,
            response.aplicadoEn()
        );
    }

    private void comandar(Habitacion habitacion, EstadoAccionSwitch accion) throws URISyntaxException {
        var request = restClient.post()
                .uri(new URI(componerUrlPostSwitch(habitacion)))
                .body(new SwitchRequest(accion));
        if (!apiKey.isBlank()) {
            request.header("X-API-Key", apiKey);
        }
        request.retrieve().body(SwitchResponse.class);
    }

    private String componerUrlPostSwitch(Habitacion habitacion) {
        //construye la URL completa del switch a partir de la información de la habitación
        return habitacion.urlSwitch() + "/switches/" + habitacion.idSwitch() + "/comandos";
    }

    private String componerUrlGetSwitch(Habitacion habitacion) {
        return habitacion.urlSwitch() + "/switches/" + habitacion.idSwitch();
    }

    private enum EstadoAccionSwitch {
        ON, OFF
    }
    private enum EstadoRespuestaSwitch {
        ON, OFF, DESCONOCIDO
    }
    private record SwitchRequest(EstadoAccionSwitch accion) {}
    private record SwitchResponse(String idSwitch, EstadoRespuestaSwitch estado, LocalDate aplicadoEn) {}
}
    