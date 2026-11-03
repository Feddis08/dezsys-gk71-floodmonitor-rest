package com.example.floodmonitor.config;

import com.example.floodmonitor.exception.ApiError;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.*;
import java.math.BigDecimal;
import java.util.*;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI floodApi() {
        return new OpenAPI().info(new Info().title("Floodmonitor API").version("v1")
            .description("Public read-only flood monitoring API. No authentication is required. "
                + "Only GET endpoints are provided; no administrative writes. No permissive CORS. "
                + "Accept application/json (default) or application/xml. XML lists use "
                + "<stations><station>…</station></stations> and <measurements><measurement>…</measurement></measurements>. "
                + "Water levels are cm, flow m³/s, rainfall mm/h, temperature °C and battery %. "
                + "NORMAL below warning threshold, WARNING from warning threshold, CRITICAL from critical threshold. "
                + "UNKNOWN for missing, invalid, inactive, offline, future or stale current data; historical values are classified at measurement time. "
                + "History is bounded by history.capacity (default 1000 per station)."))
            .security(new ArrayList<>());
    }
    @Bean
    public OpenApiCustomizer describeResponses() {
        return api -> {
            ModelConverters.getInstance().read(ApiError.class).forEach(api.getComponents()::addSchemas);
            api.getPaths().forEach((path, item) -> item.readOperations().forEach(operation -> {
                operation.setSecurity(new ArrayList<>());
                for (String code : List.of("400", "404", "405", "406", "500")) {
                    String description = switch (code) {
                        case "400" -> "Invalid query parameter, date, enum, boolean, range or limit";
                        case "404" -> "Station, measurement or resource not found";
                        case "405" -> "Read-only API: method not allowed";
                        case "406" -> "Unsupported Accept; structured error falls back to JSON";
                        default -> "Internal error without stack trace or implementation details";
                    };
                    Content content = new Content();
                    for (String format : List.of("application/json", "application/xml"))
                        content.addMediaType(format, new MediaType().schema(new Schema<>().$ref("#/components/schemas/ApiError")));
                    operation.getResponses().addApiResponse(code, new ApiResponse().description(description).content(content));
                }
                if (operation.getParameters() == null) return;
                for (var parameter : operation.getParameters()) {
                    String description = switch (parameter.getName()) {
                        case "stationId" -> "Station identifier, e.g. ST-001; unknown IDs return 404";
                        case "river" -> "Optional case-insensitive exact river name, e.g. Donau";
                        case "isActive" -> "Optional literal true or false; other values return 400";
                        case "warningLevel" -> "Optional enum NORMAL, WARNING, CRITICAL, UNKNOWN. Alerts include only current WARNING/CRITICAL";
                        case "from" -> "Optional inclusive ISO-8601 instant, e.g. 2026-05-01T08:00:00Z";
                        case "to" -> "Optional inclusive ISO-8601 instant, must be greater than or equal to from";
                        case "limit" -> "Maximum returned readings, 1..1000 (default 100), newest first; applied after filters";
                        default -> parameter.getName();
                    };
                    parameter.setDescription(description);
                    if (parameter.getName().equals("limit")) {
                        parameter.getSchema().setMinimum(BigDecimal.ONE);
                        parameter.getSchema().setMaximum(BigDecimal.valueOf(1000));
                    }
                    if (parameter.getName().equals("isActive"))
                        parameter.setSchema(new StringSchema()._enum(List.of("true", "false")));
                }
            }));
        };
    }
}
