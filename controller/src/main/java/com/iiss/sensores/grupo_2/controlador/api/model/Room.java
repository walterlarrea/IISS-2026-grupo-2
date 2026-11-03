package com.iiss.sensores.grupo_2.controlador.api.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Objects;

/** Modelo persistente de la configuración de una habitación. */
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

  /** Constructor vacío requerido para la conversión de documentos. */
  public Room() {}

  /** Crea una habitación nueva sin asignar un identificador explícito. */
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

  /** Crea una habitación con su identificador y configuración completa. */
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

  /** Devuelve el identificador de la habitación. */
  public String getId() {
    return id;
  }

  /** Asigna el identificador de la habitación. */
  public void setId(String id) {
    this.id = id;
  }

  /** Devuelve el nombre visible de la habitación. */
  public String getNombre() {
    return nombre;
  }

  /** Devuelve la temperatura objetivo de la habitación. */
  public double getTemperaturaEsperada() {
    return temperaturaEsperada;
  }

  /** Devuelve la potencia del equipo de climatización en kW. */
  public double getPotenciaKW() {
    return potenciaKW;
  }

  /** Devuelve el identificador del termostato asociado. */
  public String getIdTermostato() {
    return idTermostato;
  }

  /** Devuelve el topic MQTT del termostato. */
  public String getTopicTermostato() {
    return topicTermostato;
  }
  ;

  /** Devuelve el identificador del switch asociado. */
  public String getIdSwitch() {
    return idSwitch;
  }
  ;

  /** Devuelve la URL base de la API del switch. */
  public String getUrlSwitch() {
    return urlSwitch;
  }

  /** Actualiza el nombre visible de la habitación. */
  public void setNombre(String nombre) {
    this.nombre = nombre;
  }

  /** Actualiza la temperatura objetivo de la habitación. */
  public void setTemperaturaEsperada(double temperaturaEsperada) {
    this.temperaturaEsperada = temperaturaEsperada;
  }

  /** Actualiza la potencia del equipo de climatización. */
  public void setPotenciaKW(double potenciaKW) {
    this.potenciaKW = potenciaKW;
  }

  /** Actualiza el identificador del termostato asociado. */
  public void setIdTermostato(String idTermostato) {
    this.idTermostato = idTermostato;
  }

  /** Actualiza el topic MQTT del termostato. */
  public void setTopicTermostato(String topicTermostato) {
    this.topicTermostato = topicTermostato;
  }

  /** Actualiza el identificador del switch asociado. */
  public void setIdSwitch(String idSwitch) {
    this.idSwitch = idSwitch;
  }

  /** Actualiza la URL base de la API del switch. */
  public void setUrlSwitch(String urlSwitch) {
    this.urlSwitch = urlSwitch;
  }

  /** Compara habitaciones por identificador o, si no existe, por sus datos. */
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

  /** Calcula el hash usando la misma identidad que {@link #equals(Object)}. */
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

  /** Devuelve una representación textual de la configuración de la habitación. */
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
