package com.iiss.sensores.grupo_2.controlador.api.service;

import java.util.List;

public class InventarioInvalidoException extends RuntimeException {
    private final List<String> errores;

    public InventarioInvalidoException(List<String> errores) {
        super("inventario inválido");
        this.errores = List.copyOf(errores);
    }

    public List<String> getErrores() {
        return errores;
    }
}
