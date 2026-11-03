package com.example.floodmonitor.service;

import com.example.floodmonitor.model.*;
import com.example.floodmonitor.repository.FloodRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import java.time.*;

@Service
public class SimulationService {
    private final FloodRepository repository;
    private final WarningService warnings;
    private final Clock clock;
    public SimulationService(FloodRepository repository, WarningService warnings, Clock clock) {
        this.repository = repository;
        this.warnings = warnings;
        this.clock = clock;
    }
    private final java.util.Random random = new java.util.Random();

    // A bounded random walk keeps successive readings correlated, rather than resampling the river.
    public synchronized void tick() {
        Instant now = clock.instant();
        for (Station station : repository.stations()) {
            if (!station.isActive()) continue;
            Measurement previous = repository.latest(station.id()).orElse(null);
            if (previous != null && (previous.status() == StationStatus.OFFLINE
                    || !previous.timestamp().isBefore(now) || !warnings.valid(previous))) continue;
            double oldWater = previous == null ? station.normalWaterLevel() : previous.waterLevel();
            double rain = clamp((previous == null ? 2 : previous.rainfall()) * 0.94 + random.nextDouble() * 2, 0, 80);
            double change = clamp(rain * 0.12 - 0.4 + (random.nextDouble() - 0.5) * 0.6, -3, 5);
            double water = round(Math.max(0, oldWater + change));
            double temperature = round(clamp((previous == null ? 14 : previous.temperature())
                + (random.nextDouble() - 0.5) * 0.4, -30, 45));
            double battery = Math.max(0, (previous == null ? 95 : previous.batteryLevel()) - 0.02);
            StationStatus status = battery <= 0 ? StationStatus.OFFLINE
                : battery < 10 ? StationStatus.MAINTENANCE : StationStatus.ONLINE;
            Measurement next = new Measurement(station.id(), now, water, round(water * 2.2),
                round(rain), temperature, Math.round(battery * 100.0) / 100.0, status, WarningLevel.UNKNOWN);
            repository.save(next.withWarning(warnings.classify(station, next, true)));
        }
    }
    private double clamp(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }
    private double round(double value) { return Math.round(value * 10.0) / 10.0; }

    @PostConstruct
    public void seed() {
        Instant now = clock.instant();
        int index = 0;
        for (Station station : repository.stations()) {
            double target = new double[] {260, 340, 370}[index++ % 3];
            if (!station.isActive() || repository.latest(station.id()).isPresent()) continue;
            for (int i = 11; i >= 0; i--) {
                double water = target - i * 0.3;
                Measurement m = new Measurement(station.id(), now.minusSeconds(i * 10L), water,
                    water * 2.2, 2, 14, 95, StationStatus.ONLINE, WarningLevel.UNKNOWN);
                repository.save(m.withWarning(warnings.classify(station, m, false)));
            }
        }
    }
}
