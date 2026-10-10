package com.iiss.sensores.grupo_2.controlador.shared;

import java.util.Optional;

public interface RoomProvider {
    Optional<RoomInfo> obtenerPorTermostato(String idTermostato);
}
