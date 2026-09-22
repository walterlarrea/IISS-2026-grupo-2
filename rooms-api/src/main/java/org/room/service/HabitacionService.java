package org.room.service;

import org.room.Room;
import org.room.repository.RoomRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
                    existente.setIdSwitch(room.getIdSwitch());

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

                if (cambios.containsKey("idSwitch")) {
                    existente.setIdSwitch(
                            (String) cambios.get("idSwitch")
                    );
                }

                return repository.save(existente);
            });
    }
}
