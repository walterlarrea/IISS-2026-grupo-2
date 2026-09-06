package org.example.service;

import org.example.Habitacion;
import org.example.repository.HabitacionRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class HabitacionService {

    private final HabitacionRepository repository;

    public HabitacionService(HabitacionRepository repository) {
        this.repository = repository;
    }

    public List<Habitacion> listar() {
        return repository.findAll();
    }

    public Optional<Habitacion> buscarPorId(String id) {
        return repository.findById(id);
    }

    public Habitacion crear(Habitacion habitacion) {
        return repository.save(habitacion);
    }

    public Optional<Habitacion> modificar(String id, Habitacion habitacion) {
        return repository.findById(id)
                .map(existente -> {
                    existente.setNombre(habitacion.getNombre());
                    existente.setTemperaturaEsperada(habitacion.getTemperaturaEsperada());
                    existente.setIdTermostato(habitacion.getIdTermostato());
                    existente.setIdSwitch(habitacion.getIdSwitch());

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
}
