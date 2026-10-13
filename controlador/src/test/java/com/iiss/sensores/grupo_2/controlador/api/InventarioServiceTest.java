package com.iiss.sensores.grupo_2.controlador.api;

import com.iiss.sensores.grupo_2.controlador.api.model.Inventario;
import com.iiss.sensores.grupo_2.controlador.api.model.InventarioDocumento;
import com.iiss.sensores.grupo_2.controlador.api.repository.InventarioRepository;
import com.iiss.sensores.grupo_2.controlador.api.service.InventarioInvalidoException;
import com.iiss.sensores.grupo_2.controlador.api.service.InventarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InventarioServiceTest {
    private InventarioRepository repository;
    private InventarioService service;

    @BeforeEach
    void setUp() {
        repository = mock(InventarioRepository.class);
        service = new InventarioService(repository);
    }

    @Test
    void guardaInventarioCompletoComoUnSoloDocumento() {
        Inventario inventario = inventario(List.of(habitacion("room1", "termo1", "switch1")));

        service.reemplazar(inventario);

        verify(repository).save(new InventarioDocumento("activo", inventario));
    }

    @Test
    void rechazaIdsDuplicadosSinPersistirElInventario() {
        Inventario inventario = inventario(List.of(
                habitacion("room1", "termo1", "switch1"),
                habitacion("room2", "termo2", "switch1")
        ));

        InventarioInvalidoException exception = assertThrows(
                InventarioInvalidoException.class,
                () -> service.reemplazar(inventario)
        );

        assertEquals(List.of("idSwitch duplicado: 'switch1'"), exception.getErrores());
        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void devuelveElInventarioPersistido() {
        Inventario inventario = inventario(List.of(habitacion("room1", "termo1", "switch1")));
        when(repository.findById("activo"))
                .thenReturn(Optional.of(new InventarioDocumento("activo", inventario)));

        assertEquals(Optional.of(inventario), service.obtenerActivo());
    }

    private Inventario inventario(List<Inventario.Habitacion> habitaciones) {
        return new Inventario(
                "2.0",
                new Inventario.Sitio(
                        "casa",
                        "Casa",
                        3.7,
                        new Inventario.Tarifa(new Inventario.Franja("17:00", "23:00", Inventario.Dias.HABILES))
                ),
                habitaciones
        );
    }

    private Inventario.Habitacion habitacion(String id, String termostato, String switchId) {
        return new Inventario.Habitacion(
                id, "Living", 21.5, 1.2, termostato,
                termostato + "/status/temperature:0", switchId, "http://switches:8081"
        );
    }
}
