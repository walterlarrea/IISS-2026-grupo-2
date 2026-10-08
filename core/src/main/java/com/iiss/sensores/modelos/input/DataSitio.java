package com.iiss.sensores.modelos.input;

import java.util.List;

public record DataSitio(
	Sitio sitio,
	List<Habitacion> habitaciones
) {}