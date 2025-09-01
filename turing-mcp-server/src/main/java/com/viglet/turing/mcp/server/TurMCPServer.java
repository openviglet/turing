package com.viglet.turing.mcp.server;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class TurMCPServer {

	private final TurToolsService service;

	public TurMCPServer(TurToolsService service) {
		this.service = service;
	}

	@Bean
	public ToolCallbackProvider turingService() {
		return MethodToolCallbackProvider.builder().toolObjects(service).build();
	}

	public static void main(String[] args) {
		SpringApplication.run(TurMCPServer.class, args);
	}
}
