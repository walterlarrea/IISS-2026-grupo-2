package com.iiss.sensores.grupo_2.controlador.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.iiss.sensores.grupo_2.controlador.api.model.Room;
import com.iiss.sensores.grupo_2.controlador.api.repository.RoomRepository;
import com.iiss.sensores.grupo_2.controlador.api.service.RoomService;
import com.iiss.sensores.grupo_2.controlador.api.service.InventarioService;
import com.iiss.sensores.grupo_2.controlador.api.model.Inventario;
import com.iiss.sensores.grupo_2.controlador.shared.RoomInfo;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RoomRepository repository;

    private RoomService service;

    @BeforeEach
    void setUp() {
        service = new RoomService(repository);
    }

    @Test
    @DisplayName("Devuelve lista vacía si la colección no contiene habitaciones")
    void validarColeccionVacia() {
        when(repository.findAll()).thenReturn(List.of());

        List<Room> resultado = service.validar();

        assertTrue(resultado.isEmpty());
    }

    @Test
    @DisplayName("Devuelve lista vacía cuando todas las habitaciones son válidas y no hay duplicados")
    void validarTodasValidas() {
        Room r1 = new Room("1", "Living", 21.0, "termo-1", "http://switches-api:8090/switches/1");
        Room r2 = new Room("2", "Cocina", 20.0, "termo-2", "http://switches-api:8090/switches/2");

        when(repository.findAll()).thenReturn(List.of(r1, r2));

        List<Room> resultado = service.validar();

        assertTrue(resultado.isEmpty());
    }

    @Test
    @DisplayName("Detecta habitaciones con idTermostato nulo o vacío")
    void validarIdTermostatoInvalido() {
        Room r1 = new Room("1", "Living", 21.0, null, "http://switches-api:8090/switches/1");
        Room r2 = new Room("2", "Cocina", 20.0, "   ", "http://switches-api:8090/switches/2");
        Room r3 = new Room("3", "Dormitorio", 22.0, "termo-3", "http://switches-api:8090/switches/3");

        when(repository.findAll()).thenReturn(List.of(r1, r2, r3));

        List<Room> resultado = service.validar();

        assertEquals(2, resultado.size());
        assertTrue(resultado.contains(r1));
        assertTrue(resultado.contains(r2));
    }

    @Test
    @DisplayName("Detecta habitaciones con uriSwitch nulo, vacío o malformado")
    void validarUriSwitchInvalido() {
        Room r1 = new Room("1", "Living", 21.0, "termo-1", null);
        Room r2 = new Room("2", "Cocina", 20.0, "termo-2", "   ");
        Room r3 = new Room("3", "Dormitorio", 22.0, "termo-3", "no-es-una-uri-valida");
        Room r4 = new Room("4", "Baño", 23.0, "termo-4", "ftp://invalido.com");
        Room r5 = new Room("5", "Garage", 18.0, "termo-5", "http://switches-api:8090/switches/5");

        when(repository.findAll()).thenReturn(List.of(r1, r2, r3, r4, r5));

        List<Room> resultado = service.validar();

        assertEquals(4, resultado.size());
        assertTrue(resultado.contains(r1));
        assertTrue(resultado.contains(r2));
        assertTrue(resultado.contains(r3));
        assertTrue(resultado.contains(r4));
    }

    @Test
    @DisplayName("Devuelve todas las habitaciones duplicadas por idTermostato")
    void validarIdTermostatoDuplicado() {
        Room r1 = new Room("1", "Living", 21.0, "termo-dup", "http://switches-api:8090/switches/1");
        Room r2 = new Room("2", "Cocina", 20.0, "termo-dup", "http://switches-api:8090/switches/2");
        Room r3 = new Room("3", "Dormitorio", 22.0, "termo-ok", "http://switches-api:8090/switches/3");

        when(repository.findAll()).thenReturn(List.of(r1, r2, r3));

        List<Room> resultado = service.validar();

        assertEquals(2, resultado.size());
        assertTrue(resultado.contains(r1));
        assertTrue(resultado.contains(r2));
    }

    @Test
    @DisplayName("Devuelve todas las habitaciones duplicadas por uriSwitch")
    void validarUriSwitchDuplicado() {
        Room r1 = new Room("1", "Living", 21.0, "termo-1", "http://switches-api:8090/switches/dup");
        Room r2 = new Room("2", "Cocina", 20.0, "termo-2", "http://switches-api:8090/switches/dup");
        Room r3 = new Room("3", "Dormitorio", 22.0, "termo-3", "http://switches-api:8090/switches/ok");

        when(repository.findAll()).thenReturn(List.of(r1, r2, r3));

        List<Room> resultado = service.validar();

        assertEquals(2, resultado.size());
        assertTrue(resultado.contains(r1));
        assertTrue(resultado.contains(r2));
    }

    @Test
    @DisplayName("Una habitación con múltiples incumplimientos no se duplica en el resultado")
    void validarMultiplesIncumplimientosNoDuplica() {
        Room r1 = new Room("1", "Living", 21.0, "termo-dup", "malformada");
        Room r2 = new Room("2", "Cocina", 20.0, "termo-dup", "http://switches-api:8090/switches/2");

        when(repository.findAll()).thenReturn(List.of(r1, r2));

        List<Room> resultado = service.validar();

        assertEquals(2, resultado.size());
        assertTrue(resultado.contains(r1));
        assertTrue(resultado.contains(r2));
    }

    @Test
    @DisplayName("Usa la habitación del inventario activo para el control y compone la URL del switch")
    void obtenerPorTermostatoUsaInventarioActivo() {
        InventarioService inventarioService = mock(InventarioService.class);
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
        Inventario inventario = new Inventario(
                "2.0",
                new Inventario.Sitio(
                        "casa",
                        "Casa",
                        3.7,
                        new Inventario.Tarifa(new Inventario.Franja("17:00", "23:00", Inventario.Dias.HABILES))
                ),
                List.of(habitacion)
        );
        when(inventarioService.obtenerActivo()).thenReturn(Optional.of(inventario));

        RoomService conInventario = new RoomService(repository, inventarioService);

        assertEquals(
                Optional.of(new RoomInfo(
                        "room1",
                        "Living",
                        21.0,
                        "termo1",
                        "http://switches:8081/switches/switch1"
                )),
                conInventario.obtenerPorTermostato("termo1")
        );
        verify(repository, never()).findByIdTermostato("termo1");
    }
}
