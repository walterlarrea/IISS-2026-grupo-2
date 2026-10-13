package com.iiss.sensores.grupo_2.controlador.api.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("inventario_controlador")
public record InventarioDocumento(@Id String id, Inventario inventario) {
}
