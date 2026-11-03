package com.example.floodmonitor.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "simulation.enabled", havingValue = "true", matchIfMissing = true)
public class SimulationScheduler {
    private final SimulationService simulation;
    public SimulationScheduler(SimulationService simulation) { this.simulation = simulation; }

    @Scheduled(fixedDelayString = "${simulation.interval:10000}", initialDelayString = "${simulation.interval:10000}")
    public void update() { simulation.tick(); }
}
