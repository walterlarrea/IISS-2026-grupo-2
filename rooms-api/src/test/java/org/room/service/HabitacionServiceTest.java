package org.room.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.room.Room;
import org.room.repository.RoomRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HabitacionServiceTest {

    @Mock
    private RoomRepository repository;

    private HabitacionService service;

    @BeforeEach
    void setUp() {
        service = new HabitacionService(repository);
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
        // r1 tiene uriSwitch inválido Y idTermostato duplicado con r2
        Room r1 = new Room("1", "Living", 21.0, "termo-dup", "malformada");
        Room r2 = new Room("2", "Cocina", 20.0, "termo-dup", "http://switches-api:8090/switches/2");

        when(repository.findAll()).thenReturn(List.of(r1, r2));

        List<Room> resultado = service.validar();

        assertEquals(2, resultado.size());
        assertTrue(resultado.contains(r1));
        assertTrue(resultado.contains(r2));
    }
}
