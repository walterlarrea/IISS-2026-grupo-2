package com.iiss.sensores.grupo_2.controlador.api.repository;

import com.iiss.sensores.grupo_2.controlador.api.model.InventarioDocumento;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface InventarioRepository extends MongoRepository<InventarioDocumento, String> {
}
