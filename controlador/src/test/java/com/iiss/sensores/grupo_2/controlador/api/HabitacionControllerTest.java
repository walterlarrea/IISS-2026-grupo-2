package com.iiss.sensores.grupo_2.controlador.api;

import com.iiss.sensores.grupo_2.controlador.api.controller.RoomController;
import com.iiss.sensores.grupo_2.controlador.api.model.Inventario;
import com.iiss.sensores.grupo_2.controlador.api.service.RoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HabitacionControllerTest {

    private RoomService service;
    private RoomController controller;

    @BeforeEach
    void setUp() {
        service = mock(RoomService.class);
        controller = new RoomController(service);
    }

    @Test
    void listarHabitacionesTest() {
        List<Inventario.Habitacion> habitaciones = List.of(habitacion("dormitorio"));
        when(service.listar()).thenReturn(habitaciones);

        assertEquals(habitaciones, controller.listar());
        verify(service).listar();
    }

    @Test
    void validarHabitacionesTest() {
        List<Inventario.Habitacion> habitaciones = List.of(habitacion("dormitorio"));
        when(service.validar()).thenReturn(habitaciones);

        assertEquals(habitaciones, controller.validar());
        verify(service).validar();
    }

    @Test
    void buscarPorIdTest() {
        String id = "dormitorio";
        Inventario.Habitacion habitacion = habitacion(id);
        when(service.buscarPorId(id)).thenReturn(Optional.of(habitacion));

        assertEquals(ResponseEntity.ok(habitacion), controller.buscarPorId(id));
        verify(service).buscarPorId(id);
    }

    @Test
    void buscarPorIdNoExistenteTest() {
        String id = "inexistente";
        when(service.buscarPorId(id)).thenReturn(Optional.empty());

        assertEquals(ResponseEntity.notFound().build(), controller.buscarPorId(id));
        verify(service).buscarPorId(id);
    }

    @Test
    void buscarPorTermostatoTest() {
        String idTermostato = "termostato-01";
        Inventario.Habitacion habitacion = habitacion("dormitorio");
        when(service.obtenerPorTermostato(idTermostato)).thenReturn(Optional.of(habitacion));

        assertEquals(ResponseEntity.ok(habitacion), controller.buscarPorTermostato(idTermostato));
        verify(service).obtenerPorTermostato(idTermostato);
    }

    @Test
    void crearHabitacionTest() {
        Inventario.Habitacion habitacion = habitacion("dormitorio");
        when(service.crear(habitacion)).thenReturn(habitacion);

        assertEquals(habitacion, controller.crear(habitacion));
        verify(service).crear(habitacion);
    }

    @Test
    void modificarHabitacionTest() {
        String id = "dormitorio";
        Inventario.Habitacion habitacion = habitacion(id);
        when(service.modificar(id, habitacion)).thenReturn(Optional.of(habitacion));

        assertEquals(ResponseEntity.ok(habitacion), controller.modificar(id, habitacion));
        verify(service).modificar(id, habitacion);
    }

    @Test
    void modificarHabitacionNoExistenteTest() {
        String id = "inexistente";
        Inventario.Habitacion habitacion = habitacion(id);
        when(service.modificar(id, habitacion)).thenReturn(Optional.empty());

        assertEquals(ResponseEntity.notFound().build(), controller.modificar(id, habitacion));
        verify(service).modificar(id, habitacion);
    }

    @Test
    void eliminarHabitacionTest() {
        String id = "dormitorio";
        when(service.eliminar(id)).thenReturn(true);

        assertEquals(ResponseEntity.noContent().build(), controller.eliminar(id));
        verify(service).eliminar(id);
    }

    @Test
    void eliminarHabitacionNoExistenteTest() {
        String id = "inexistente";
        when(service.eliminar(id)).thenReturn(false);

        assertEquals(ResponseEntity.notFound().build(), controller.eliminar(id));
        verify(service).eliminar(id);
    }

    @Test
    void modificarParcialmenteHabitacionTest() {
        String id = "dormitorio";
        Map<String, Object> cambios = Map.of("nombre", "Sala", "temperaturaEsperada", 22.0);
        Inventario.Habitacion habitacion = habitacion(id);
        when(service.modificarParcialmente(id, cambios)).thenReturn(Optional.of(habitacion));

        assertEquals(ResponseEntity.ok(habitacion), controller.modificarParcialmente(id, cambios));
        verify(service).modificarParcialmente(id, cambios);
    }

    @Test
    void modificarParcialmenteHabitacionNoExistenteTest() {
        String id = "inexistente";
        Map<String, Object> cambios = Map.of("nombre", "Sala");
        when(service.modificarParcialmente(id, cambios)).thenReturn(Optional.empty());

        assertEquals(ResponseEntity.notFound().build(), controller.modificarParcialmente(id, cambios));
        verify(service).modificarParcialmente(id, cambios);
    }

    private static Inventario.Habitacion habitacion(String id) {
        return new Inventario.Habitacion(
                id,
                "Dormitorio",
                21.0,
                1.5,
                "termostato-01",
                "termostato/topic",
                "switch-01",
                "http://switches-api:8090/switches/1"
        );
    }
}
