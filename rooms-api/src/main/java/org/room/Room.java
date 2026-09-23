package org.room;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Objects;

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

    public void setId(String id) {
        this.id = id;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Room room = (Room) o;
        if (id != null && room.id != null) {
            return Objects.equals(id, room.id);
        }
        return Objects.equals(id, room.id) &&
               Objects.equals(nombre, room.nombre) &&
               Double.compare(room.temperaturaEsperada, temperaturaEsperada) == 0 &&
               Objects.equals(idTermostato, room.idTermostato) &&
               Objects.equals(uriSwitch, room.uriSwitch);
    }

    @Override
    public int hashCode() {
        return id != null ? Objects.hashCode(id) : Objects.hash(nombre, temperaturaEsperada, idTermostato, uriSwitch);
    }

    @Override
    public String toString() {
        return "Room{" +
                "id='" + id + '\'' +
                ", nombre='" + nombre + '\'' +
                ", temperaturaEsperada=" + temperaturaEsperada +
                ", idTermostato='" + idTermostato + '\'' +
                ", uriSwitch='" + uriSwitch + '\'' +
                '}';
    }
}