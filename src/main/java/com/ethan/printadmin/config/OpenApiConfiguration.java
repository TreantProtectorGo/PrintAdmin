package com.ethan.printadmin.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {
    @Bean
    public OpenAPI printAdminApi() {
        return new OpenAPI().info(new Info().title("PrintAdmin API").version("v1")
                .description("Manage users, printers, and print-job records. Monthly quotas use the configured "
                        + "calendar timezone (Asia/Hong_Kong by default). This service does not send documents to printers."));
    }
}
