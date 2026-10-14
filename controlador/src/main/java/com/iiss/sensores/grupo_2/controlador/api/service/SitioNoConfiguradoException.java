package com.iiss.sensores.grupo_2.controlador.api.service;

public class SitioNoConfiguradoException extends RuntimeException {
    public SitioNoConfiguradoException() {
        super("Todavía no se cargó ningún sitio");
    }
}
