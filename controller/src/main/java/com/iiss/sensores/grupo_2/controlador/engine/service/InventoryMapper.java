package com.iiss.sensores.grupo_2.controlador.engine.service;

import com.iiss.sensores.grupo_2.controlador.api.model.Inventory;
import com.iiss.sensores.grupo_2.enums.RushDays;
import com.iiss.sensores.grupo_2.enums.SwitchState;
import com.iiss.sensores.grupo_2.modelos.input.*;
import com.iiss.sensores.grupo_2.modelos.input.DataSite;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class InventoryMapper {
    //transforma el modelo de la api en modelos que use el core
    public DataSite convertir(Inventory inventory) {
        if (inventory == null || inventory.sitio() == null) {
            throw new IllegalArgumentException("El inventory y el site son obligatorios");
        }

        Inventory.Sitio sitioApi = inventory.sitio();
        Inventory.Franja franja = sitioApi.tarifa().punta();
        RushHour rushHour = null;

        if (franja != null) {
            if (franja.dias() == null) {
                throw new IllegalArgumentException("La fee recibida utiliza días que el core no soporta. {}" + franja.dias());
            }
            try {
                RushDays rushDays = RushDays.valueOf(franja.dias().name());
                rushHour = new RushHour(franja.desde(), franja.hasta(), rushDays);
            } catch (Exception e) {
                throw new IllegalArgumentException("La fee recibida utiliza días que el core no soporta. {}" + franja.dias(), e);
            }
        }

        Site site = new Site(sitioApi.id(), sitioApi.nombre(), sitioApi.potenciaContratadaKW(), new Fee(rushHour));

        List<Room> habitaciones = inventory.habitaciones()
                .stream()
                .map(h -> new Room(
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
                        SwitchState.APAGADO
                ))
                .toList();

        return new DataSite(site, habitaciones);
    }
}

