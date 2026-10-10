package com.iiss.sensores.grupo_2.controlador.api.repository;

import com.iiss.sensores.grupo_2.controlador.api.model.Room;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoomRepository extends MongoRepository<Room, String> {
    Optional<Room> findByIdTermostato(String idTermostato);
}
