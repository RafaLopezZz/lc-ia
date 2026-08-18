package com.leovinci.leos.adapters.in.https;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import lcia.syntheticretrieval.SyntheticTrustBoundary;

/**
 * Synthetic HTTP adapter: explicit non-production trust and protocol boundary.
 */
public final class SyntheticRemoteGatewayHttpBoundary implements AutoCloseable {
    private static final Set<String> FIELDS = Set.of(
            "tenant",
            "gateway",
            "credential",
            "nonce",
            "issued",
            "expires",
            "correlation",
            "idempotency",
            "remote");
    private final HttpServer server;
    private final SyntheticTrustBoundary boundary;
    private final BooleanSupplier locallyAuthorized;

    public SyntheticRemoteGatewayHttpBoundary(SyntheticTrustBoundary boundary, BooleanSupplier locallyAuthorized)
            throws IOException {
        this.boundary = boundary;
        this.locallyAuthorized = locallyAuthorized;
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/synthetic/poll", this::poll);
        server.start();
    }

    public URI uri() {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/synthetic/poll");
    }

    private void poll(HttpExchange exchange) throws IOException {
        try {
            Map<String, String> values = values(exchange.getRequestURI());
            if (!values.keySet().equals(FIELDS)) {
                send(exchange, 400, "category=PROHIBITED");
                return;
            }
            SyntheticTrustBoundary.Decision decision = boundary.authorize(new SyntheticTrustBoundary.Poll(
                    new SyntheticTrustBoundary.TenantId(values.get("tenant")),
                    new SyntheticTrustBoundary.GatewayId(values.get("gateway")),
                    new SyntheticTrustBoundary.CredentialId(values.get("credential")),
                    new SyntheticTrustBoundary.Nonce(values.get("nonce")),
                    Instant.parse(values.get("issued")), Instant.parse(values.get("expires")),
                    new SyntheticTrustBoundary.CorrelationId(values.get("correlation")),
                    new SyntheticTrustBoundary.IdempotencyKey(values.get("idempotency")),
                    remoteAuthorization(values.get("remote"))));
            if (decision instanceof SyntheticTrustBoundary.Rejected rejected) {
                send(exchange, 403, "category=" + rejected.reason());
                return;
            }
            if (!locallyAuthorized.getAsBoolean()) {
                send(exchange, 403, "category=LOCAL_DENIED");
                return;
            }
            SyntheticTrustBoundary.Work work = ((SyntheticTrustBoundary.Accepted) decision).work();
            send(exchange, 200, "operation=" + work.operationId().value() + "&attempt=" + work.attemptId().value()
                    + "&candidate=" + work.candidates().getFirst().value() + "&trace=AUTHORIZED");
        } catch (RuntimeException invalid) {
            send(exchange, 400, "category=PROHIBITED");
        }
    }

    private static Map<String, String> values(URI uri) {
        Map<String, String> values = new HashMap<>();
        for (String pair : uri.getRawQuery().split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length != 2 || values.put(URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                    URLDecoder.decode(parts[1], StandardCharsets.UTF_8)) != null) {
                throw new IllegalArgumentException("invalid synthetic request");
            }
        }
        return values;
    }

    private static boolean remoteAuthorization(String value) {
        if (!"true".equals(value) && !"false".equals(value)) {
            throw new IllegalArgumentException("remote must be boolean");
        }
        return Boolean.parseBoolean(value);
    }

    private static void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
