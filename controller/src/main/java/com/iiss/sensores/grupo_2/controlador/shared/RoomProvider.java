package com.iiss.sensores.grupo_2.controlador.shared;

import com.iiss.sensores.grupo_2.controlador.api.model.Inventory;

import java.util.Optional;

public interface RoomProvider {
    Optional<Inventory.Habitacion> obtenerPorTermostato(String idTermostato);
}
