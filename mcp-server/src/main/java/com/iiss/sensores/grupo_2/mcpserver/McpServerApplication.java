package com.iiss.sensores.grupo_2.mcpserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Aplicación Spring Boot que publica las herramientas MCP del sistema. */
@SpringBootApplication
public class McpServerApplication {

    /** Inicia el servidor MCP. */
    public static void main(String[] args) {
        SpringApplication.run(McpServerApplication.class, args);
    }
}
