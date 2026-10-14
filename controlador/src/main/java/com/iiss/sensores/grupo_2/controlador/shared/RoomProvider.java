package com.iiss.sensores.grupo_2.controlador.shared;

import com.iiss.sensores.grupo_2.controlador.api.model.Inventario;

import java.util.Optional;

public interface RoomProvider {
    Optional<Inventario.Habitacion> obtenerPorTermostato(String idTermostato);
}
