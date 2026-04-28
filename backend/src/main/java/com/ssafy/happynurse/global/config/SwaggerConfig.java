package com.ssafy.happynurse.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @Bean
    public OpenAPI openAPI() {
        String jwtScheme = "bearerAuth";

        OpenAPI api = new OpenAPI()
            .info(new Info()
                .title("HappyNurse API")
                .description("HappyNurse 서비스 API 문서")
                .version("v1.0.0"));

        // 환경별 server URL 분기
        if ("prod".equals(activeProfile)) {
            api.addServersItem(new Server()
                .url("https://k14e101.p.ssafy.io/api")
                .description("Prod 서버"));
        } else if ("dev".equals(activeProfile)) {
            api.addServersItem(new Server()
                .url("https://k14e101.p.ssafy.io/dev/api")
                .description("Dev 서버"));
        }

        // Local은 항상 추가 (개발자가 swagger 로컬 띄울 때 유용)
        api.addServersItem(new Server()
            .url("http://localhost:8080")
            .description("Local 서버"));

        return api
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

                if ("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method)) {
                    operation.getResponses()
                        .addApiResponse("400", new ApiResponse().description("유효하지 않은 데이터 (BadRequestException)"))
                        .addApiResponse("409", new ApiResponse().description("데이터 중복 발생 (DuplicateResourceException)"));
                }

                if ("GET".equals(method) || "PUT".equals(method) || "PATCH".equals(method) || "DELETE".equals(method)) {
                    operation.getResponses()
                        .addApiResponse("404", new ApiResponse().description("요청한 리소스를 찾을 수 없음 (ResourceNotFoundException)"));
                }
            });
        });
    }
}