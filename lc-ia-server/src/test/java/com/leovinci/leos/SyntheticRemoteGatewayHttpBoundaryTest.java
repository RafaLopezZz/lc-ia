package com.leovinci.leos;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.leovinci.leos.adapters.in.https.SyntheticRemoteGatewayHttpBoundary;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import lcia.syntheticretrieval.SyntheticTrustBoundary;
import org.junit.jupiter.api.Test;

class SyntheticRemoteGatewayHttpBoundaryTest {
    private static final Instant NOW = Instant.parse("2026-08-17T12:00:00Z");

    @Test
    void httpEdgeAcceptsOnlyBoundRemoteAndLocalAuthorizationWithMinimizedOutput() throws Exception {
        try (SyntheticRemoteGatewayHttpBoundary edge = edge(true)) {
            HttpResponse<String> response = get(edge.uri(), "nonce-a", "key-a", "true", "");

            assertEquals(200, response.statusCode());
            assertEquals("trace=AUTHORIZED", response.body());
        }
    }

    @Test
    void localDenialPrevailsAndProhibitedFieldsNeverCrossTheHttpBoundary() throws Exception {
        try (SyntheticRemoteGatewayHttpBoundary denied = edge(false);
                SyntheticRemoteGatewayHttpBoundary allowed = edge(true)) {
            assertEquals(403, get(denied.uri(), "nonce-a", "key-a", "true", "").statusCode());
            assertEquals(400, get(allowed.uri(), "nonce-a", "key-a", "true", "&path=forbidden").statusCode());
        }
    }

    @Test
    void httpEdgeRejectsRemoteDenialAndTokenOrContentInEveryAcceptedBoundaryField() throws Exception {
        try (SyntheticRemoteGatewayHttpBoundary edge = edge(true)) {
            assertEquals(403, request(edge.uri(), fields()).statusCode());
            for (String field : fields().keySet()) {
                for (String prohibited : new String[] { "human-token-value", "document-content-value" }) {
                    Map<String, String> invalid = fields();
                    invalid.put(field, prohibited);
                    assertEquals(400, request(edge.uri(), invalid).statusCode(), field + " must reject " + prohibited);
                }
            }
        }
    }

    private static SyntheticRemoteGatewayHttpBoundary edge(boolean localAuthorized) throws Exception {
        SyntheticTrustBoundary boundary = new SyntheticTrustBoundary(Clock.fixed(NOW, ZoneOffset.UTC));
        boundary.bind(new SyntheticTrustBoundary.TenantId("tenant-a"),
                new SyntheticTrustBoundary.GatewayId("gateway-a"),
                new SyntheticTrustBoundary.CredentialId("credential-a"));
        return new SyntheticRemoteGatewayHttpBoundary(boundary, () -> localAuthorized);
    }

    private static HttpResponse<String> get(URI uri, String nonce, String key, String remote, String extra)
            throws Exception {
        String query = "tenant=tenant-a&gateway=gateway-a&credential=credential-a&nonce=" + nonce
                + "&issued=2026-08-17T12:00:00Z"
                + "&expires=2026-08-17T12:00:10Z&correlation=correlation-a&idempotency=" + key + "&remote=" + remote
                + extra;
        return HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(uri + "?" + query)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static Map<String, String> fields() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("tenant", "tenant-a");
        fields.put("gateway", "gateway-a");
        fields.put("credential", "credential-a");
        fields.put("nonce", "nonce-a");
        fields.put("issued", "2026-08-17T12:00:00Z");
        fields.put("expires", "2026-08-17T12:00:10Z");
        fields.put("correlation", "correlation-a");
        fields.put("idempotency", "key-a");
        fields.put("remote", "false");
        return fields;
    }

    private static HttpResponse<String> request(URI uri, Map<String, String> fields) throws Exception {
        String query = String.join("&",
                fields.entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue()).toList());
        return HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(uri + "?" + query)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
