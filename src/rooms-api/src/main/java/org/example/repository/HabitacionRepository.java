package org.example.repository;

import org.example.Habitacion;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface HabitacionRepository extends MongoRepository<Habitacion, String> {
}
