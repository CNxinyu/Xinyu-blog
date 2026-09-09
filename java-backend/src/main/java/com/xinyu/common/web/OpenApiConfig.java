package com.xinyu.common.web;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.annotations.tags.Tag;

@Configuration(proxyBeanMethods = false)
@OpenAPIDefinition(
        info = @Info(
                title = "Xinyu Blog API",
                version = "v1",
                description = "HTTP API for the Xinyu Blog modular monolith"),
        tags = {
                @Tag(name = "Auth", description = "Registration, login and token lifecycle"),
                @Tag(name = "User", description = "Profile management, roles and administrator user management"),
                @Tag(name = "Article", description = "Markdown articles and publication lifecycle"),
                @Tag(name = "Taxonomy", description = "Article categories and tags"),
                @Tag(name = "Comment", description = "Article comments and moderation")
        })
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER)
public class OpenApiConfig {
}
