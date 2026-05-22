package com.raisetimeline.backend.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "JWT access token. POST /api/auth/login で取得してください。"
)
public class OpenApiConfig {

    @Bean
    public OpenAPI raiseTimelineOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("RaiseTimeline API")
                .version("1.0.0")
                .description("ソーシャルタイムライン API。認証が必要なエンドポイントは Authorize ボタンで JWT を設定してください。")
            )
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
