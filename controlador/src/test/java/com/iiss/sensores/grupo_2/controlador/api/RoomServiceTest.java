package com.iiss.sensores.grupo_2.controlador.api;

import com.iiss.sensores.grupo_2.controlador.api.model.Inventario;
import com.iiss.sensores.grupo_2.controlador.api.service.InventarioService;
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

    private InventarioService inventarioService;
    private RoomService service;

    @BeforeEach
    void setUp() {
        inventarioService = mock(InventarioService.class);
        service = new RoomService(inventarioService);
    }

    @Test
    @DisplayName("Devuelve lista vacía si el inventario no contiene habitaciones")
    void validarColeccionVacia() {
        when(inventarioService.obtenerActivo()).thenReturn(Optional.of(inventario(List.of())));

        assertTrue(service.validar().isEmpty());
    }

    @Test
    @DisplayName("Devuelve lista vacía cuando las habitaciones son válidas y no están duplicadas")
    void validarTodasValidas() {
        Inventario.Habitacion living = habitacion(
                "1", "termo-1", "http://switches-api:8090/switches/1");
        Inventario.Habitacion cocina = habitacion(
                "2", "termo-2", "http://switches-api:8090/switches/2");
        when(inventarioService.obtenerActivo())
                .thenReturn(Optional.of(inventario(List.of(living, cocina))));

        assertTrue(service.validar().isEmpty());
    }

    @Test
    @DisplayName("Detecta habitaciones con idTermostato nulo o vacío")
    void validarIdTermostatoInvalido() {
        Inventario.Habitacion nulo = habitacion(
                "1", null, "http://switches-api:8090/switches/1");
        Inventario.Habitacion vacio = habitacion(
                "2", "   ", "http://switches-api:8090/switches/2");
        Inventario.Habitacion valido = habitacion(
                "3", "termo-3", "http://switches-api:8090/switches/3");
        when(inventarioService.obtenerActivo())
                .thenReturn(Optional.of(inventario(List.of(nulo, vacio, valido))));

        List<Inventario.Habitacion> resultado = service.validar();

        assertEquals(List.of(nulo, vacio), resultado);
    }

    @Test
    @DisplayName("Detecta habitaciones con urlSwitch nula, vacía o inválida")
    void validarUrlSwitchInvalida() {
        Inventario.Habitacion nula = habitacion("1", "termo-1", null);
        Inventario.Habitacion vacia = habitacion("2", "termo-2", "   ");
        Inventario.Habitacion malformada = habitacion("3", "termo-3", "no-es-una-uri-valida");
        Inventario.Habitacion protocoloInvalido = habitacion("4", "termo-4", "ftp://invalido.com");
        Inventario.Habitacion valida = habitacion(
                "5", "termo-5", "http://switches-api:8090/switches/5");
        when(inventarioService.obtenerActivo()).thenReturn(Optional.of(
                inventario(List.of(nula, vacia, malformada, protocoloInvalido, valida))));

        List<Inventario.Habitacion> resultado = service.validar();

        assertEquals(List.of(nula, vacia, malformada, protocoloInvalido), resultado);
    }

    @Test
    @DisplayName("Devuelve todas las habitaciones duplicadas por idTermostato")
    void validarIdTermostatoDuplicado() {
        Inventario.Habitacion living = habitacion(
                "1", "termo-dup", "http://switches-api:8090/switches/1");
        Inventario.Habitacion cocina = habitacion(
                "2", "termo-dup", "http://switches-api:8090/switches/2");
        Inventario.Habitacion dormitorio = habitacion(
                "3", "termo-ok", "http://switches-api:8090/switches/3");
        when(inventarioService.obtenerActivo())
                .thenReturn(Optional.of(inventario(List.of(living, cocina, dormitorio))));

        assertEquals(List.of(living, cocina), service.validar());
    }

    @Test
    @DisplayName("Devuelve todas las habitaciones duplicadas por urlSwitch")
    void validarUrlSwitchDuplicada() {
        String urlDuplicada = "http://switches-api:8090/switches/duplicado";
        Inventario.Habitacion living = habitacion("1", "termo-1", urlDuplicada);
        Inventario.Habitacion cocina = habitacion("2", "termo-2", urlDuplicada);
        Inventario.Habitacion dormitorio = habitacion(
                "3", "termo-3", "http://switches-api:8090/switches/ok");
        when(inventarioService.obtenerActivo())
                .thenReturn(Optional.of(inventario(List.of(living, cocina, dormitorio))));

        assertEquals(List.of(living, cocina), service.validar());
    }

    @Test
    @DisplayName("Una habitación con varios incumplimientos no se duplica en el resultado")
    void validarMultiplesIncumplimientosNoDuplica() {
        Inventario.Habitacion living = habitacion("1", "termo-dup", "malformada");
        Inventario.Habitacion cocina = habitacion(
                "2", "termo-dup", "http://switches-api:8090/switches/2");
        when(inventarioService.obtenerActivo())
                .thenReturn(Optional.of(inventario(List.of(living, cocina))));

        assertEquals(List.of(living, cocina), service.validar());
    }

    @Test
    @DisplayName("Busca la habitación por termostato en el inventario activo")
    void obtenerPorTermostatoUsaInventarioActivo() {
        Inventario.Habitacion habitacion = new Inventario.Habitacion(
                "room1",
                "Living",
                21.0,
                1.2,
                "termo1",
                "termo1/status/temperature:0",
                "switch1",
                "http://switches:8081"
        );
        when(inventarioService.obtenerActivo())
                .thenReturn(Optional.of(inventario(List.of(habitacion))));

        assertEquals(Optional.of(habitacion), service.obtenerPorTermostato("termo1"));
        verify(inventarioService).obtenerActivo();
    }

    private static Inventario inventario(List<Inventario.Habitacion> habitaciones) {
        return new Inventario(
                "2.0",
                new Inventario.Sitio(
                        "casa",
                        "Casa",
                        3.7,
                        new Inventario.Tarifa(
                                new Inventario.Franja("17:00", "23:00", Inventario.Dias.HABILES)
                        )
                ),
                habitaciones
        );
    }

    private static Inventario.Habitacion habitacion(String id, String idTermostato, String urlSwitch) {
        return new Inventario.Habitacion(
                id,
                "Habitación " + id,
                21.0,
                1.2,
                idTermostato,
                "topic/" + id,
                "switch-" + id,
                urlSwitch
        );
    }
}
