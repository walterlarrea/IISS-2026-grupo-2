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
    private String idSwitch;

    public Room() {
    }

    public Room(String nombre, double temperaturaEsperada, String idTermostato, String idSwitch) {
        this.nombre = nombre;
        this.temperaturaEsperada = temperaturaEsperada;
        this.idTermostato = idTermostato;
        this.idSwitch = idSwitch;
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

    public void setNombre(String nombre) {
    this.nombre = nombre;
    }

    public void setTemperaturaEsperada(double temperaturaEsperada) {
    this.temperaturaEsperada = temperaturaEsperada;
    }

    public void setIdTermostato(String idTermostato) {
    this.idTermostato = idTermostato;
    }

    public void setIdSwitch(String idSwitch) {
    this.idSwitch = idSwitch;
    }
    
}