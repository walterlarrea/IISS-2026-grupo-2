package org.room.service;

import org.junit.jupiter.api.Test;
import org.room.Room;
import org.room.repository.RoomRepository;
import org.springframework.http.ResponseEntity;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class HabitacionServiceTest {

    @Test
    void crearHabitacionTest() {

        RoomRepository repositoryMock = mock(RoomRepository.class);

        RoomService service = new RoomService(repositoryMock);

        Room habitacion = new Room(
                "Dormitorio",
                21.0,
                "termostato-01",
                "switch-01"
        );

        service.crear(habitacion);

        verify(repositoryMock, times(1)).save(habitacion);
    }

    @Test
    void listarHabitacionesTest() {

        RoomRepository repositoryMock = mock(RoomRepository.class);

        RoomService service = new RoomService(repositoryMock);

        List<Room> habitaciones = List.of(
            new Room("Dormitorio", 21.0, "termostato-01", "switch-01"),
            new Room("Sala", 22.0, "termostato-02", "switch-02")
        );

        when(repositoryMock.findAll()).thenReturn(habitaciones);

        List<Room> resultado = service.listar();

        assertEquals(habitaciones, resultado);

        verify(repositoryMock, times(1)).findAll();
    }

    @Test
    void buscarPorIdTest() {

        RoomRepository repositoryMock = mock(RoomRepository.class);

        RoomService service = new RoomService(repositoryMock);

        String id = "habitacion-01";

        Room habitacion = new Room(
                "Dormitorio",
                21.0,
                "termostato-01",
                "switch-01"
        );

        when(repositoryMock.findById(id)).thenReturn(java.util.Optional.of(habitacion));

        java.util.Optional<Room> resultado = service.buscarPorId(id);

        assertEquals(java.util.Optional.of(habitacion), resultado);

        verify(repositoryMock, times(1)).findById(id);
    }

    @Test
    void eliminarHabitacionTest() {

        RoomRepository repositoryMock = mock(RoomRepository.class);

        RoomService service = new RoomService(repositoryMock);

        String id = "habitacion-01";

        when(repositoryMock.existsById(id)).thenReturn(true);

        boolean resultado = service.eliminar(id);

        assertEquals(true, resultado);

        verify(repositoryMock, times(1)).existsById(id);
        verify(repositoryMock, times(1)).deleteById(id);
    }

    @Test
    void eliminarHabitacionNoExistenteTest() {
        
        RoomRepository repositoryMock = mock(RoomRepository.class);

        RoomService service = new RoomService(repositoryMock);

        String id = "habitacion-01";

        when(repositoryMock.existsById(id)).thenReturn(false);

        boolean resultado = service.eliminar(id);

        assertEquals(false, resultado);

        verify(repositoryMock, times(1)).existsById(id);
        verify(repositoryMock, times(0)).deleteById(id);
    }

    @Test
    void buscarPorIdTermostatoTest() {
        
        RoomRepository repositoryMock = mock(RoomRepository.class);

        RoomService service = new RoomService(repositoryMock);

        String idTermostato = "termostato-01";

        Room habitacion = new Room(
                "Dormitorio",
                21.0,
                idTermostato,
                "switch-01"
        );

        when(repositoryMock.findByIdTermostato(idTermostato)).thenReturn(java.util.Optional.of(habitacion));

        java.util.Optional<Room> resultado = service.buscarPorIdTermostato(idTermostato);

        assertEquals(java.util.Optional.of(habitacion), resultado);

        verify(repositoryMock, times(1)).findByIdTermostato(idTermostato);
    }

    @Test
    void modificarParcialmenteTest() {

    RoomRepository repositoryMock = mock(RoomRepository.class);

    RoomService service = new RoomService(repositoryMock);

    String id = "habitacion-01";

    Room habitacionExistente = new Room(
            "Dormitorio",
            21.0,
            "termostato-01",
            "switch-01"
    );

    when(repositoryMock.findById(id))
            .thenReturn(java.util.Optional.of(habitacionExistente));

    when(repositoryMock.save(habitacionExistente))
            .thenReturn(habitacionExistente);

    java.util.Map<String, Object> cambios = java.util.Map.of(
        "nombre", "Sala",
        "temperaturaEsperada", 22.0
    );

    java.util.Optional<Room> resultado =
            service.modificarParcialmente(id, cambios);

    assertEquals("Sala", resultado.get().getNombre());
    assertEquals(22.0, resultado.get().getTemperaturaEsperada());

    verify(repositoryMock, times(1)).findById(id);
    verify(repositoryMock, times(1)).save(habitacionExistente);
}

    @Test
    void modificarParcialmenteNoExistenteTest() {
        
        RoomRepository repositoryMock = mock(RoomRepository.class);

        RoomService service = new RoomService(repositoryMock);

        String id = "habitacion-01";

        when(repositoryMock.findById(id)).thenReturn(java.util.Optional.empty());

        java.util.Map<String, Object> cambios = java.util.Map.of(
            "nombre", "Sala",
            "temperaturaEsperada", 22.0
        );

        java.util.Optional<Room> resultado = service.modificarParcialmente(id, cambios);

        assertEquals(java.util.Optional.empty(), resultado);

        verify(repositoryMock, times(1)).findById(id);
        verify(repositoryMock, times(0)).save(any());
    }

    @Test
    void modificarTest(){

        RoomRepository repositoryMock = mock(RoomRepository.class);

        RoomService service = new RoomService(repositoryMock);

        String id = "habitacion-01";

            Room habitacionExistente = new Room(
                "Dormitorio",
                21.0,
                "termostato-01",
                "switch-01"
            );

            when(repositoryMock.findById(id)).thenReturn(java.util.Optional.of(habitacionExistente));

            Room habitacionModificada = new Room(
                "Sala",
                22.0,
                "termostato-02",
                "switch-02"
            );

            when(repositoryMock.save(habitacionExistente)).thenReturn(habitacionExistente);

            java.util.Optional<Room> resultado = service.modificar(id, habitacionModificada);

            assertEquals("Sala", resultado.get().getNombre());
            assertEquals(22.0, resultado.get().getTemperaturaEsperada());
            assertEquals("termostato-02", resultado.get().getIdTermostato());
            assertEquals("switch-02", resultado.get().getUriSwitch());

            verify(repositoryMock, times(1)).findById(id);
            verify(repositoryMock, times(1)).save(habitacionExistente);
        }
}
