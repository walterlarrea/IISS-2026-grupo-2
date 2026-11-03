package com.iiss.sensores.grupo_2.controlador.api.repository;

import com.iiss.sensores.grupo_2.controlador.api.model.Room;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Acceso a las habitaciones persistidas en MongoDB. */
@Repository
public interface RoomRepository extends MongoRepository<Room, String> {
  /** Busca una habitación por el identificador de su termostato. */
  Optional<Room> findByIdTermostato(String idTermostato);
}
