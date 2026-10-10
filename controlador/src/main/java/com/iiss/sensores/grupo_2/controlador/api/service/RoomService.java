package com.iiss.sensores.grupo_2.controlador.api.service;

import com.iiss.sensores.grupo_2.controlador.api.model.Room;
import com.iiss.sensores.grupo_2.controlador.api.repository.RoomRepository;
import com.iiss.sensores.grupo_2.controlador.shared.RoomInfo;
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

    private final RoomRepository repository;

    public RoomService(RoomRepository repository) {
        this.repository = repository;
    }

    public List<Room> listar() {
        return repository.findAll();
    }

    public Optional<Room> buscarPorId(String id) {
        return repository.findById(id);
    }

    @CacheEvict(value = "habitaciones_termostato", allEntries = true)
    public Room crear(Room room) {
        return repository.save(room);
    }

    @CacheEvict(value = "habitaciones_termostato", allEntries = true)
    public Optional<Room> modificar(String id, Room room) {
        return repository.findById(id).map(existente -> {
            existente.setNombre(room.getNombre());
            existente.setTemperaturaEsperada(room.getTemperaturaEsperada());
            existente.setIdTermostato(room.getIdTermostato());
            existente.setUriSwitch(room.getUriSwitch());

            return repository.save(existente);
        });
    }

    @CacheEvict(value = "habitaciones_termostato", allEntries = true)
    public boolean eliminar(String id) {
        if (!repository.existsById(id)) {
            return false;
        }

        repository.deleteById(id);
        return true;
    }

    public Optional<Room> buscarPorIdTermostato(String idTermostato) {
        return repository.findByIdTermostato(idTermostato);
    }

    @Override
    @Cacheable(value = "habitaciones_termostato", key = "#idTermostato")
    public Optional<RoomInfo> obtenerPorTermostato(String idTermostato) {
        return repository.findByIdTermostato(idTermostato)
                .map(r -> new RoomInfo(
                        r.getId(),
                        r.getNombre(),
                        r.getTemperaturaEsperada(),
                        r.getIdTermostato(),
                        r.getUriSwitch()
                ));
    }

    @CacheEvict(value = "habitaciones_termostato", allEntries = true)
    public Optional<Room> modificarParcialmente(
            String id,
            Map<String, Object> cambios) {

        return repository.findById(id)
                .map(existente -> {

                    if (cambios.containsKey("nombre")) {
                        existente.setNombre((String) cambios.get("nombre"));
                    }

                    if (cambios.containsKey("temperaturaEsperada")) {
                        existente.setTemperaturaEsperada(
                                ((Number) cambios.get("temperaturaEsperada")).doubleValue()
                        );
                    }

                    if (cambios.containsKey("idTermostato")) {
                        existente.setIdTermostato(
                                (String) cambios.get("idTermostato")
                        );
                    }

                    if (cambios.containsKey("uriSwitch")) {
                        existente.setUriSwitch(
                                (String) cambios.get("uriSwitch")
                        );
                    }

                    return repository.save(existente);
                });
    }

    public List<Room> validar() {
        List<Room> habitaciones = repository.findAll();
        Set<Room> noCumplen = new LinkedHashSet<>();

        Map<String, List<Room>> porTermostato = new LinkedHashMap<>();
        Map<String, List<Room>> porUriSwitch = new LinkedHashMap<>();

        for (Room room : habitaciones) {
            boolean invalido = false;

            if (room.getIdTermostato() == null || room.getIdTermostato().isBlank()) {
                invalido = true;
            } else {
                porTermostato.computeIfAbsent(room.getIdTermostato(), k -> new ArrayList<>()).add(room);
            }

            if (!esUriValida(room.getUriSwitch())) {
                invalido = true;
            } else {
                porUriSwitch.computeIfAbsent(room.getUriSwitch(), k -> new ArrayList<>()).add(room);
            }

            if (invalido) {
                noCumplen.add(room);
            }
        }

        for (List<Room> grupo : porTermostato.values()) {
            if (grupo.size() > 1) {
                noCumplen.addAll(grupo);
            }
        }

        for (List<Room> grupo : porUriSwitch.values()) {
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
