package org.iiss.grupo_2.sub.dto;

public class Room {
    private String id;
    private String nombre;
    private double temperaturaEsperada;
    private String idTermostato;
    private String uriSwitch;

    public Room() {}

    public Room(String id, String nombre, double temperaturaEsperada, String idTermostato, String uriSwitch) {
        this.id = id;
        this.nombre = nombre;
        this.temperaturaEsperada = temperaturaEsperada;
        this.idTermostato = idTermostato;
        this.uriSwitch = uriSwitch;
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

    public String getUriSwitch() {
        return uriSwitch;
    }
}
