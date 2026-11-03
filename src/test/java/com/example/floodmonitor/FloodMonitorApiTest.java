package com.example.floodmonitor;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "simulation.enabled=false")
@AutoConfigureMockMvc
class FloodMonitorApiTest {
    @Autowired MockMvc mvc;
    @Autowired org.springframework.context.ApplicationContext context;

    @org.springframework.boot.test.context.TestConfiguration
    static class FixedTime {
        @org.springframework.context.annotation.Bean
        @org.springframework.context.annotation.Primary
        java.time.Clock testClock() {
            return java.time.Clock.fixed(java.time.Instant.parse("2026-05-01T12:00:00Z"), java.time.ZoneOffset.UTC);
        }
    }

    @Test void simulationSchedulerIsDisabledForTests() {
        org.junit.jupiter.api.Assertions.assertTrue(context.getBeansOfType(
            com.example.floodmonitor.service.SimulationScheduler.class).isEmpty());
    }

    @Test void openApiDocumentsPublicReadOnlyContractAndFormats() throws Exception {
        mvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.info.description").value(org.hamcrest.Matchers.containsString("No authentication")))
            .andExpect(jsonPath("$.paths['/api/v1/stations'].get.responses['400'].content['application/xml']").exists())
            .andExpect(jsonPath("$.paths['/api/v1/stations/{stationId}/measurements'].get.parameters[?(@.name=='limit')].schema.maximum").value(org.hamcrest.Matchers.hasItem(1000)))
            .andExpect(jsonPath("$.paths['/api/v1/stations'].post").doesNotExist());
    }

    @Test void jsonAndXmlMeasurementHaveMatchingFields() throws Exception {
        String path = "/api/v1/stations/ST-002/measurements/latest";
        var json = new com.fasterxml.jackson.databind.ObjectMapper().readTree(mvc.perform(get(path))
            .andReturn().getResponse().getContentAsString());
        var xml = new com.fasterxml.jackson.dataformat.xml.XmlMapper().readTree(mvc.perform(get(path).accept("application/xml"))
            .andReturn().getResponse().getContentAsString());
        var fields = json.fieldNames();
        while (fields.hasNext()) {
            String field = fields.next();
            org.junit.jupiter.api.Assertions.assertEquals(json.get(field).asText(), xml.get(field).asText(), field);
        }
    }

