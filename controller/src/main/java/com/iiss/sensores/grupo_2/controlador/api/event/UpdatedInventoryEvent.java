package com.iiss.sensores.grupo_2.controlador.api.event;

import com.iiss.sensores.grupo_2.controlador.api.model.Inventory;

/** Evento publicado cuando se reemplaza el inventario activo. */
public record UpdatedInventoryEvent(Inventory inventory) {}
