package com.iqra.zakat.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end acceptance test: runs every Flyway migration against a real PostgreSQL, checks the imported
 * history against the Phase 1 benchmark totals, then drives the REST API through an allocation lifecycle.
 *
 * Database used, in order of preference:
 *   1. ZAKAT_IT_JDBC_URL (+ ZAKAT_IT_DB_USER / ZAKAT_IT_DB_PASSWORD) — must point at an EMPTY database.
 *   2. A throwaway postgres:16-alpine container (Testcontainers), when Docker is running.
 * If neither is available the test is skipped, not failed.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIf("databaseAvailable")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ReconciliationIntegrationTest {

    private static final String EXTERNAL_URL = System.getenv("ZAKAT_IT_JDBC_URL");
    private static PostgreSQLContainer<?> postgres;

    static boolean databaseAvailable() {
        if (EXTERNAL_URL != null && !EXTERNAL_URL.isBlank()) return true;
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable t) {
            return false;
        }
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        if (EXTERNAL_URL != null && !EXTERNAL_URL.isBlank()) {
            registry.add("spring.datasource.url", () -> EXTERNAL_URL);
            registry.add("spring.datasource.username", () -> envOr("ZAKAT_IT_DB_USER", "postgres"));
            registry.add("spring.datasource.password", () -> envOr("ZAKAT_IT_DB_PASSWORD", "postgres"));
        } else {
            if (postgres == null) {
                postgres = new PostgreSQLContainer<>("postgres:16-alpine");
                postgres.start();
            }
            registry.add("spring.datasource.url", postgres::getJdbcUrl);
            registry.add("spring.datasource.username", postgres::getUsername);
            registry.add("spring.datasource.password", postgres::getPassword);
        }
    }

    private static String envOr(String key, String fallback) {
        String v = System.getenv(key);
        return v == null || v.isBlank() ? fallback : v;
    }

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private TestRestTemplate rest;

    private final ObjectMapper json = new ObjectMapper();

    // ------------------------------------------------------------------ data reconciliation

    @Test
    @Order(1)
    void importedRowCountsMatchSource() {
        assertThat(count("zk_allocation")).isEqualTo(965);
        assertThat(count("zk_beneficiary")).isEqualTo(968);
        assertThat(count("zk_assessment")).isEqualTo(7);
        assertThat(count("zk_planning")).isEqualTo(9);
        assertThat(count("zk_school")).isEqualTo(56);
        assertThat(count("zk_classification")).isEqualTo(13);
        assertThat(count("zk_beneficiary_category")).isEqualTo(7);
    }

    @Test
    @Order(2)
    void paidTotalsPerYearMatchBenchmark() {
        Map<Integer, String> expected = Map.of(
                2020, "372940.00", 2021, "479650.00", 2022, "1302046.93", 2023, "1244370.00",
                2024, "1821950.00", 2025, "3313624.00", 2026, "4105212.00");
        Map<Integer, Integer> expectedRows = Map.of(
                2020, 11, 2021, 52, 2022, 91, 2023, 124, 2024, 130, 2025, 264, 2026, 293);

        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT zakat_year, COUNT(*) AS n, SUM(paid_amount) AS paid FROM zk_allocation GROUP BY zakat_year");
        assertThat(rows).hasSize(7);
        for (Map<String, Object> r : rows) {
            int year = ((Number) r.get("zakat_year")).intValue();
            assertThat(((Number) r.get("n")).intValue()).as("rows " + year).isEqualTo(expectedRows.get(year));
            assertThat((BigDecimal) r.get("paid")).as("paid " + year).isEqualByComparingTo(expected.get(year));
        }
        assertThat(jdbc.queryForObject("SELECT SUM(paid_amount) FROM zk_allocation", BigDecimal.class))
                .isEqualByComparingTo("12639792.93");
    }

    @Test
    @Order(3)
    void paymentMethodTotalsMatchBenchmark() {
        Map<String, String> expected = Map.of(
                "CASH", "3858611.00", "ONLINE", "1088548.00", "BANK", "642800.00", "NOT_SET", "7049833.93");
        jdbc.queryForList("SELECT COALESCE(payment_method,'NOT_SET') AS m, SUM(paid_amount) AS paid "
                        + "FROM zk_allocation GROUP BY 1")
                .forEach(r -> assertThat((BigDecimal) r.get("paid")).as((String) r.get("m"))
                        .isEqualByComparingTo(expected.get((String) r.get("m"))));
    }

    @Test
    @Order(4)
    void assessmentBalancesArePreserved() {
        assertThat(jdbc.queryForObject("SELECT SUM(zakat_amount) FROM zk_assessment", BigDecimal.class))
                .isEqualByComparingTo("12205127.32");
        assertThat(balance(2026)).isEqualByComparingTo("-665.62"); // over-allocation kept, not clamped
        assertThat(balance(2022)).isEqualByComparingTo("0.01");    // rounding variance kept
        assertThat(jdbc.queryForObject(
                "SELECT total_allocated_amount FROM zk_assessment WHERE zakat_year = 2026", BigDecimal.class))
                .isEqualByComparingTo("3945212.00");
    }

    @Test
    @Order(5)
    void medicalAllocationsLinkToMedicalEmergencyButKeepSourceText() {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM zk_allocation a JOIN zk_classification c "
                + "USING (classification_id) WHERE a.classification = 'Medical' AND c.name = 'Medical Emergency'",
                Integer.class);
        assertThat(n).isEqualTo(18);
    }

    // ------------------------------------------------------------------ API behaviour

    @Test
    @Order(10)
    void planning2026MatchesApexReport() throws Exception {
        JsonNode plans = getJson("/api/v1/planning?years=2026", login("admin", "admin123"));
        long allocations = 0;
        BigDecimal paid = BigDecimal.ZERO;
        for (JsonNode p : plans) {
            allocations += p.get("allocationCount").asLong();
            paid = paid.add(p.get("paidAmount").decimalValue());
            if ("School fees".equals(p.get("classification").asText())) {
                assertThat(p.get("allocationCount").asLong()).isEqualTo(55);
                assertThat(p.get("cyMinus1").decimalValue()).isEqualByComparingTo("982624");
            }
        }
        assertThat(allocations).isEqualTo(239);
        assertThat(paid).isEqualByComparingTo("4105212");
    }

    @Test
    @Order(11)
    void masterCountsArePopulated() throws Exception {
        String token = login("admin", "admin123");
        JsonNode cats = getJson("/api/v1/beneficiary-categories?size=50", token).get("content");
        long total = 0;
        for (JsonNode c : cats) total += c.get("beneficiaryCount").asLong();
        assertThat(total).isEqualTo(968);

        JsonNode schools = getJson("/api/v1/schools?size=100", token).get("content");
        boolean holyFatima = false;
        for (JsonNode s : schools) {
            if ("Holy Fatima Convent School".equals(s.get("name").asText())) {
                assertThat(s.get("beneficiaryCount").asLong()).isEqualTo(29);
                holyFatima = true;
            }
        }
        assertThat(holyFatima).isTrue();
    }

    @Test
    @Order(12)
    void allocationLifecycleRecalculatesAssessment() throws Exception {
        String token = login("accountant", "account123");

        JsonNode ben = send(HttpMethod.POST, "/api/v1/beneficiaries", token,
                "{\"name\":\"IT Beneficiary\",\"beneficiaryCategory\":\"Poor Family\"}", HttpStatus.CREATED);
        long benId = ben.get("beneficiaryId").asLong();
        assertThat(ben.get("beneficiaryCode").asText()).isEqualTo("BEN-0969");

        // Not verified yet -> business rule 422
        send(HttpMethod.POST, "/api/v1/allocations", token, allocation(benId, 5000, null),
                HttpStatus.UNPROCESSABLE_ENTITY);

        send(HttpMethod.PATCH, "/api/v1/beneficiaries/" + benId + "/verification", token,
                "{\"verificationStatus\":\"VERIFIED\"}", HttpStatus.OK);

        JsonNode alloc = send(HttpMethod.POST, "/api/v1/allocations", token, allocation(benId, 5000, null),
                HttpStatus.CREATED);
        long allocId = alloc.get("allocationId").asLong();
        assertThat(alloc.get("fundType").asText()).isEqualTo("ZAKAT");
        assertThat(alloc.get("category").asText()).isEqualTo("Poor Family");
        assertThat(balance(2026)).isEqualByComparingTo("-5665.62");

        JsonNode paid = send(HttpMethod.PUT, "/api/v1/allocations/" + allocId, token,
                allocation(benId, 5000, 5000), HttpStatus.OK);
        assertThat(paid.get("status").asText()).isEqualTo("PAID");
        assertThat(paid.get("approvalStatus").asText()).isEqualTo("APPROVED");

        send(HttpMethod.DELETE, "/api/v1/allocations/" + allocId, token, null, HttpStatus.NO_CONTENT);
        assertThat(balance(2026)).isEqualByComparingTo("-665.62");

        send(HttpMethod.DELETE, "/api/v1/beneficiaries/" + benId, token, null, HttpStatus.NO_CONTENT);
    }

    @Test
    @Order(13)
    void rolesAreEnforced() throws Exception {
        String viewer = login("viewer", "viewer123");
        send(HttpMethod.POST, "/api/v1/beneficiaries", viewer, "{\"name\":\"x\"}", HttpStatus.FORBIDDEN);
        ResponseEntity<String> export = rest.exchange("/api/v1/export/allocations.xlsx", HttpMethod.GET,
                new HttpEntity<>(headers(viewer)), String.class);
        assertThat(export.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        String accountant = login("accountant", "account123");
        send(HttpMethod.POST, "/api/v1/classifications", accountant,
                "{\"name\":\"x\",\"fundType\":\"ZAKAT\"}", HttpStatus.FORBIDDEN);

        ResponseEntity<String> anon = rest.getForEntity("/api/v1/assessments", String.class);
        assertThat(anon.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ------------------------------------------------------------------ helpers

    private static String allocation(long benId, int planned, Integer paidAmount) {
        // classification 11 = "Poor Family" (ZAKAT) in V2 seed
        return "{\"zakatYear\":2026,\"classificationId\":11,\"beneficiaryId\":" + benId
                + ",\"plannedAmount\":" + planned
                + (paidAmount != null ? ",\"paidAmount\":" + paidAmount + ",\"paymentMethod\":\"CASH\"" : "")
                + "}";
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }

    private BigDecimal balance(int year) {
        return jdbc.queryForObject("SELECT remaining_balance FROM zk_assessment WHERE zakat_year = ?",
                BigDecimal.class, year);
    }

    private String login(String user, String password) throws Exception {
        ResponseEntity<String> res = rest.postForEntity("/api/v1/auth/login",
                new HttpEntity<>("{\"username\":\"" + user + "\",\"password\":\"" + password + "\"}", jsonHeaders(null)),
                String.class);
        assertThat(res.getStatusCode()).as("login " + user).isEqualTo(HttpStatus.OK);
        return json.readTree(res.getBody()).get("token").asText();
    }

    private JsonNode getJson(String path, String token) throws Exception {
        ResponseEntity<String> res = rest.exchange(path, HttpMethod.GET, new HttpEntity<>(headers(token)), String.class);
        assertThat(res.getStatusCode()).as(path).isEqualTo(HttpStatus.OK);
        return json.readTree(res.getBody());
    }

    private JsonNode send(HttpMethod method, String path, String token, String body, HttpStatus expected)
            throws Exception {
        ResponseEntity<String> res = rest.exchange(path, method, new HttpEntity<>(body, jsonHeaders(token)), String.class);
        assertThat(res.getStatusCode()).as(method + " " + path + " -> " + res.getBody()).isEqualTo(expected);
        return res.getBody() == null ? null : json.readTree(res.getBody());
    }

    private static HttpHeaders headers(String token) {
        HttpHeaders h = new HttpHeaders();
        if (token != null) h.setBearerAuth(token);
        return h;
    }

    private static HttpHeaders jsonHeaders(String token) {
        HttpHeaders h = headers(token);
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }
}
