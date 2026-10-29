package com.iiss.sensores.grupo_2.controlador.api.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Objects;

@Document("habitaciones")
public class Room {
  @Id private String id;
  private String nombre;
  private double temperaturaEsperada;
  private double potenciaKW;
  private String idTermostato;
  private String topicTermostato;
  private String idSwitch;
  private String urlSwitch;

  public Room() {}

  public Room(
      String nombre,
      double temperaturaEsperada,
      double potenciaKW,
      String idTermostato,
      String topicTermostato,
      String idSwitch,
      String urlSwitch) {
    this.nombre = nombre;
    this.temperaturaEsperada = temperaturaEsperada;
    this.potenciaKW = potenciaKW;
    this.idTermostato = idTermostato;
    this.topicTermostato = topicTermostato;
    this.idSwitch = idSwitch;
    this.urlSwitch = urlSwitch;
  }

  public Room(
      String id,
      String nombre,
      double temperaturaEsperada,
      double potenciaKW,
      String idTermostato,
      String topicTermostato,
      String idSwitch,
      String urlSwitch) {
    this.id = id;
    this.nombre = nombre;
    this.temperaturaEsperada = temperaturaEsperada;
    this.potenciaKW = potenciaKW;
    this.idTermostato = idTermostato;
    this.topicTermostato = topicTermostato;
    this.idSwitch = idSwitch;
    this.urlSwitch = urlSwitch;
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

  public double getPotenciaKW() {
    return potenciaKW;
  }

  public String getIdTermostato() {
    return idTermostato;
  }

  public String getTopicTermostato() {
    return topicTermostato;
  }
  ;

  public String getIdSwitch() {
    return idSwitch;
  }
  ;

  public String getUrlSwitch() {
    return urlSwitch;
  }

  public void setNombre(String nombre) {
    this.nombre = nombre;
  }

  public void setTemperaturaEsperada(double temperaturaEsperada) {
    this.temperaturaEsperada = temperaturaEsperada;
  }

  public void setPotenciaKW(double potenciaKW) {
    this.potenciaKW = potenciaKW;
  }

  public void setIdTermostato(String idTermostato) {
    this.idTermostato = idTermostato;
  }

  public void setTopicTermostato(String topicTermostato) {
    this.topicTermostato = topicTermostato;
  }

  public void setIdSwitch(String idSwitch) {
    this.idSwitch = idSwitch;
  }

  public void setUrlSwitch(String urlSwitch) {
    this.urlSwitch = urlSwitch;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Room room = (Room) o;
    if (id != null && room.id != null) {
      return Objects.equals(id, room.id);
    }
    return Objects.equals(id, room.id)
        && Objects.equals(nombre, room.nombre)
        && Double.compare(room.temperaturaEsperada, temperaturaEsperada) == 0
        && Double.compare(room.potenciaKW, potenciaKW) == 0
        && Objects.equals(idTermostato, room.idTermostato)
        && Objects.equals(topicTermostato, room.topicTermostato)
        && Objects.equals(idSwitch, room.idSwitch)
        && Objects.equals(urlSwitch, room.urlSwitch);
  }

  @Override
  public int hashCode() {
    return id != null
        ? Objects.hashCode(id)
        : Objects.hash(
            nombre,
            temperaturaEsperada,
            potenciaKW,
            idTermostato,
            topicTermostato,
            idSwitch,
            urlSwitch);
  }

  @Override
  public String toString() {
    return "Room{"
        + "id='"
        + id
        + '\''
        + ", nombre='"
        + nombre
        + '\''
        + ", temperaturaEsperada="
        + temperaturaEsperada
        + ", potenciaKW="
        + potenciaKW
        + ", idTermostato='"
        + idTermostato
        + '\''
        + ", topicTermostato='"
        + topicTermostato
        + '\''
        + ", idSwitch='"
        + idSwitch
        + '\''
        + ", urlSwitch='"
        + urlSwitch
        + '\''
        + '}';
  }
}
