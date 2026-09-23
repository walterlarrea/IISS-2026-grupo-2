package org.room.repository;
import java.util.Optional;
import org.room.Room;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RoomRepository extends MongoRepository<Room, String> {
    Optional<Room> findByIdTermostato(String idTermostato);
}
