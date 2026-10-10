package com.iiss.sensores.grupo_2.room.repository;
import java.util.Optional;
import com.iiss.sensores.grupo_2.room.Room;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RoomRepository extends MongoRepository<Room, String> {
    Optional<Room> findByIdTermostato(String idTermostato);
}
