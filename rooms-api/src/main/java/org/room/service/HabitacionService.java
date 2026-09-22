package org.room.service;

import org.room.Room;
import org.room.repository.RoomRepository;
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
public class HabitacionService {

    private final RoomRepository repository;

    public HabitacionService(RoomRepository repository) {
        this.repository = repository;
    }

    public List<Room> listar() {
        return repository.findAll();
    }

    public Optional<Room> buscarPorId(String id) {
        return repository.findById(id);
    }

    public Room crear(Room room) {
        return repository.save(room);
    }

    public Optional<Room> modificar(String id, Room room) {
        return repository.findById(id).map(existente -> {
                    existente.setNombre(room.getNombre());
                    existente.setTemperaturaEsperada(room.getTemperaturaEsperada());
                    existente.setIdTermostato(room.getIdTermostato());
                    existente.setUriSwitch(room.getUriSwitch());

                    return repository.save(existente);
        });
    }

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