    @Test void latestHasAllFieldsAndIsoInstant() throws Exception {
        mvc.perform(get("/api/v1/stations/ST-001/measurements/latest"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.stationId").value("ST-001"))
            .andExpect(jsonPath("$.timestamp").value(org.hamcrest.Matchers.endsWith("Z")))
            .andExpect(jsonPath("$.waterLevel").isNumber()).andExpect(jsonPath("$.flowRate").isNumber())
            .andExpect(jsonPath("$.rainfall").isNumber()).andExpect(jsonPath("$.temperature").isNumber())
            .andExpect(jsonPath("$.batteryLevel").isNumber()).andExpect(jsonPath("$.status").value("ONLINE"))
            .andExpect(jsonPath("$.warningLevel").value("NORMAL"));
    }

    @Test void historyFiltersInclusiveTimeRange() throws Exception {
        String latest = new com.fasterxml.jackson.databind.ObjectMapper().readTree(mvc.perform(
            get("/api/v1/stations/ST-001/measurements/latest")).andReturn().getResponse().getContentAsString())
            .get("timestamp").asText();
        mvc.perform(get("/api/v1/stations/ST-001/measurements").param("from", latest).param("to", latest))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
    }

    @Test void historyXmlUsesMeasurementElements() throws Exception {
        mvc.perform(get("/api/v1/stations/ST-001/measurements").param("limit", "2").accept("application/xml"))
            .andExpect(status().isOk()).andExpect(xpath("/measurements/measurement").nodeCount(2));
    }

    @Test void acceptsMaximumLimit() throws Exception {
        mvc.perform(get("/api/v1/stations/ST-001/measurements").param("limit", "1000"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(12));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"ST-001,NORMAL", "ST-002,WARNING", "ST-003,CRITICAL"})
    void computesWarningLevels(String id, String warning) throws Exception {
        mvc.perform(get("/api/v1/stations/" + id + "/measurements/latest"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.warningLevel").value(warning));
    }

    @Test void stationFiltersAndAlerts() throws Exception {
        mvc.perform(get("/api/v1/stations").param("river", "Donau").param("isActive", "true").param("warningLevel", "WARNING"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value("ST-002"));
        mvc.perform(get("/api/v1/alerts").param("river", "Salzach").param("warningLevel", "CRITICAL"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].stationId").value("ST-003"));
        mvc.perform(get("/api/v1/alerts").accept("application/xml"))
            .andExpect(status().isOk()).andExpect(xpath("/measurements/measurement").nodeCount(2));
    }

    @Test void statisticsIncludeRequiredAggregates() throws Exception {
        mvc.perform(get("/api/v1/stations/ST-002/statistics"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.measurementCount").value(12))
            .andExpect(jsonPath("$.warningCount").value(12)).andExpect(jsonPath("$.criticalCount").value(0))
            .andExpect(jsonPath("$.minWaterLevel").isNumber()).andExpect(jsonPath("$.maxWaterLevel").isNumber())
            .andExpect(jsonPath("$.averageWaterLevel").isNumber()).andExpect(jsonPath("$.averageFlowRate").isNumber())
            .andExpect(jsonPath("$.totalRainfall").isNumber());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {
        "/stations?isActive=yes", "/stations?isActive=1", "/stations?warningLevel=FLOOD",
        "/alerts?warningLevel=BAD", "/stations/ST-001/measurements?limit=0",
        "/stations/ST-001/measurements?limit=-1", "/stations/ST-001/measurements?limit=1001",
        "/stations/ST-001/measurements?limit=abc", "/stations/ST-001/measurements?from=not-a-date",
        "/stations/ST-001/measurements?from=2026-01-02T00:00:00Z&to=2026-01-01T00:00:00Z",
        "/stations/ST-001/statistics?from=bad", "/stations/ST-001/measurements?warningLevel=BAD"})
    void invalidParametersAreStructured400(String path) throws Exception {
        mvc.perform(get("/api/v1" + path)).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400)).andExpect(jsonPath("$.message").isString())
            .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test void xmlErrorsAndReadOnlyBoundary() throws Exception {
        mvc.perform(get("/api/v1/stations/ST-999").accept("application/xml"))
            .andExpect(status().isNotFound()).andExpect(xpath("/error/status").string("404"));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/stations"))
            .andExpect(status().isMethodNotAllowed());
        mvc.perform(get("/api/v1/stations").header("Origin", "https://evil.example"))
            .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test void listsStationsAsXml() throws Exception {
        mvc.perform(get("/api/v1/stations").accept("application/xml"))
            .andExpect(status().isOk()).andExpect(xpath("/stations/station").nodeCount(3))
            .andExpect(xpath("/stations/station[1]/id").string("ST-001"));
    }

    @Test void unknownStationIsStructuredJson404() throws Exception {
        mvc.perform(get("/api/v1/stations/ST-999"))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.path").value("/api/v1/stations/ST-999"))
            .andExpect(jsonPath("$.timestamp").exists()).andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test void findsExistingStation() throws Exception {
        mvc.perform(get("/api/v1/stations/ST-001"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.id").value("ST-001"));
    }

    @Test void unsupportedFormatIs406() throws Exception {
        mvc.perform(get("/api/v1/stations").accept("text/csv"))
            .andExpect(status().isNotAcceptable());
    }

    @Test void listsStationsAsJsonByDefault() throws Exception {
        mvc.perform(get("/api/v1/stations"))
            .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith("application/json"))
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].id").value("ST-001"))
            .andExpect(jsonPath("$[0].isActive").value(true))
            .andExpect(jsonPath("$[0].location.latitude").isNumber());
    }
}
