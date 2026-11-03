package com.example.floodmonitor.controller;

import com.example.floodmonitor.model.*;
import com.example.floodmonitor.service.FloodService;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;

@RestController
@RequestMapping(value = "/api/v1", produces = {"application/json", "application/xml"})
public class StationController {
    private final FloodService service;
    public StationController(FloodService service) { this.service = service; }

    @Operation(summary = "List stations", description = "Filter by river, activity and current server-computed warning level. JSON array or XML stations/station list.")
    @GetMapping("/stations")
    public List<Station> stations(@RequestParam(required = false) String river,
            @RequestParam(required = false) String isActive, @RequestParam(required = false) WarningLevel warningLevel) {
        return service.stations(river, isActive, warningLevel);
    }
    @Operation(summary = "Get a station", description = "Thresholds in cm and geographic latitude/longitude in degrees.")
    @GetMapping("/stations/{stationId}")
    public Station station(@PathVariable String stationId) { return service.station(stationId); }

    @Operation(summary = "Latest retained measurement", description = "Current warning recalculated on every read; stale data is UNKNOWN. 404 if no measurement exists.")
    @GetMapping("/stations/{stationId}/measurements/latest")
    public Measurement latest(@PathVariable String stationId) { return service.latest(stationId); }

    @Operation(summary = "Measurement history", description = "Inclusive from/to window, newest first, filtered before limit. Historical warning is not invalidated by present age. JSON array or XML measurements/measurement list.")
    @GetMapping("/stations/{stationId}/measurements")
    public List<Measurement> history(@PathVariable String stationId,
            @RequestParam(required = false) Instant from, @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "100") int limit, @RequestParam(required = false) WarningLevel warningLevel) {
        return service.history(stationId, from, to, limit, warningLevel);
    }
    @Operation(summary = "Current flood alerts", description = "Latest WARNING or CRITICAL reading per active station, excluding stale/invalid/offline data. Optional river and warning filters. JSON array or XML measurements/measurement list.")
    @GetMapping("/alerts")
    public List<Measurement> alerts(@RequestParam(required = false) String river,
            @RequestParam(required = false) WarningLevel warningLevel) { return service.alerts(river, warningLevel); }

    @Operation(summary = "Statistics over retained measurements", description = "Inclusive from/to window, valid readings only. Empty windows have null extrema/averages, zero counts/sum. totalRainfall is the sum of mm/h readings, not integrated rainfall. warningCount excludes criticalCount.")
    @GetMapping("/stations/{stationId}/statistics")
    public Statistics statistics(@PathVariable String stationId,
            @RequestParam(required = false) Instant from, @RequestParam(required = false) Instant to) {
        return service.statistics(stationId, from, to);
    }
}
