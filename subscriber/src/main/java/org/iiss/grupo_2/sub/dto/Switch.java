package org.iiss.grupo_2.sub.dto;

public class Switch {
    private String id;
    private Boolean encendido;

    public Switch() {}

    public Switch(String id, Boolean encendido) {
        this.id = id;
        this.encendido = encendido;
    }

    public String getId() {
        return this.id;
    }

    public Boolean getEncendido() {
        return this.encendido;
    }
}
