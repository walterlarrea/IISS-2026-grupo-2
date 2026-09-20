package org.example;
import org.example.controller.HabitacionController;

import org.example.service.HabitacionService;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class HabitacionControllerTest {

    @Test
    void listarHabitacionesTest() {

        HabitacionService serviceMock = mock(HabitacionService.class);

        HabitacionController controller = new HabitacionController(serviceMock);

        List<Habitacion> habitaciones = List.of(
            new Habitacion("Dormitorio", 21.0, "termostato-01", "switch-01"),
            new Habitacion("Sala", 22.0, "termostato-02", "switch-02")
        );

        when(serviceMock.listar()).thenReturn(habitaciones);

        List<Habitacion> resultado = controller.listar();

        assertEquals(habitaciones, resultado);

        verify(serviceMock, times(1)).listar();
    }

    @Test
    void buscarPorIdTest() {

        HabitacionService serviceMock = mock(HabitacionService.class);

        HabitacionController controller = new HabitacionController(serviceMock);

        String id = "habitacion-01";

        Habitacion habitacion = new Habitacion(
                "Dormitorio",
                21.0,
                "termostato-01",
                "switch-01"
        );

        when(serviceMock.buscarPorId(id)).thenReturn(Optional.of(habitacion));

        ResponseEntity<Habitacion> resultado = controller.buscarPorId(id);

        assertEquals(ResponseEntity.ok(habitacion), resultado);

        verify(serviceMock, times(1)).buscarPorId(id);
    }

    @Test
    void crearHabitacionTest() {

        HabitacionService serviceMock = mock(HabitacionService.class);

        HabitacionController controller = new HabitacionController(serviceMock);

        Habitacion habitacion = new Habitacion(
                "Dormitorio",
                21.0,
                "termostato-01",
                "switch-01"
        );

        when(serviceMock.crear(habitacion)).thenReturn(habitacion);

        Habitacion resultado = controller.crear(habitacion);

        assertEquals(habitacion, resultado);

        verify(serviceMock, times(1)).crear(habitacion);
    }

    @Test
    void modificarHabitacionTest() {

        HabitacionService serviceMock = mock(HabitacionService.class);

        HabitacionController controller = new HabitacionController(serviceMock);

        String id = "habitacion-01";

        Habitacion habitacion = new Habitacion(
                "Dormitorio",
                21.0,
                "termostato-01",
                "switch-01"
        );

        when(serviceMock.modificar(id, habitacion)).thenReturn(Optional.of(habitacion));

        ResponseEntity<Habitacion> resultado = controller.modificar(id, habitacion);

        assertEquals(ResponseEntity.ok(habitacion), resultado);

        verify(serviceMock, times(1)).modificar(id, habitacion);
    }

    @Test
    void eliminarHabitacionTest() {

        HabitacionService serviceMock = mock(HabitacionService.class);

        HabitacionController controller = new HabitacionController(serviceMock);

        String id = "habitacion-01";

        when(serviceMock.eliminar(id)).thenReturn(true);

        ResponseEntity<Void> resultado = controller.eliminar(id);

        assertEquals(ResponseEntity.noContent().build(), resultado);

        verify(serviceMock, times(1)).eliminar(id);
    }

    @Test
    void modificarParcialmenteHabitacionTest() {

        HabitacionService serviceMock = mock(HabitacionService.class);

        HabitacionController controller = new HabitacionController(serviceMock);

        String id = "habitacion-01";

        Map<String, Object> cambios = Map.of(
                "nombre", "Sala",
                "temperaturaEsperada", 22.0
        );

        Habitacion habitacion = new Habitacion(
                "Sala",
                22.0,
                "termostato-01",
                "switch-01"
        );

        when(serviceMock.modificarParcialmente(id, cambios)).thenReturn(Optional.of(habitacion));

        ResponseEntity<Habitacion> resultado = controller.modificarParcialmente(id, cambios);

        assertEquals(ResponseEntity.ok(habitacion), resultado);

        verify(serviceMock, times(1)).modificarParcialmente(id, cambios);
    }

    @Test
    void eliminarHabitacionNoExistenteTest() {

        HabitacionService serviceMock = mock(HabitacionService.class);

        HabitacionController controller = new HabitacionController(serviceMock);

        String id = "habitacion-01";

        when(serviceMock.eliminar(id)).thenReturn(false);

        ResponseEntity<Void> resultado = controller.eliminar(id);

        assertEquals(ResponseEntity.notFound().build(), resultado);

        verify(serviceMock, times(1)).eliminar(id);
    }

    @Test
    void buscarPorIdNoExistenteTest() {

        HabitacionService serviceMock = mock(HabitacionService.class);

        HabitacionController controller = new HabitacionController(serviceMock);

        String id = "habitacion-01";

        when(serviceMock.buscarPorId(id)).thenReturn(Optional.empty());

        ResponseEntity<Habitacion> resultado = controller.buscarPorId(id);

        assertEquals(ResponseEntity.notFound().build(), resultado);

        verify(serviceMock, times(1)).buscarPorId(id);
    }

    @Test
    void modificarHabitacionNoExistenteTest() {

        HabitacionService serviceMock = mock(HabitacionService.class);

        HabitacionController controller = new HabitacionController(serviceMock);

        String id = "habitacion-01";

        Habitacion habitacion = new Habitacion(
                "Dormitorio",
                21.0,
                "termostato-01",
                "switch-01"
        );

        when(serviceMock.modificar(id, habitacion)).thenReturn(Optional.empty());

        ResponseEntity<Habitacion> resultado = controller.modificar(id, habitacion);

        assertEquals(ResponseEntity.notFound().build(), resultado);

        verify(serviceMock, times(1)).modificar(id, habitacion);
    }

    @Test
    void modificarParcialmenteHabitacionNoExistenteTest() {

        HabitacionService serviceMock = mock(HabitacionService.class);

        HabitacionController controller = new HabitacionController(serviceMock);

        String id = "habitacion-01";

        Map<String, Object> cambios = Map.of(
                "nombre", "Sala",
                "temperaturaEsperada", 22.0
        );

        when(serviceMock.modificarParcialmente(id, cambios)).thenReturn(Optional.empty());

        ResponseEntity<Habitacion> resultado = controller.modificarParcialmente(id, cambios);

        assertEquals(ResponseEntity.notFound().build(), resultado);

        verify(serviceMock, times(1)).modificarParcialmente(id, cambios);
    }
}
