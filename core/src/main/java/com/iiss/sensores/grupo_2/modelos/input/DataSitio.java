package com.iiss.sensores.grupo_2.modelos.input;

import java.util.List;

public record DataSitio(
	Sitio sitio,
	List<Habitacion> habitaciones
) {}