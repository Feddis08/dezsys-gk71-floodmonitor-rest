package com.example.floodmonitor.model;

import java.time.Instant;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Inclusive time window over retained valid measurements. Water: cm, flow: m³/s. "
    + "totalRainfall is the sum of rainfall readings (mm/h), not integrated precipitation. "
    + "Warning and critical counts are disjoint. Empty windows return null extrema/averages and zero counts/sum.")
@JacksonXmlRootElement(localName = "statistics")
public record Statistics(String stationId, Instant from, Instant to, int measurementCount,
                         Double minWaterLevel, Double maxWaterLevel, Double averageWaterLevel,
                         Double averageFlowRate, double totalRainfall, long warningCount, long criticalCount) {}
