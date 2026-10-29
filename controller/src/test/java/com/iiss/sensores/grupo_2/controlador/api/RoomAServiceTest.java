package com.iiss.sensores.grupo_2.controlador.api;

import com.iiss.sensores.grupo_2.controlador.api.model.Inventory;
import com.iiss.sensores.grupo_2.controlador.api.service.InventoryService;
import com.iiss.sensores.grupo_2.controlador.api.service.RoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoomAServiceTest {

  private InventoryService inventoryService;
  private RoomService service;

  @BeforeEach
  void setUp() {
    inventoryService = mock(InventoryService.class);
    service = new RoomService(inventoryService);
  }

  @Test
  void crearHabitacionLaAgregaAlInventario() {
    Inventory inventory = inventario(List.of(habitacion("sala")));
    Inventory.Habitacion nueva = habitacion("dormitorio");
    when(inventoryService.obtenerActivo()).thenReturn(Optional.of(inventory));

    assertEquals(nueva, service.crear(nueva));

    ArgumentCaptor<Inventory> captor = ArgumentCaptor.forClass(Inventory.class);
    verify(inventoryService).reemplazar(captor.capture());
    assertEquals(List.of(habitacion("sala"), nueva), captor.getValue().habitaciones());
  }

  @Test
  void crearHabitacionConIdExistenteNoModificaInventario() {
    Inventory.Habitacion existente = habitacion("sala");
    Inventory inventory = inventario(List.of(existente));
    when(inventoryService.obtenerActivo()).thenReturn(Optional.of(inventory));

    assertEquals(existente, service.crear(habitacion("sala")));

    verify(inventoryService, never()).reemplazar(any());
  }

  @Test
  void listarHabitacionesDelInventarioActivo() {
    List<Inventory.Habitacion> habitaciones = List.of(habitacion("sala"));
    when(inventoryService.obtenerActivo()).thenReturn(Optional.of(inventario(habitaciones)));

    assertEquals(habitaciones, service.listar());
  }

  @Test
  void buscarPorIdDevuelveHabitacionDelInventario() {
    Inventory.Habitacion habitacion = habitacion("sala");
    when(inventoryService.obtenerActivo()).thenReturn(Optional.of(inventario(List.of(habitacion))));

    assertEquals(Optional.of(habitacion), service.buscarPorId("sala"));
    assertTrue(service.buscarPorId("inexistente").isEmpty());
  }

  @Test
  void eliminarHabitacionLaQuitaDelInventario() {
    Inventory inventory = inventario(List.of(habitacion("sala"), habitacion("dormitorio")));
    when(inventoryService.obtenerActivo()).thenReturn(Optional.of(inventory));

    assertTrue(service.eliminar("sala"));

    ArgumentCaptor<Inventory> captor = ArgumentCaptor.forClass(Inventory.class);
    verify(inventoryService).reemplazar(captor.capture());
    assertEquals(List.of(habitacion("dormitorio")), captor.getValue().habitaciones());
  }

  @Test
  void eliminarHabitacionInexistenteNoModificaInventario() {
    when(inventoryService.obtenerActivo())
        .thenReturn(Optional.of(inventario(List.of(habitacion("sala")))));

    assertFalse(service.eliminar("inexistente"));

    verify(inventoryService, never()).reemplazar(any());
  }

  @Test
  void obtenerPorTermostatoBuscaEnInventario() {
    Inventory.Habitacion habitacion = habitacion("sala");
    when(inventoryService.obtenerActivo()).thenReturn(Optional.of(inventario(List.of(habitacion))));

    assertEquals(Optional.of(habitacion), service.obtenerPorTermostato("termostato-sala"));
    assertTrue(service.obtenerPorTermostato("inexistente").isEmpty());
  }

  @Test
  void modificarHabitacionReemplazaDatosYConservaElIdDeLaRuta() {
    Inventory inventory = inventario(List.of(habitacion("sala")));
    Inventory.Habitacion modificada = habitacion("otro-id");
    when(inventoryService.obtenerActivo()).thenReturn(Optional.of(inventory));

    Optional<Inventory.Habitacion> resultado = service.modificar("sala", modificada);

    assertTrue(resultado.isPresent());
    assertEquals(habitacion("sala").id(), resultado.get().id());
    assertEquals(modificada.nombre(), resultado.get().nombre());
    assertEquals(modificada.temperaturaEsperada(), resultado.get().temperaturaEsperada());
    assertEquals(modificada.idTermostato(), resultado.get().idTermostato());
    ArgumentCaptor<Inventory> captor = ArgumentCaptor.forClass(Inventory.class);
    verify(inventoryService).reemplazar(captor.capture());
    assertEquals(List.of(resultado.get()), captor.getValue().habitaciones());
  }

  @Test
  void modificarHabitacionInexistenteDevuelveVacio() {
    when(inventoryService.obtenerActivo())
        .thenReturn(Optional.of(inventario(List.of(habitacion("sala")))));

    assertTrue(service.modificar("inexistente", habitacion("inexistente")).isEmpty());

    verify(inventoryService, never()).reemplazar(any());
  }

  @Test
  void modificarParcialmenteConservaCamposNoModificados() {
    Inventory.Habitacion original = habitacion("sala");
    when(inventoryService.obtenerActivo()).thenReturn(Optional.of(inventario(List.of(original))));
    Map<String, Object> cambios = Map.of("nombre", "Estar", "temperaturaEsperada", 23.0);

    Inventory.Habitacion resultado = service.modificarParcialmente("sala", cambios).orElseThrow();

    assertEquals("Estar", resultado.nombre());
    assertEquals(23.0, resultado.temperaturaEsperada());
    assertEquals(original.idTermostato(), resultado.idTermostato());
    assertEquals(original.urlSwitch(), resultado.urlSwitch());
    verify(inventoryService).reemplazar(any());
  }

  @Test
  void modificarParcialmenteHabitacionInexistenteDevuelveVacio() {
    when(inventoryService.obtenerActivo())
        .thenReturn(Optional.of(inventario(List.of(habitacion("sala")))));

    assertTrue(service.modificarParcialmente("inexistente", Map.of("nombre", "Estar")).isEmpty());

    verify(inventoryService, never()).reemplazar(any());
  }

  @Test
  void validarDetectaHabitacionesConTermostatoDuplicado() {
    Inventory.Habitacion sala = habitacion("sala");
    Inventory.Habitacion dormitorio =
        new Inventory.Habitacion(
            "dormitorio",
            "Dormitorio",
            21.0,
            1.5,
            sala.idTermostato(),
            "termostato/dormitorio",
            "switch-dormitorio",
            "http://switches-api:8090/switches/dormitorio");
    when(inventoryService.obtenerActivo())
        .thenReturn(Optional.of(inventario(List.of(sala, dormitorio))));

    assertEquals(List.of(sala, dormitorio), service.validar());
  }

  private static Inventory inventario(List<Inventory.Habitacion> habitaciones) {
    return new Inventory(
        "2.0",
        new Inventory.Sitio(
            "casa",
            "Casa",
            5.0,
            new Inventory.Tarifa(new Inventory.Franja("17:00", "23:00", Inventory.Dias.HABILES))),
        habitaciones);
  }

  private static Inventory.Habitacion habitacion(String id) {
    return new Inventory.Habitacion(
        id,
        "Habitación " + id,
        21.0,
        1.5,
        "termostato-" + id,
        "termostato/" + id,
        "switch-" + id,
        "http://switches-api:8090/switches/" + id);
  }
}
