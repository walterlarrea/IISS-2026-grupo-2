package org.subs.classes;

public class Room {
    private String id;
    private String nombre;
    private double temperaturaEsperada;
    private String idTermostato;
    private String uriSwitch;

    public Room() {}

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

    public String getUriSwitch() {
        return uriSwitch;
    }
}
