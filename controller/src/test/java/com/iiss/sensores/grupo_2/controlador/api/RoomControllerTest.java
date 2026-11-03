package com.iiss.sensores.grupo_2.controlador.api;

import com.iiss.sensores.grupo_2.controlador.api.controller.RoomController;
import com.iiss.sensores.grupo_2.controlador.api.model.Inventory;
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

/** Verifica que los endpoints deleguen operaciones al servicio de habitaciones. */
class RoomControllerTest {

  private RoomService service;
  private RoomController controller;

  /** Prepara un servicio simulado y el controlador bajo prueba. */
  @BeforeEach
  void setUp() {
    service = mock(RoomService.class);
    controller = new RoomController(service);
  }

  /** Comprueba que el endpoint de listado devuelva el resultado del servicio. */
  @Test
  void listarHabitacionesTest() {
    List<Inventory.Habitacion> habitaciones = List.of(habitacion("dormitorio"));
    when(service.listar()).thenReturn(habitaciones);

    assertEquals(habitaciones, controller.listar());
    verify(service).listar();
  }

  /** Comprueba que el endpoint de validación devuelva los resultados del servicio. */
  @Test
  void validarHabitacionesTest() {
    List<Inventory.Habitacion> habitaciones = List.of(habitacion("dormitorio"));
    when(service.validar()).thenReturn(habitaciones);

    assertEquals(habitaciones, controller.validar());
    verify(service).validar();
  }

  /** Comprueba la respuesta exitosa al buscar una habitación existente. */
  @Test
  void buscarPorIdTest() {
    String id = "dormitorio";
    Inventory.Habitacion habitacion = habitacion(id);
    when(service.buscarPorId(id)).thenReturn(Optional.of(habitacion));

    assertEquals(ResponseEntity.ok(habitacion), controller.buscarPorId(id));
    verify(service).buscarPorId(id);
  }

  /** Comprueba la respuesta 404 al buscar una habitación inexistente. */
  @Test
  void buscarPorIdNoExistenteTest() {
    String id = "inexistente";
    when(service.buscarPorId(id)).thenReturn(Optional.empty());

    assertEquals(ResponseEntity.notFound().build(), controller.buscarPorId(id));
    verify(service).buscarPorId(id);
  }

  /** Comprueba la respuesta exitosa al buscar por identificador de termostato. */
  @Test
  void buscarPorTermostatoTest() {
    String idTermostato = "termostato-01";
    Inventory.Habitacion habitacion = habitacion("dormitorio");
    when(service.obtenerPorTermostato(idTermostato)).thenReturn(Optional.of(habitacion));

    assertEquals(ResponseEntity.ok(habitacion), controller.buscarPorTermostato(idTermostato));
    verify(service).obtenerPorTermostato(idTermostato);
  }

  /** Comprueba que el endpoint de creación delegue la habitación al servicio. */
  @Test
  void crearHabitacionTest() {
    Inventory.Habitacion habitacion = habitacion("dormitorio");
    when(service.crear(habitacion)).thenReturn(habitacion);

    assertEquals(habitacion, controller.crear(habitacion));
    verify(service).crear(habitacion);
  }

  /** Comprueba la respuesta exitosa al modificar una habitación existente. */
  @Test
  void modificarHabitacionTest() {
    String id = "dormitorio";
    Inventory.Habitacion habitacion = habitacion(id);
    when(service.modificar(id, habitacion)).thenReturn(Optional.of(habitacion));

    assertEquals(ResponseEntity.ok(habitacion), controller.modificar(id, habitacion));
    verify(service).modificar(id, habitacion);
  }

  /** Comprueba la respuesta 404 al modificar una habitación inexistente. */
  @Test
  void modificarHabitacionNoExistenteTest() {
    String id = "inexistente";
    Inventory.Habitacion habitacion = habitacion(id);
    when(service.modificar(id, habitacion)).thenReturn(Optional.empty());

    assertEquals(ResponseEntity.notFound().build(), controller.modificar(id, habitacion));
    verify(service).modificar(id, habitacion);
  }

  /** Comprueba que la eliminación exitosa responda sin contenido. */
  @Test
  void eliminarHabitacionTest() {
    String id = "dormitorio";
    when(service.eliminar(id)).thenReturn(true);

    assertEquals(ResponseEntity.noContent().build(), controller.eliminar(id));
    verify(service).eliminar(id);
  }

  /** Comprueba que eliminar una habitación inexistente responda 404. */
  @Test
  void eliminarHabitacionNoExistenteTest() {
    String id = "inexistente";
    when(service.eliminar(id)).thenReturn(false);

    assertEquals(ResponseEntity.notFound().build(), controller.eliminar(id));
    verify(service).eliminar(id);
  }

  /** Comprueba la respuesta exitosa de una actualización parcial. */
  @Test
  void modificarParcialmenteHabitacionTest() {
    String id = "dormitorio";
    Map<String, Object> cambios = Map.of("nombre", "Sala", "temperaturaEsperada", 22.0);
    Inventory.Habitacion habitacion = habitacion(id);
    when(service.modificarParcialmente(id, cambios)).thenReturn(Optional.of(habitacion));

    assertEquals(ResponseEntity.ok(habitacion), controller.modificarParcialmente(id, cambios));
    verify(service).modificarParcialmente(id, cambios);
  }

  /** Comprueba la respuesta 404 de una actualización parcial sin coincidencia. */
  @Test
  void modificarParcialmenteHabitacionNoExistenteTest() {
    String id = "inexistente";
    Map<String, Object> cambios = Map.of("nombre", "Sala");
    when(service.modificarParcialmente(id, cambios)).thenReturn(Optional.empty());

    assertEquals(ResponseEntity.notFound().build(), controller.modificarParcialmente(id, cambios));
    verify(service).modificarParcialmente(id, cambios);
  }

  /** Crea una habitación de prueba con configuración completa. */
  private static Inventory.Habitacion habitacion(String id) {
    return new Inventory.Habitacion(
        id,
        "Dormitorio",
        21.0,
        1.5,
        "termostato-01",
        "termostato/topic",
        "switch-01",
        "http://switches-api:8090/switches/1");
  }
}
