package com.iiss.sensores.grupo_2.controlador.api.service;

import java.util.List;

public class InvalidRequestException extends RuntimeException {
    private final List<String> errores;

    public InvalidRequestException(String mensaje, List<String> errores) {
        super(mensaje);
        this.errores = List.copyOf(errores);
    }

    public List<String> getErrores() {
        return errores;
    }
}
