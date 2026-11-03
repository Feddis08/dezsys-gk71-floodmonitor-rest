package com.example.floodmonitor.repository;

import com.example.floodmonitor.model.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.*;

@Repository
public class FloodRepository {
    private final List<Station> stations;
    private final Map<String, NavigableMap<Instant, Measurement>> measurements = new HashMap<>();
    private final int capacity;

    @Autowired
    public FloodRepository(@Value("${history.capacity:1000}") int capacity) {
        this(List.of(
            new Station("ST-001", "Wien Reichsbrücke", "Donau", new Location(48.228, 16.413), 250, 350, 450, true),
            new Station("ST-002", "Linz", "Donau", new Location(48.310, 14.286), 220, 320, 420, true),
            new Station("ST-003", "Salzburg", "Salzach", new Location(47.802, 13.044), 150, 250, 350, true)), capacity);
    }
    public FloodRepository(List<Station> stations, int capacity) {
        if (capacity < 1 || capacity > 10000) throw new IllegalArgumentException("history.capacity must be 1..10000");
        this.stations = List.copyOf(stations);
        this.capacity = capacity;
        for (Station s : stations) measurements.put(s.id(), new TreeMap<>());
    }
    public List<Station> stations() { return stations; }
    public synchronized void save(Measurement measurement) {
        var history = measurements.get(measurement.stationId());
        if (history == null || measurement.timestamp() == null) throw new IllegalArgumentException("Unknown station or timestamp");
        history.put(measurement.timestamp(), measurement);
        while (history.size() > capacity) history.pollFirstEntry();
    }
    public synchronized Optional<Measurement> latest(String stationId) {
        var history = measurements.get(stationId);
        return history == null || history.isEmpty() ? Optional.empty() : Optional.of(history.lastEntry().getValue());
    }
    public synchronized List<Measurement> history(String stationId) {
        var history = measurements.get(stationId);
        return history == null ? List.of() : List.copyOf(history.values());
    }
}
