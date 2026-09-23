package org.room.controller;

// import org.room.controller.HabitacionController;
// import org.example.service.HabitacionService;
import org.junit.jupiter.api.Test;
import org.room.Room;
import org.room.service.RoomService;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class HabitacionControllerTest {

    @Test
    void listarHabitacionesTest() {

        RoomService serviceMock = mock(RoomService.class);

        RoomController controller = new RoomController(serviceMock);

        List<Room> habitaciones = List.of(
            new Room("Dormitorio", 21.0, "termostato-01", "switch-01"),
            new Room("Sala", 22.0, "termostato-02", "switch-02")
        );

        when(serviceMock.listar()).thenReturn(habitaciones);

        List<Room> resultado = controller.listar();

        assertEquals(habitaciones, resultado);

        verify(serviceMock, times(1)).listar();
    }

    @Test
    void buscarPorIdTest() {

        RoomService serviceMock = mock(RoomService.class);

        RoomController controller = new RoomController(serviceMock);

        String id = "habitacion-01";

        Room habitacion = new Room(
                "Dormitorio",
                21.0,
                "termostato-01",
                "switch-01"
        );

        when(serviceMock.buscarPorId(id)).thenReturn(Optional.of(habitacion));

        ResponseEntity<Room> resultado = controller.buscarPorId(id);

        assertEquals(ResponseEntity.ok(habitacion), resultado);

        verify(serviceMock, times(1)).buscarPorId(id);
    }

    @Test
    void crearHabitacionTest() {

        RoomService serviceMock = mock(RoomService.class);

        RoomController controller = new RoomController(serviceMock);

        Room habitacion = new Room(
                "Dormitorio",
                21.0,
                "termostato-01",
                "switch-01"
        );

        when(serviceMock.crear(habitacion)).thenReturn(habitacion);

        Room resultado = controller.crear(habitacion);

        assertEquals(habitacion, resultado);

        verify(serviceMock, times(1)).crear(habitacion);
    }

    @Test
    void modificarHabitacionTest() {

        RoomService serviceMock = mock(RoomService.class);

        RoomController controller = new RoomController(serviceMock);

        String id = "habitacion-01";

        Room habitacion = new Room(
                "Dormitorio",
                21.0,
                "termostato-01",
                "switch-01"
        );

        when(serviceMock.modificar(id, habitacion)).thenReturn(Optional.of(habitacion));

        ResponseEntity<Room> resultado = controller.modificar(id, habitacion);

        assertEquals(ResponseEntity.ok(habitacion), resultado);

        verify(serviceMock, times(1)).modificar(id, habitacion);
    }

    @Test
    void eliminarHabitacionTest() {

        RoomService serviceMock = mock(RoomService.class);

        RoomController controller = new RoomController(serviceMock);

        String id = "habitacion-01";

        when(serviceMock.eliminar(id)).thenReturn(true);

        ResponseEntity<Void> resultado = controller.eliminar(id);

        assertEquals(ResponseEntity.noContent().build(), resultado);

        verify(serviceMock, times(1)).eliminar(id);
    }

    @Test
    void modificarParcialmenteHabitacionTest() {

        RoomService serviceMock = mock(RoomService.class);

        RoomController controller = new RoomController(serviceMock);

        String id = "habitacion-01";

        Map<String, Object> cambios = Map.of(
                "nombre", "Sala",
                "temperaturaEsperada", 22.0
        );

        Room habitacion = new Room(
                "Sala",
                22.0,
                "termostato-01",
                "switch-01"
        );

        when(serviceMock.modificarParcialmente(id, cambios)).thenReturn(Optional.of(habitacion));

        ResponseEntity<Room> resultado = controller.modificarParcialmente(id, cambios);

        assertEquals(ResponseEntity.ok(habitacion), resultado);

        verify(serviceMock, times(1)).modificarParcialmente(id, cambios);
    }

    @Test
    void eliminarHabitacionNoExistenteTest() {

        RoomService serviceMock = mock(RoomService.class);

        RoomController controller = new RoomController(serviceMock);

        String id = "habitacion-01";

        when(serviceMock.eliminar(id)).thenReturn(false);

        ResponseEntity<Void> resultado = controller.eliminar(id);

        assertEquals(ResponseEntity.notFound().build(), resultado);

        verify(serviceMock, times(1)).eliminar(id);
    }

    @Test
    void buscarPorIdNoExistenteTest() {

        RoomService serviceMock = mock(RoomService.class);

        RoomController controller = new RoomController(serviceMock);

        String id = "habitacion-01";

        when(serviceMock.buscarPorId(id)).thenReturn(Optional.empty());

        ResponseEntity<Room> resultado = controller.buscarPorId(id);

        assertEquals(ResponseEntity.notFound().build(), resultado);

        verify(serviceMock, times(1)).buscarPorId(id);
    }

    @Test
    void modificarHabitacionNoExistenteTest() {

        RoomService serviceMock = mock(RoomService.class);

        RoomController controller = new RoomController(serviceMock);

        String id = "habitacion-01";

        Room habitacion = new Room(
                "Dormitorio",
                21.0,
                "termostato-01",
                "switch-01"
        );

        when(serviceMock.modificar(id, habitacion)).thenReturn(Optional.empty());

        ResponseEntity<Room> resultado = controller.modificar(id, habitacion);

        assertEquals(ResponseEntity.notFound().build(), resultado);

        verify(serviceMock, times(1)).modificar(id, habitacion);
    }

    @Test
    void modificarParcialmenteHabitacionNoExistenteTest() {

        RoomService serviceMock = mock(RoomService.class);

        RoomController controller = new RoomController(serviceMock);

        String id = "habitacion-01";

        Map<String, Object> cambios = Map.of(
                "nombre", "Sala",
                "temperaturaEsperada", 22.0
        );

        when(serviceMock.modificarParcialmente(id, cambios)).thenReturn(Optional.empty());

        ResponseEntity<Room> resultado = controller.modificarParcialmente(id, cambios);

        assertEquals(ResponseEntity.notFound().build(), resultado);

        verify(serviceMock, times(1)).modificarParcialmente(id, cambios);
    }
}