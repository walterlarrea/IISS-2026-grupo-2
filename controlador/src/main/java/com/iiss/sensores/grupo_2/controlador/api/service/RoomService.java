package com.iiss.sensores.grupo_2.controlador.api.service;

import com.iiss.sensores.grupo_2.controlador.api.model.Inventario;
import com.iiss.sensores.grupo_2.controlador.shared.RoomProvider;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class RoomService implements RoomProvider {

    private final InventarioService inventarioService;

    public RoomService(InventarioService inventarioService) {
        this.inventarioService = inventarioService;
    }

    public List<Inventario.Habitacion> listar() {
        Optional<Inventario> inventarioActivo = inventarioService.obtenerActivo();
        return inventarioActivo.map(Inventario::habitaciones).orElse(null);
    }

    public Optional<Inventario.Habitacion> buscarPorId(String id) {
        Optional<Inventario> inventarioActivo = inventarioService.obtenerActivo();
        return inventarioActivo.flatMap(inventario -> inventario.habitaciones().stream()
                .filter(habitacion -> habitacion.id().equals(id))
                .findFirst());
    }

    @CacheEvict(value = "habitaciones_termostato", allEntries = true)
    public Inventario.Habitacion crear(Inventario.Habitacion habitacion) {
        Inventario inventario = inventarioService.obtenerActivo()
                .orElseThrow(SitioNoConfiguradoException::new);

        Optional<Inventario.Habitacion> existente = inventario.habitaciones().stream()
                .filter(actual -> actual.id().equals(habitacion.id()))
                .findFirst();
        if (existente.isPresent()) {
            return existente.get();
        }

        List<Inventario.Habitacion> habitaciones = new ArrayList<>(inventario.habitaciones());
        habitaciones.add(habitacion);
        inventarioService.reemplazar(new Inventario(inventario.schemaVersion(), inventario.sitio(), habitaciones));
        return habitacion;
    }

    @CacheEvict(value = "habitaciones_termostato", allEntries = true)
    public Optional<Inventario.Habitacion> modificar(String id, Inventario.Habitacion habitacion) {
        Optional<Inventario> inventarioActivo = inventarioService.obtenerActivo();
        if (inventarioActivo.isEmpty()) {
            return Optional.empty();
        }

        Inventario inventario = inventarioActivo.get();
        List<Inventario.Habitacion> habitaciones = new ArrayList<>(inventario.habitaciones());
        for (int i = 0; i < habitaciones.size(); i++) {
            if (habitaciones.get(i).id().equals(id)) {
                Inventario.Habitacion actualizada = new Inventario.Habitacion(
                        id,
                        habitacion.nombre(),
                        habitacion.temperaturaEsperada(),
                        habitacion.potenciaKW(),
                        habitacion.idTermostato(),
                        habitacion.topicTermostato(),
                        habitacion.idSwitch(),
                        habitacion.urlSwitch()
                );
                habitaciones.set(i, actualizada);
                inventarioService.reemplazar(
                        new Inventario(inventario.schemaVersion(), inventario.sitio(), habitaciones)
                );
                return Optional.of(actualizada);
            }
        }
        return Optional.empty();
    }

    @CacheEvict(value = "habitaciones_termostato", allEntries = true)
    public boolean eliminar(String id) {
        Optional<Inventario> inventarioActivo = inventarioService.obtenerActivo();
        if (inventarioActivo.isEmpty()) {
            return false;
        }

        Inventario inventario = inventarioActivo.get();
        List<Inventario.Habitacion> habitaciones = new ArrayList<>(inventario.habitaciones());
        if (!habitaciones.removeIf(habitacion -> habitacion.id().equals(id))) {
            return false;
        }
        inventarioService.reemplazar(
                new Inventario(inventario.schemaVersion(), inventario.sitio(), habitaciones)
        );
        return true;
    }

    @Override
    @Cacheable(value = "habitaciones_termostato", key = "#idTermostato")
    public Optional<Inventario.Habitacion> obtenerPorTermostato(String idTermostato) {
        if (inventarioService != null) {
            Optional<Inventario> inventarioActivo = inventarioService.obtenerActivo();
            if (inventarioActivo.isPresent()) {
                return inventarioActivo.get().habitaciones().stream()
                        .filter(habitacion -> habitacion.idTermostato().equals(idTermostato))
                        .findFirst();
            }
        }
        return Optional.empty();
    }

    @CacheEvict(value = "habitaciones_termostato", allEntries = true)
    public Optional<Inventario.Habitacion> modificarParcialmente(
            String id,
            Map<String, Object> cambios) {

        Optional<Inventario> inventarioActivo = inventarioService.obtenerActivo();
        if (inventarioActivo.isEmpty()) {
            return Optional.empty();
        }

        Inventario inventario = inventarioActivo.get();
        List<Inventario.Habitacion> habitaciones = new ArrayList<>(inventario.habitaciones());
        for (int i = 0; i < habitaciones.size(); i++) {
            Inventario.Habitacion existente = habitaciones.get(i);
            if (!existente.id().equals(id)) {
                continue;
            }

            String urlSwitchKey = cambios.containsKey("urlSwitch") ? "urlSwitch" : "uriSwitch";
            Inventario.Habitacion actualizada = new Inventario.Habitacion(
                    id,
                    cambios.containsKey("nombre") ? (String) cambios.get("nombre") : existente.nombre(),
                    cambios.containsKey("temperaturaEsperada")
                            ? ((Number) cambios.get("temperaturaEsperada")).doubleValue()
                            : existente.temperaturaEsperada(),
                    cambios.containsKey("potenciaKW")
                            ? ((Number) cambios.get("potenciaKW")).doubleValue()
                            : existente.potenciaKW(),
                    cambios.containsKey("idTermostato")
                            ? (String) cambios.get("idTermostato")
                            : existente.idTermostato(),
                    cambios.containsKey("topicTermostato")
                            ? (String) cambios.get("topicTermostato")
                            : existente.topicTermostato(),
                    cambios.containsKey("idSwitch")
                            ? (String) cambios.get("idSwitch")
                            : existente.idSwitch(),
                    cambios.containsKey(urlSwitchKey)
                            ? (String) cambios.get(urlSwitchKey)
                            : existente.urlSwitch()
            );
            habitaciones.set(i, actualizada);
            inventarioService.reemplazar(
                    new Inventario(inventario.schemaVersion(), inventario.sitio(), habitaciones)
            );
            return Optional.of(actualizada);
        }
        return Optional.empty();
    }

    public List<Inventario.Habitacion> validar() {
        List<Inventario.Habitacion> habitaciones = inventarioService.obtenerActivo()
                .map(Inventario::habitaciones)
                .orElseGet(List::of);
        Set<Inventario.Habitacion> noCumplen = new LinkedHashSet<>();

        Map<String, List<Inventario.Habitacion>> porTermostato = new LinkedHashMap<>();
        Map<String, List<Inventario.Habitacion>> porUrlSwitch = new LinkedHashMap<>();

        for (Inventario.Habitacion habitacion : habitaciones) {
            boolean invalido = false;

            if (habitacion.idTermostato() == null || habitacion.idTermostato().isBlank()) {
                invalido = true;
            } else {
                porTermostato.computeIfAbsent(habitacion.idTermostato(), k -> new ArrayList<>()).add(habitacion);
            }

            if (!esUriValida(habitacion.urlSwitch())) {
                invalido = true;
            } else {
                porUrlSwitch.computeIfAbsent(habitacion.urlSwitch(), k -> new ArrayList<>()).add(habitacion);
            }

            if (invalido) {
                noCumplen.add(habitacion);
            }
        }

        for (List<Inventario.Habitacion> grupo : porTermostato.values()) {
            if (grupo.size() > 1) {
                noCumplen.addAll(grupo);
            }
        }

        for (List<Inventario.Habitacion> grupo : porUrlSwitch.values()) {
            if (grupo.size() > 1) {
                noCumplen.addAll(grupo);
            }
        }

        return new ArrayList<>(noCumplen);
    }

    private boolean esUriValida(String uriStr) {
        if (uriStr == null || uriStr.isBlank()) {
            return false;
        }
        try {
            URI uri = new URI(uriStr);
            if (!uri.isAbsolute()) {
                return false;
            }
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                return false;
            }
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
