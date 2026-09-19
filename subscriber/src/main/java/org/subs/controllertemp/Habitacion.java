package org.subs.controllertemp;

public class Habitacion {
    private String id;
    private String nombre;
    private double temperaturaEsperada;
    private String idTermostato;
    private String idSwitch;

    public Habitacion() {
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public double getTemperaturaEsperada() {
        return temperaturaEsperada;
    }

    public String getIdTermostato() {
        return idTermostato;
    }

    public String getIdSwitch() {
        return idSwitch;
    }
}
