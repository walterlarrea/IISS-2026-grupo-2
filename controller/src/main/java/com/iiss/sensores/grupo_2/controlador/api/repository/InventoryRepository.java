package com.iiss.sensores.grupo_2.controlador.api.repository;

import com.iiss.sensores.grupo_2.controlador.api.model.InventoryDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

/** Acceso a los documentos de inventario almacenados en MongoDB. */
public interface InventoryRepository extends MongoRepository<InventoryDocument, String> {}
