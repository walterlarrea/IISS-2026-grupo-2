package com.iiss.sensores.grupo_2.controlador.engine.service;

import com.iiss.sensores.grupo_2.controlador.api.model.Inventario;
import com.iiss.sensores.grupo_2.enums.DiasPunta;
import com.iiss.sensores.grupo_2.enums.EstadoSwitch;
import com.iiss.sensores.grupo_2.modelos.input.DataSitio;
import com.iiss.sensores.grupo_2.modelos.input.Habitacion;
import com.iiss.sensores.grupo_2.modelos.input.Punta;
import com.iiss.sensores.grupo_2.modelos.input.Tarifa;
import com.iiss.sensores.grupo_2.modelos.input.Sitio;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class InventarioMapper {
    //transforma el modelo de la api en modelos que use el core
    public DataSitio convertir(Inventario inventario) {
        if (inventario == null || inventario.sitio() == null) {
            throw new IllegalArgumentException("El inventario y el sitio son obligatorios");
        }

        Inventario.Sitio sitioApi = inventario.sitio();
        Inventario.Franja franja = sitioApi.tarifa().punta();
        Punta punta = null;

        if (franja != null) {
            if (franja.dias() == null || !"HABILES".equals(franja.dias().name())) {
                throw new IllegalArgumentException("La tarifa recibida utiliza días que el core no soporta");
            }
            punta = new Punta(franja.desde(), franja.hasta(), DiasPunta.HABILES);
        }

        Sitio sitio = new Sitio(sitioApi.id(), sitioApi.nombre(), sitioApi.potenciaContratadaKW(), new Tarifa(punta));

        List<Habitacion> habitaciones = inventario.habitaciones()
                .stream()
                .map(h -> new Habitacion(
                        h.id(),
                        h.nombre(),
                        h.temperaturaEsperada(),
                        h.potenciaKW(),
                        h.idTermostato(),
                        h.topicTermostato(),
                        h.idSwitch(),
                        h.urlSwitch(),
                        //Not a Number
                        Double.NaN, //temperatura desconocida hasta recibir MQTT, sería la temperatura actual
                        EstadoSwitch.APAGADO
                ))
                .toList();

        return new DataSitio(sitio, habitaciones);
    }
}

