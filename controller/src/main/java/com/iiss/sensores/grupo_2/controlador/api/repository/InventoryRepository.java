package com.iiss.sensores.grupo_2.controlador.api.repository;

import com.iiss.sensores.grupo_2.controlador.api.model.InventoryDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface InventoryRepository extends MongoRepository<InventoryDocument, String> {}
