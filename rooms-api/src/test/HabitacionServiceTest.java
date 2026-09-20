package org.example;

import org.example.controller.HabitacionController;
import org.example.service.HabitacionService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class HabitacionServiceTest {

    @Test
    void crearHabitacionTest() {

        HabitacionRepository repositoryMock = mock(HabitacionRepository.class);

        HabitacionService service = new HabitacionService(repositoryMock);

        Habitacion habitacion = new Habitacion(
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

        HabitacionRepository repositoryMock = mock(HabitacionRepository.class);

        HabitacionService service = new HabitacionService(repositoryMock);

        List<Habitacion> habitaciones = List.of(
            new Habitacion("Dormitorio", 21.0, "termostato-01", "switch-01"),
            new Habitacion("Sala", 22.0, "termostato-02", "switch-02")
        );

        when(repositoryMock.findAll()).thenReturn(habitaciones);

        List<Habitacion> resultado = service.listar();

        assertEquals(habitaciones, resultado);

        verify(repositoryMock, times(1)).findAll();
    }

    @Test
    void buscarPorIdTest() {

        HabitacionRepository repositoryMock = mock(HabitacionRepository.class);

        HabitacionService service = new HabitacionService(repositoryMock);

        String id = "habitacion-01";

        Habitacion habitacion = new Habitacion(
                "Dormitorio",
                21.0,
                "termostato-01",
                "switch-01"
        );

        when(repositoryMock.findById(id)).thenReturn(java.util.Optional.of(habitacion));

        java.util.Optional<Habitacion> resultado = service.buscarPorId(id);

        assertEquals(java.util.Optional.of(habitacion), resultado);

        verify(repositoryMock, times(1)).findById(id);
    }

    @Test
    void eliminarHabitacionTest() {

        HabitacionRepository repositoryMock = mock(HabitacionRepository.class);

        HabitacionService service = new HabitacionService(repositoryMock);

        String id = "habitacion-01";

        when(repositoryMock.existsById(id)).thenReturn(true);

        boolean resultado = service.eliminar(id);

        assertEquals(true, resultado);

        verify(repositoryMock, times(1)).existsById(id);
        verify(repositoryMock, times(1)).deleteById(id);
    }

    @Test
    void eliminarHabitacionNoExistenteTest() {
        
        HabitacionRepository repositoryMock = mock(HabitacionRepository.class);

        HabitacionService service = new HabitacionService(repositoryMock);

        String id = "habitacion-01";

        when(repositoryMock.existsById(id)).thenReturn(false);

        boolean resultado = service.eliminar(id);

        assertEquals(false, resultado);

        verify(repositoryMock, times(1)).existsById(id);
        verify(repositoryMock, times(0)).deleteById(id);
    }

    @Test
    void buscarPorIdTermostatoTest() {
        
        HabitacionRepository repositoryMock = mock(HabitacionRepository.class);

        HabitacionService service = new HabitacionService(repositoryMock);

        String idTermostato = "termostato-01";

        Habitacion habitacion = new Habitacion(
                "Dormitorio",
                21.0,
                idTermostato,
                "switch-01"
        );

        when(repositoryMock.findByIdTermostato(idTermostato)).thenReturn(java.util.Optional.of(habitacion));

        java.util.Optional<Habitacion> resultado = service.buscarPorIdTermostato(idTermostato);

        assertEquals(java.util.Optional.of(habitacion), resultado);

        verify(repositoryMock, times(1)).findByIdTermostato(idTermostato);
    }

    @Test
    void modificarParcialmenteTest() {

    HabitacionRepository repositoryMock = mock(HabitacionRepository.class);

    HabitacionService service = new HabitacionService(repositoryMock);

    String id = "habitacion-01";

    Habitacion habitacionExistente = new Habitacion(
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

    java.util.Optional<Habitacion> resultado =
            service.modificarParcialmente(id, cambios);

    assertEquals("Sala", resultado.get().getNombre());
    assertEquals(22.0, resultado.get().getTemperaturaEsperada());

    verify(repositoryMock, times(1)).findById(id);
    verify(repositoryMock, times(1)).save(habitacionExistente);
}

    @Test
    void modificarParcialmenteNoExistenteTest() {
        
        HabitacionRepository repositoryMock = mock(HabitacionRepository.class);

        HabitacionService service = new HabitacionService(repositoryMock);

        String id = "habitacion-01";

        when(repositoryMock.findById(id)).thenReturn(java.util.Optional.empty());

        java.util.Map<String, Object> cambios = java.util.Map.of(
            "nombre", "Sala",
            "temperaturaEsperada", 22.0
        );

        java.util.Optional<Habitacion> resultado = service.modificarParcialmente(id, cambios);

        assertEquals(java.util.Optional.empty(), resultado);

        verify(repositoryMock, times(1)).findById(id);
        verify(repositoryMock, times(0)).save(any());
    }

    @Test
    void modificarTest(){

        HabitacionRepository repositoryMock = mock(HabitacionRepository.class);

        HabitacionService service = new HabitacionService(repositoryMock);

        String id = "habitacion-01";

            Habitacion habitacionExistente = new Habitacion(
                "Dormitorio",
                21.0,
                "termostato-01",
                "switch-01"
            );

            when(repositoryMock.findById(id)).thenReturn(java.util.Optional.of(habitacionExistente));

            Habitacion habitacionModificada = new Habitacion(
                "Sala",
                22.0,
                "termostato-02",
                "switch-02"
            );

            when(repositoryMock.save(habitacionExistente)).thenReturn(habitacionExistente);

            java.util.Optional<Habitacion> resultado = service.modificar(id, habitacionModificada);

            assertEquals("Sala", resultado.get().getNombre());
            assertEquals(22.0, resultado.get().getTemperaturaEsperada());
            assertEquals("termostato-02", resultado.get().getIdTermostato());
            assertEquals("switch-02", resultado.get().getIdSwitch());

            verify(repositoryMock, times(1)).findById(id);
            verify(repositoryMock, times(1)).save(habitacionExistente);
        }
}
