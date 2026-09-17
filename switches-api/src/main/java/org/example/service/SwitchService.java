package org.example.service;

import org.example.Switch;
import org.example.dto.SwitchResponse;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class SwitchService {

    private final ConcurrentMap<String, Switch> switches = new ConcurrentHashMap<>();

    public SwitchResponse obtener(String id) {
        Switch switchActual = switches.computeIfAbsent(id, ignored -> new Switch(id, false));
        return new SwitchResponse(switchActual.getId(), switchActual.isEncendido());
    }

    public SwitchResponse actualizar(String id, boolean encendido) {
        Switch switchActual = switches.compute(id, (clave, existente) -> {
            if (existente == null) {
                return new Switch(clave, encendido);
            }

            existente.setEncendido(encendido);
            return existente;
        });
        return new SwitchResponse(switchActual.getId(), switchActual.isEncendido());
    }
}