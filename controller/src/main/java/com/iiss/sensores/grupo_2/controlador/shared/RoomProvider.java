package com.iiss.sensores.grupo_2.controlador.shared;

import com.iiss.sensores.grupo_2.controlador.api.model.Inventory;

import java.util.Optional;

/** Contrato para buscar una habitación asociada a un termostato. */
public interface RoomProvider {
  /** Devuelve la habitación asociada al identificador de termostato, si existe. */
  Optional<Inventory.Habitacion> obtenerPorTermostato(String idTermostato);
}
