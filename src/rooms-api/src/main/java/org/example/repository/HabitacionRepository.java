package org.example.repository;
import java.util.Optional;
import org.example.Habitacion;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface HabitacionRepository extends MongoRepository<Habitacion, String> {
    Optional<Habitacion> findByIdTermostato(String idTermostato);
}
