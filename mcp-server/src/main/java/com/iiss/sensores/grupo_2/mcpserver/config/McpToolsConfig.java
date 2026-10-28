package com.iiss.sensores.grupo_2.mcpserver.config;

import com.iiss.sensores.grupo_2.mcpserver.tools.EstadoTools;
import com.iiss.sensores.grupo_2.mcpserver.tools.HabitacionTools;
import com.iiss.sensores.grupo_2.mcpserver.tools.SwitchTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers all MCP tool classes with the Spring AI MCP Server auto-configuration.
 *
 * <p>Spring AI 2.0.0 discovers tools through a {@link ToolCallbackProvider} bean.
 * {@link MethodToolCallbackProvider} scans the given objects for {@code @Tool}-annotated
 * methods and exposes each one as an MCP tool (with name, description and JSON schema
 * derived from the annotation and method signature).
 */
@Configuration
public class McpToolsConfig {

    @Bean
    public ToolCallbackProvider mcpTools(
            HabitacionTools habitacionTools,
            SwitchTools switchTools,
            EstadoTools estadoTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(habitacionTools, switchTools, estadoTools)
                .build();
    }
}
