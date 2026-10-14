package com.iiss.sensores.grupo_2.controlador.api;

import com.iiss.sensores.grupo_2.controlador.api.model.Inventario;
import com.iiss.sensores.grupo_2.controlador.api.service.InventarioService;
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

class HabitacionServiceTest {

    private InventarioService inventarioService;
    private RoomService service;

    @BeforeEach
    void setUp() {
        inventarioService = mock(InventarioService.class);
        service = new RoomService(inventarioService);
    }

    @Test
    void crearHabitacionLaAgregaAlInventario() {
        Inventario inventario = inventario(List.of(habitacion("sala")));
        Inventario.Habitacion nueva = habitacion("dormitorio");
        when(inventarioService.obtenerActivo()).thenReturn(Optional.of(inventario));

        assertEquals(nueva, service.crear(nueva));

        ArgumentCaptor<Inventario> captor = ArgumentCaptor.forClass(Inventario.class);
        verify(inventarioService).reemplazar(captor.capture());
        assertEquals(List.of(habitacion("sala"), nueva), captor.getValue().habitaciones());
    }

    @Test
    void crearHabitacionConIdExistenteNoModificaInventario() {
        Inventario.Habitacion existente = habitacion("sala");
        Inventario inventario = inventario(List.of(existente));
        when(inventarioService.obtenerActivo()).thenReturn(Optional.of(inventario));

        assertEquals(existente, service.crear(habitacion("sala")));

        verify(inventarioService, never()).reemplazar(any());
    }

    @Test
    void listarHabitacionesDelInventarioActivo() {
        List<Inventario.Habitacion> habitaciones = List.of(habitacion("sala"));
        when(inventarioService.obtenerActivo()).thenReturn(Optional.of(inventario(habitaciones)));

        assertEquals(habitaciones, service.listar());
    }

    @Test
    void buscarPorIdDevuelveHabitacionDelInventario() {
        Inventario.Habitacion habitacion = habitacion("sala");
        when(inventarioService.obtenerActivo()).thenReturn(Optional.of(inventario(List.of(habitacion))));

        assertEquals(Optional.of(habitacion), service.buscarPorId("sala"));
        assertTrue(service.buscarPorId("inexistente").isEmpty());
    }

    @Test
    void eliminarHabitacionLaQuitaDelInventario() {
        Inventario inventario = inventario(List.of(habitacion("sala"), habitacion("dormitorio")));
        when(inventarioService.obtenerActivo()).thenReturn(Optional.of(inventario));

        assertTrue(service.eliminar("sala"));

        ArgumentCaptor<Inventario> captor = ArgumentCaptor.forClass(Inventario.class);
        verify(inventarioService).reemplazar(captor.capture());
        assertEquals(List.of(habitacion("dormitorio")), captor.getValue().habitaciones());
    }

    @Test
    void eliminarHabitacionInexistenteNoModificaInventario() {
        when(inventarioService.obtenerActivo()).thenReturn(Optional.of(inventario(List.of(habitacion("sala")))));

        assertFalse(service.eliminar("inexistente"));

        verify(inventarioService, never()).reemplazar(any());
    }

    @Test
    void obtenerPorTermostatoBuscaEnInventario() {
        Inventario.Habitacion habitacion = habitacion("sala");
        when(inventarioService.obtenerActivo()).thenReturn(Optional.of(inventario(List.of(habitacion))));

        assertEquals(Optional.of(habitacion), service.obtenerPorTermostato("termostato-sala"));
        assertTrue(service.obtenerPorTermostato("inexistente").isEmpty());
    }

    @Test
    void modificarHabitacionReemplazaDatosYConservaElIdDeLaRuta() {
        Inventario inventario = inventario(List.of(habitacion("sala")));
        Inventario.Habitacion modificada = habitacion("otro-id");
        when(inventarioService.obtenerActivo()).thenReturn(Optional.of(inventario));

        Optional<Inventario.Habitacion> resultado = service.modificar("sala", modificada);

        assertTrue(resultado.isPresent());
        assertEquals(habitacion("sala").id(), resultado.get().id());
        assertEquals(modificada.nombre(), resultado.get().nombre());
        assertEquals(modificada.temperaturaEsperada(), resultado.get().temperaturaEsperada());
        assertEquals(modificada.idTermostato(), resultado.get().idTermostato());
        ArgumentCaptor<Inventario> captor = ArgumentCaptor.forClass(Inventario.class);
        verify(inventarioService).reemplazar(captor.capture());
        assertEquals(List.of(resultado.get()), captor.getValue().habitaciones());
    }

    @Test
    void modificarHabitacionInexistenteDevuelveVacio() {
        when(inventarioService.obtenerActivo()).thenReturn(Optional.of(inventario(List.of(habitacion("sala")))));

        assertTrue(service.modificar("inexistente", habitacion("inexistente")).isEmpty());

        verify(inventarioService, never()).reemplazar(any());
    }

    @Test
    void modificarParcialmenteConservaCamposNoModificados() {
        Inventario.Habitacion original = habitacion("sala");
        when(inventarioService.obtenerActivo()).thenReturn(Optional.of(inventario(List.of(original))));
        Map<String, Object> cambios = Map.of("nombre", "Estar", "temperaturaEsperada", 23.0);

        Inventario.Habitacion resultado = service.modificarParcialmente("sala", cambios).orElseThrow();

        assertEquals("Estar", resultado.nombre());
        assertEquals(23.0, resultado.temperaturaEsperada());
        assertEquals(original.idTermostato(), resultado.idTermostato());
        assertEquals(original.urlSwitch(), resultado.urlSwitch());
        verify(inventarioService).reemplazar(any());
    }

    @Test
    void modificarParcialmenteHabitacionInexistenteDevuelveVacio() {
        when(inventarioService.obtenerActivo()).thenReturn(Optional.of(inventario(List.of(habitacion("sala")))));

        assertTrue(service.modificarParcialmente("inexistente", Map.of("nombre", "Estar")).isEmpty());

        verify(inventarioService, never()).reemplazar(any());
    }

    @Test
    void validarDetectaHabitacionesConTermostatoDuplicado() {
        Inventario.Habitacion sala = habitacion("sala");
        Inventario.Habitacion dormitorio = new Inventario.Habitacion(
                "dormitorio",
                "Dormitorio",
                21.0,
                1.5,
                sala.idTermostato(),
                "termostato/dormitorio",
                "switch-dormitorio",
                "http://switches-api:8090/switches/dormitorio"
        );
        when(inventarioService.obtenerActivo())
                .thenReturn(Optional.of(inventario(List.of(sala, dormitorio))));

        assertEquals(List.of(sala, dormitorio), service.validar());
    }

    private static Inventario inventario(List<Inventario.Habitacion> habitaciones) {
        return new Inventario(
                "2.0",
                new Inventario.Sitio(
                        "casa",
                        "Casa",
                        5.0,
                        new Inventario.Tarifa(
                                new Inventario.Franja("17:00", "23:00", Inventario.Dias.HABILES)
                        )
                ),
                habitaciones
        );
    }

    private static Inventario.Habitacion habitacion(String id) {
        return new Inventario.Habitacion(
                id,
                "Habitación " + id,
                21.0,
                1.5,
                "termostato-" + id,
                "termostato/" + id,
                "switch-" + id,
                "http://switches-api:8090/switches/" + id
        );
    }
}
