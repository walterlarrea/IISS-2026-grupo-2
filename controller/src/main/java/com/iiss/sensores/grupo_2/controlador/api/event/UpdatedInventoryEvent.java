package com.iiss.sensores.grupo_2.controlador.api.event;

import com.iiss.sensores.grupo_2.controlador.api.model.Inventory;

public record UpdatedInventoryEvent(Inventory inventory) {}
