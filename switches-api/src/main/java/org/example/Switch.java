package org.example;

public class Switch {

    private final String id;
    private boolean encendido;

    public Switch(String id, boolean encendido) {
        this.id = id;
        this.encendido = encendido;
    }

    public String getId() {
        return id;
    }

    public boolean isEncendido() {
        return encendido;
    }

    public void setEncendido(boolean encendido) {
        this.encendido = encendido;
    }
}