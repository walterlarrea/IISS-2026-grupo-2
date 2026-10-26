package com.iiss.sensores.grupo_2.controlador.api;

import com.iiss.sensores.grupo_2.controlador.api.model.Inventory;
import com.iiss.sensores.grupo_2.controlador.api.model.InventoryDocument;
import com.iiss.sensores.grupo_2.controlador.api.repository.InventoryRepository;
import com.iiss.sensores.grupo_2.controlador.api.event.UpdatedInventoryEvent;
import com.iiss.sensores.grupo_2.controlador.api.service.InvalidInventoryException;
import com.iiss.sensores.grupo_2.controlador.api.service.InventoryService;
import org.springframework.context.ApplicationEventPublisher;
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

class InventoryServiceTest {
    private InventoryRepository repository;
    private ApplicationEventPublisher eventPublisher;
    private InventoryService service;

    @BeforeEach
    void setUp() {
        repository = mock(InventoryRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        service = new InventoryService(repository, eventPublisher);
    }

    @Test
    void guardaInventarioCompletoComoUnSoloDocumento() {
        Inventory inventory = inventario(List.of(habitacion("room1", "termo1", "switch1")));

        service.reemplazar(inventory);

        verify(repository).save(new InventoryDocument("activo", inventory));
        verify(eventPublisher).publishEvent(new UpdatedInventoryEvent(inventory));
    }

    @Test
    void rechazaIdsDuplicadosSinPersistirElInventario() {
        Inventory inventory = inventario(List.of(
                habitacion("room1", "termo1", "switch1"),
                habitacion("room2", "termo2", "switch1")
        ));

        InvalidInventoryException exception = assertThrows(
                InvalidInventoryException.class,
                () -> service.reemplazar(inventory)
        );

        assertEquals(List.of("idSwitch duplicado: 'switch1'"), exception.getErrores());
        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(eventPublisher, never()).publishEvent(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void devuelveElInventarioPersistido() {
        Inventory inventory = inventario(List.of(habitacion("room1", "termo1", "switch1")));
        when(repository.findById("activo"))
                .thenReturn(Optional.of(new InventoryDocument("activo", inventory)));

        assertEquals(Optional.of(inventory), service.obtenerActivo());
    }

    private Inventory inventario(List<Inventory.Habitacion> habitaciones) {
        return new Inventory(
                "2.0",
                new Inventory.Sitio(
                        "casa",
                        "Casa",
                        3.7,
                        new Inventory.Tarifa(new Inventory.Franja("17:00", "23:00", Inventory.Dias.HABILES))
                ),
                habitaciones
        );
    }

    private Inventory.Habitacion habitacion(String id, String termostato, String switchId) {
        return new Inventory.Habitacion(
                id, "Living", 21.5, 1.2, termostato,
                termostato + "/status/temperature:0", switchId, "http://switches:8081"
        );
    }
}
