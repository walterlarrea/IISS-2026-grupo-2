package com.iiss.sensores.grupo_2.switches.service;

import com.iiss.sensores.grupo_2.switches.EstadoSwitch;
import com.iiss.sensores.grupo_2.switches.Switch;
import com.iiss.sensores.grupo_2.switches.dto.SwitchAccionResponse;
import com.iiss.sensores.grupo_2.switches.dto.SwitchResponse;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class SwitchService {

    private final ConcurrentMap<String, Switch> switches = new ConcurrentHashMap<>();

    public SwitchResponse obtener(String id) {
        Switch switchActual = switches.computeIfAbsent(id, ignored -> new Switch(id, EstadoSwitch.OFF, Instant.now()));
        return new SwitchResponse(switchActual.getId(), switchActual.getEstado(), switchActual.getAplicadoEn());
    }

    public SwitchResponse actualizar(String id, EstadoSwitch accion) {
        boolean[] cambio = {false};

        Switch switchActual = switches.compute(id, (clave, existente) -> {
            if (existente == null) {
                cambio[0] = true;
                return new Switch(clave, accion, Instant.now());
            }

            if (existente.getEstado() != accion) {
                cambio[0] = true;
            }

            existente.setEstado(accion);
            existente.setAplicadoEn(Instant.now());
            return existente;
        });

        return new SwitchAccionResponse(switchActual.getId(), switchActual.getEstado(), switchActual.getAplicadoEn(), cambio[0]);
    }
}