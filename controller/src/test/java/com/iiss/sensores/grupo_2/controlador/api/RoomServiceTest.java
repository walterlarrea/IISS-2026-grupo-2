package com.iiss.sensores.grupo_2.controlador.api;

import com.iiss.sensores.grupo_2.controlador.api.model.Inventory;
import com.iiss.sensores.grupo_2.controlador.api.service.InventoryService;
import com.iiss.sensores.grupo_2.controlador.api.service.RoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoomServiceTest {

  private InventoryService inventoryService;
  private RoomService service;

  @BeforeEach
  void setUp() {
    inventoryService = mock(InventoryService.class);
    service = new RoomService(inventoryService);
  }

  @Test
  @DisplayName("Devuelve lista vacía si el inventory no contiene habitaciones")
  void validarColeccionVacia() {
    when(inventoryService.obtenerActivo()).thenReturn(Optional.of(inventario(List.of())));

    assertTrue(service.validar().isEmpty());
  }

  @Test
  @DisplayName("Devuelve lista vacía cuando las habitaciones son válidas y no están duplicadas")
  void validarTodasValidas() {
    Inventory.Habitacion living = habitacion("1", "termo-1", "http://switches-api:8090/switches/1");
    Inventory.Habitacion cocina = habitacion("2", "termo-2", "http://switches-api:8090/switches/2");
    when(inventoryService.obtenerActivo())
        .thenReturn(Optional.of(inventario(List.of(living, cocina))));

    assertTrue(service.validar().isEmpty());
  }

  @Test
  @DisplayName("Detecta habitaciones con idTermostato nulo o vacío")
  void validarIdTermostatoInvalido() {
    Inventory.Habitacion nulo = habitacion("1", null, "http://switches-api:8090/switches/1");
    Inventory.Habitacion vacio = habitacion("2", "   ", "http://switches-api:8090/switches/2");
    Inventory.Habitacion valido = habitacion("3", "termo-3", "http://switches-api:8090/switches/3");
    when(inventoryService.obtenerActivo())
        .thenReturn(Optional.of(inventario(List.of(nulo, vacio, valido))));

    List<Inventory.Habitacion> resultado = service.validar();

    assertEquals(List.of(nulo, vacio), resultado);
  }

  @Test
  @DisplayName("Detecta habitaciones con urlSwitch nula, vacía o inválida")
  void validarUrlSwitchInvalida() {
    Inventory.Habitacion nula = habitacion("1", "termo-1", null);
    Inventory.Habitacion vacia = habitacion("2", "termo-2", "   ");
    Inventory.Habitacion malformada = habitacion("3", "termo-3", "no-es-una-uri-valida");
    Inventory.Habitacion protocoloInvalido = habitacion("4", "termo-4", "ftp://invalido.com");
    Inventory.Habitacion valida = habitacion("5", "termo-5", "http://switches-api:8090/switches/5");
    when(inventoryService.obtenerActivo())
        .thenReturn(
            Optional.of(inventario(List.of(nula, vacia, malformada, protocoloInvalido, valida))));

    List<Inventory.Habitacion> resultado = service.validar();

    assertEquals(List.of(nula, vacia, malformada, protocoloInvalido), resultado);
  }

  @Test
  @DisplayName("Devuelve todas las habitaciones duplicadas por idTermostato")
  void validarIdTermostatoDuplicado() {
    Inventory.Habitacion living =
        habitacion("1", "termo-dup", "http://switches-api:8090/switches/1");
    Inventory.Habitacion cocina =
        habitacion("2", "termo-dup", "http://switches-api:8090/switches/2");
    Inventory.Habitacion dormitorio =
        habitacion("3", "termo-ok", "http://switches-api:8090/switches/3");
    when(inventoryService.obtenerActivo())
        .thenReturn(Optional.of(inventario(List.of(living, cocina, dormitorio))));

    assertEquals(List.of(living, cocina), service.validar());
  }

  @Test
  @DisplayName("Devuelve todas las habitaciones duplicadas por urlSwitch")
  void validarUrlSwitchDuplicada() {
    String urlDuplicada = "http://switches-api:8090/switches/duplicado";
    Inventory.Habitacion living = habitacion("1", "termo-1", urlDuplicada);
    Inventory.Habitacion cocina = habitacion("2", "termo-2", urlDuplicada);
    Inventory.Habitacion dormitorio =
        habitacion("3", "termo-3", "http://switches-api:8090/switches/ok");
    when(inventoryService.obtenerActivo())
        .thenReturn(Optional.of(inventario(List.of(living, cocina, dormitorio))));

    assertEquals(List.of(living, cocina), service.validar());
  }

  @Test
  @DisplayName("Una habitación con varios incumplimientos no se duplica en el resultado")
  void validarMultiplesIncumplimientosNoDuplica() {
    Inventory.Habitacion living = habitacion("1", "termo-dup", "malformada");
    Inventory.Habitacion cocina =
        habitacion("2", "termo-dup", "http://switches-api:8090/switches/2");
    when(inventoryService.obtenerActivo())
        .thenReturn(Optional.of(inventario(List.of(living, cocina))));

    assertEquals(List.of(living, cocina), service.validar());
  }

  @Test
  @DisplayName("Busca la habitación por termostato en el inventory activo")
  void obtenerPorTermostatoUsaInventarioActivo() {
    Inventory.Habitacion habitacion =
        new Inventory.Habitacion(
            "room1",
            "Living",
            21.0,
            1.2,
            "termo1",
            "termo1/status/temperature:0",
            "switch1",
            "http://switches:8081");
    when(inventoryService.obtenerActivo()).thenReturn(Optional.of(inventario(List.of(habitacion))));

    assertEquals(Optional.of(habitacion), service.obtenerPorTermostato("termo1"));
    verify(inventoryService).obtenerActivo();
  }

  private static Inventory inventario(List<Inventory.Habitacion> habitaciones) {
    return new Inventory(
        "2.0",
        new Inventory.Sitio(
            "casa",
            "Casa",
            3.7,
            new Inventory.Tarifa(new Inventory.Franja("17:00", "23:00", Inventory.Dias.HABILES))),
        habitaciones);
  }

  private static Inventory.Habitacion habitacion(String id, String idTermostato, String urlSwitch) {
    return new Inventory.Habitacion(
        id, "Habitación " + id, 21.0, 1.2, idTermostato, "topic/" + id, "switch-" + id, urlSwitch);
  }
}
