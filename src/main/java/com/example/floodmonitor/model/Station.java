package com.example.floodmonitor.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "station")
public record Station(String id, String name, String river, Location location,
                      double normalWaterLevel, double warningWaterLevel, double criticalWaterLevel,
                      @JsonProperty("isActive") boolean isActive) {
    public Station {
        if (id == null || location == null || !Double.isFinite(normalWaterLevel)
                || !Double.isFinite(warningWaterLevel) || !Double.isFinite(criticalWaterLevel)
                || normalWaterLevel < 0 || warningWaterLevel <= normalWaterLevel
                || criticalWaterLevel <= warningWaterLevel) {
            throw new IllegalArgumentException("Station thresholds must be finite and increasing");
        }
    }
}
