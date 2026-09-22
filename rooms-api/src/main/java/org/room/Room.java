package org.room;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("habitaciones")
public class Room {

    @Id
    private String id;
    private String nombre;
    private double temperaturaEsperada;
    private String idTermostato;
    private String uriSwitch;

    public Room() {
    }

    public Room(String nombre, double temperaturaEsperada, String idTermostato, String uriSwitch) {
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

    public void setNombre(String nombre) {
    this.nombre = nombre;
    }

    public void setTemperaturaEsperada(double temperaturaEsperada) {
    this.temperaturaEsperada = temperaturaEsperada;
    }

    public void setIdTermostato(String idTermostato) {
    this.idTermostato = idTermostato;
    }

    public void setUriSwitch(String uriSwitch) {
    this.uriSwitch = uriSwitch;
    }
    
}