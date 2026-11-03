package com.example.floodmonitor;

import com.example.floodmonitor.model.*;
import com.example.floodmonitor.repository.FloodRepository;
import com.example.floodmonitor.service.*;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class FloodLogicTest {
    private final Instant now = Instant.parse("2026-05-01T12:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    private final Station station = new Station("ST-001", "Test", "Donau", new Location(48, 16), 200, 300, 400, true);
    private final WarningService warnings = new WarningService(clock, 60000);

    private Measurement measurement(double water, Instant timestamp, StationStatus status) {
        return new Measurement(station.id(), timestamp, water, 700, 20, 15, 90, status, WarningLevel.NORMAL);
    }
    @Test void exactThresholdsAreInclusive() {
        assertEquals(WarningLevel.NORMAL, warnings.classify(station, measurement(299.9, now, StationStatus.ONLINE), true));
        assertEquals(WarningLevel.WARNING, warnings.classify(station, measurement(300, now, StationStatus.ONLINE), true));
        assertEquals(WarningLevel.CRITICAL, warnings.classify(station, measurement(400, now, StationStatus.ONLINE), true));
    }
    @Test void absentStaleInvalidOfflineAndFutureAreUnknown() {
        assertEquals(WarningLevel.UNKNOWN, warnings.classify(station, null, true));
        for (double water : new double[] {-1, Double.NaN, Double.POSITIVE_INFINITY})
            assertEquals(WarningLevel.UNKNOWN, warnings.classify(station, measurement(water, now, StationStatus.ONLINE), true));
        assertEquals(WarningLevel.UNKNOWN, warnings.classify(station, measurement(500, now.minusMillis(60001), StationStatus.ONLINE), true));
        assertEquals(WarningLevel.CRITICAL, warnings.classify(station, measurement(500, now.minusMillis(60000), StationStatus.ONLINE), true));
        assertEquals(WarningLevel.UNKNOWN, warnings.classify(station, measurement(500, now.plusSeconds(1), StationStatus.ONLINE), true));
        assertEquals(WarningLevel.UNKNOWN, warnings.classify(station, measurement(500, now, StationStatus.OFFLINE), true));
    }
    @Test void historyRetainsRecordedWarningsAndIgnoresClientWarning() {
        FloodRepository repo = new FloodRepository(List.of(station), 100);
        repo.save(measurement(450, now.minusSeconds(100), StationStatus.ONLINE));
        FloodService service = new FloodService(repo, warnings);
        assertEquals(WarningLevel.UNKNOWN, service.latest(station.id()).warningLevel());
        assertEquals(WarningLevel.CRITICAL, service.history(station.id(), null, null, 100, null).get(0).warningLevel());
        assertEquals(1, service.statistics(station.id(), null, null).criticalCount());
    }
    @Test void emptyHistoryAndStatisticsAreDefined() {
        FloodService service = new FloodService(new FloodRepository(List.of(station), 10), warnings);
        assertEquals(WarningLevel.UNKNOWN, service.currentWarning(station));
        assertTrue(service.history(station.id(), null, null, 10, null).isEmpty());
        Statistics s = service.statistics(station.id(), null, null);
        assertEquals(0, s.measurementCount()); assertNull(s.averageWaterLevel()); assertEquals(0, s.totalRainfall());
        assertThrows(com.example.floodmonitor.exception.ApiException.class, () -> service.latest(station.id()));
    }
    @Test void repositoryIsBoundedOrderedAndSnapshotIsImmutable() throws Exception {
        FloodRepository repo = new FloodRepository(List.of(station), 20);
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            List<Callable<Void>> jobs = new ArrayList<>();
            for (int i = 0; i < 200; i++) {
                int n = i;
                jobs.add(() -> { repo.save(measurement(n, now.plusSeconds(n), StationStatus.ONLINE)); repo.history(station.id()); return null; });
            }
            for (Future<Void> future : pool.invokeAll(jobs)) future.get();
        } finally { pool.shutdownNow(); }
        assertEquals(20, repo.history(station.id()).size());
        assertEquals(now.plusSeconds(199), repo.latest(station.id()).orElseThrow().timestamp());
        assertEquals(now.plusSeconds(180), repo.history(station.id()).get(0).timestamp());
        assertThrows(UnsupportedOperationException.class, () -> repo.history(station.id()).clear());
    }
    @Test void simulationContinuesPreviousValuesAndRespectsPhysicalBounds() {
        FloodRepository repo = new FloodRepository(List.of(station), 100);
        Measurement previous = measurement(350, now.minusSeconds(10), StationStatus.ONLINE);
        repo.save(previous);
        SimulationService simulation = new SimulationService(repo, warnings, clock);
        simulation.tick();
        Measurement next = repo.latest(station.id()).orElseThrow();
        assertEquals(now, next.timestamp());
        assertTrue(Math.abs(next.waterLevel() - previous.waterLevel()) <= 5);
        assertTrue(next.waterLevel() >= previous.waterLevel(), "Heavy rain should raise the level");
        assertEquals(Math.round(next.waterLevel() * 2.2 * 10) / 10.0, next.flowRate());
        assertTrue(next.batteryLevel() >= 0 && next.batteryLevel() <= 100);
        assertTrue(next.rainfall() >= 0 && next.waterLevel() >= 0);
        simulation.tick();
        assertEquals(2, repo.history(station.id()).size(), "Same clock instant must not create duplicate readings");
    }
    @Test void simulationSkipsInactiveAndOfflineAndPreservesExistingHistoryOnSeed() {
        Station inactive = new Station("ST-002", "Inactive", "Donau", new Location(48, 16), 200, 300, 400, false);
        FloodRepository repo = new FloodRepository(List.of(station, inactive), 100);
        repo.save(measurement(350, now.minusSeconds(10), StationStatus.OFFLINE));
        SimulationService simulation = new SimulationService(repo, warnings, clock);
        simulation.seed();
        simulation.tick();
        assertEquals(1, repo.history(station.id()).size());
        assertTrue(repo.history(inactive.id()).isEmpty());
    }
    @Test void maximumLimitReturnsExactly1000FromLargerRetainedHistory() {
        FloodRepository repo = new FloodRepository(List.of(station), 2000);
        for (int i = 0; i < 1200; i++) repo.save(measurement(350, now.minusSeconds(i), StationStatus.ONLINE));
        FloodService service = new FloodService(repo, warnings);
        List<Measurement> history = service.history(station.id(), null, null, 1000, WarningLevel.WARNING);
        assertEquals(1000, history.size());
        assertEquals(now, history.get(0).timestamp());
        assertEquals(now.minusSeconds(999), history.get(999).timestamp());
    }
    @Test void exactStatisticsAndWindow() {
        FloodRepository repo = new FloodRepository(List.of(station), 100);
        repo.save(measurement(200, now.minusSeconds(20), StationStatus.ONLINE));
        repo.save(measurement(300, now.minusSeconds(10), StationStatus.ONLINE));
        repo.save(measurement(400, now, StationStatus.ONLINE));
        FloodService service = new FloodService(repo, warnings);
        Statistics s = service.statistics(station.id(), now.minusSeconds(10), now);
        assertEquals(2, s.measurementCount()); assertEquals(300, s.minWaterLevel());
        assertEquals(400, s.maxWaterLevel()); assertEquals(350, s.averageWaterLevel());
        assertEquals(700, s.averageFlowRate()); assertEquals(40, s.totalRainfall());
        assertEquals(1, s.warningCount()); assertEquals(1, s.criticalCount());
        assertEquals(1, service.history(station.id(), null, null, 1, WarningLevel.WARNING).size());
        assertEquals(300, service.history(station.id(), null, null, 1, WarningLevel.WARNING).get(0).waterLevel());
    }
    @Test void invalidSensorFieldsDoNotGenerateAlerts() {
        FloodRepository repo = new FloodRepository(List.of(station), 100);
        for (Measurement m : List.of(
                new Measurement(station.id(), now, 500, -1, 0, 15, 90, StationStatus.ONLINE, WarningLevel.CRITICAL),
                new Measurement(station.id(), now, 500, 500, -1, 15, 90, StationStatus.ONLINE, WarningLevel.CRITICAL),
                new Measurement(station.id(), now, 500, 500, 0, 100, 90, StationStatus.ONLINE, WarningLevel.CRITICAL),
                new Measurement(station.id(), now, 500, 500, 0, 15, 101, StationStatus.ONLINE, WarningLevel.CRITICAL))) {
            repo.save(m);
            assertEquals(WarningLevel.UNKNOWN, warnings.classify(station, m, true));
            assertTrue(new FloodService(repo, warnings).alerts(null, null).isEmpty());
        }
    }
    @Test void invalidThresholdsAndUnboundedCapacityAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new FloodRepository(10001));
        assertThrows(IllegalArgumentException.class, () -> new FloodRepository(0));
        assertThrows(IllegalArgumentException.class, () -> new Station("X", "X", "X", new Location(0, 0), 200, 300, 300, true));
    }
    @Test void lowBatteryTriggersMaintenance() {
        FloodRepository repo = new FloodRepository(List.of(station), 100);
        repo.save(new Measurement(station.id(), now.minusSeconds(10), 300, 660, 0, 15, 4, StationStatus.ONLINE, WarningLevel.WARNING));
        new SimulationService(repo, warnings, clock).tick();
        assertEquals(StationStatus.MAINTENANCE, repo.latest(station.id()).orElseThrow().status());
    }
}
