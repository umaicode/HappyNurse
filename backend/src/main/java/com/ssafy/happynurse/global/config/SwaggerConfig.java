package com.ssafy.happynurse.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        String jwtScheme = "bearerAuth";

        return new OpenAPI()
            .info(new Info()
                .title("HappyNurse API")
                .description("HappyNurse 서비스 API 문서")
                .version("v1.0.0"))            .addServersItem(new Server().url("http://localhost:8080")
                .description("Local 서버"))
            .addServersItem(new Server().url("https://j14e101.p.ssafy.io:dev")
                .description("Dev 서버"))
            .addServersItem(new Server().url("https://j14e101.p.ssafy.io")
                .description("Prod 서버"))

            .addSecurityItem(new SecurityRequirement().addList(jwtScheme))
            .components(new Components()
                .addSecuritySchemes(jwtScheme, new SecurityScheme()
                    .name(jwtScheme)
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")));
    }

    @Bean
    public OpenApiCustomizer globalOpenApiCustomizer() {
        return openApi -> openApi.getPaths().values().forEach(pathItem -> {
            pathItem.readOperationsMap().forEach((httpMethod, operation) -> {
                String method = httpMethod.name();

                operation.getResponses()
                    .addApiResponse("401", new ApiResponse().description("인증되지 않은 사용자 (UnAuthorizedException)"))
                    .addApiResponse("403", new ApiResponse().description("권한이 없는 사용자 요청 (ForbiddenException)"))
                    .addApiResponse("500", new ApiResponse().description("서버 내부 오류 (InternalServerError)"));

                // POST, PUT, PATCH (데이터를 생성/수정하는 조작)에만 400, 409 추가
                if ("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method)) {
                    operation.getResponses()
                        .addApiResponse("400", new ApiResponse().description("유효하지 않은 데이터 (BadRequestException)"))
                        .addApiResponse("409", new ApiResponse().description("데이터 중복 발생 (DuplicateResourceException)"));
                }

                // GET, PUT, PATCH, DELETE (특정 리소스를 다루는 경우)에 404 추가
                if ("GET".equals(method) || "PUT".equals(method) || "PATCH".equals(method) || "DELETE".equals(method)) {
                    operation.getResponses()
                        .addApiResponse("404", new ApiResponse().description("요청한 리소스를 찾을 수 없음 (ResourceNotFoundException)"));
                }
            });
        });
    }
}