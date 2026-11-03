package com.example.floodmonitor.service;

import com.example.floodmonitor.model.*;
import com.example.floodmonitor.repository.FloodRepository;
import com.example.floodmonitor.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.*;

@Service
public class FloodService {
    private final FloodRepository repository;
    private final WarningService warnings;
    public FloodService(FloodRepository repository, WarningService warnings) {
        this.repository = repository;
        this.warnings = warnings;
    }
    public Station station(String id) {
        return repository.stations().stream().filter(s -> s.id().equals(id)).findFirst()
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Station " + id + " not found"));
    }
    public List<Station> stations(String river, String active, WarningLevel warning) {
        Boolean isActive = null;
        if (active != null) {
            if (!active.equals("true") && !active.equals("false")) throw new IllegalArgumentException("Invalid isActive");
            isActive = Boolean.valueOf(active);
        }
        final Boolean requestedActive = isActive;
        return repository.stations().stream()
            .filter(s -> river == null || s.river().equalsIgnoreCase(river))
            .filter(s -> requestedActive == null || s.isActive() == requestedActive)
            .filter(s -> warning == null || currentWarning(s) == warning).toList();
    }
    public WarningLevel currentWarning(Station s) {
        return warnings.classify(s, repository.latest(s.id()).orElse(null), true);
    }
    public Measurement latest(String id) {
        Station station = station(id);
        Measurement m = repository.latest(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No measurement for station " + id));
        return m.withWarning(warnings.classify(station, m, true));
    }
    public List<Measurement> history(String id, Instant from, Instant to, int limit, WarningLevel warning) {
        Station station = station(id);
        validateRange(from, to);
        if (limit < 1 || limit > 1000) throw new IllegalArgumentException("Limit must be 1..1000");
        return window(id, from, to).stream()
            .map(m -> m.withWarning(warnings.classify(station, m, false)))
            .filter(m -> warning == null || warning == m.warningLevel())
            .sorted(Comparator.comparing(Measurement::timestamp).reversed()).limit(limit).toList();
    }
    public List<Measurement> alerts(String river, WarningLevel warning) {
        return stations(river, null, null).stream()
            .filter(s -> repository.latest(s.id()).isPresent()).map(s -> latest(s.id()))
            .filter(m -> m.warningLevel() == WarningLevel.WARNING || m.warningLevel() == WarningLevel.CRITICAL)
            .filter(m -> warning == null || m.warningLevel() == warning).toList();
    }
    public Statistics statistics(String id, Instant from, Instant to) {
        Station station = station(id);
        validateRange(from, to);
        List<Measurement> values = window(id, from, to).stream().filter(warnings::valid).toList();
        var water = values.stream().mapToDouble(Measurement::waterLevel).summaryStatistics();
        return new Statistics(id, from, to, values.size(), values.isEmpty() ? null : water.getMin(),
            values.isEmpty() ? null : water.getMax(), values.isEmpty() ? null : water.getAverage(),
            values.isEmpty() ? null : values.stream().mapToDouble(Measurement::flowRate).average().orElseThrow(),
            values.stream().mapToDouble(Measurement::rainfall).sum(),
            values.stream().filter(m -> warnings.classify(station, m, false) == WarningLevel.WARNING).count(),
            values.stream().filter(m -> warnings.classify(station, m, false) == WarningLevel.CRITICAL).count());
    }
    private List<Measurement> window(String id, Instant from, Instant to) {
        return repository.history(id).stream()
            .filter(m -> from == null || !m.timestamp().isBefore(from))
            .filter(m -> to == null || !m.timestamp().isAfter(to)).toList();
    }
    private void validateRange(Instant from, Instant to) {
        if (from != null && to != null && from.isAfter(to)) throw new IllegalArgumentException("from must not be after to");
    }
}
