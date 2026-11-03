package com.example.floodmonitor.service;

import com.example.floodmonitor.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.*;

@Service
public class WarningService {
    private final Clock clock;
    private final long maxAgeMillis;
    public WarningService(Clock clock, @Value("${measurement.max-age-ms:60000}") long maxAgeMillis) {
        if (maxAgeMillis < 1) throw new IllegalArgumentException("max-age-ms must be positive");
        this.clock = clock;
        this.maxAgeMillis = maxAgeMillis;
    }
    public boolean valid(Measurement m) {
        return m != null && m.timestamp() != null && m.status() != null && m.status() != StationStatus.OFFLINE
            && Double.isFinite(m.waterLevel()) && m.waterLevel() >= 0
            && Double.isFinite(m.flowRate()) && m.flowRate() >= 0
            && Double.isFinite(m.rainfall()) && m.rainfall() >= 0
            && Double.isFinite(m.temperature()) && m.temperature() >= -60 && m.temperature() <= 60
            && Double.isFinite(m.batteryLevel()) && m.batteryLevel() >= 0 && m.batteryLevel() <= 100;
    }
    public WarningLevel classify(Station station, Measurement m, boolean requireCurrent) {
        if (!station.isActive() || !valid(m)) return WarningLevel.UNKNOWN;
        Instant now = clock.instant();
        if (m.timestamp().isAfter(now) || (requireCurrent && m.timestamp().isBefore(now.minusMillis(maxAgeMillis))))
            return WarningLevel.UNKNOWN;
        if (m.waterLevel() >= station.criticalWaterLevel()) return WarningLevel.CRITICAL;
        if (m.waterLevel() >= station.warningWaterLevel()) return WarningLevel.WARNING;
        return WarningLevel.NORMAL;
    }
}
