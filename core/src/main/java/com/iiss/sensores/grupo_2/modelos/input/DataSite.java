package com.iiss.sensores.grupo_2.modelos.input;

import java.util.List;

/** Agrupa la configuración del sitio y el estado de sus habitaciones. */
public record DataSite(Site site, List<Room> habitaciones) {}
