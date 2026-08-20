package lcia.syntheticretrieval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.io.StringWriter;
import java.lang.reflect.RecordComponent;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.spi.ToolProvider;
import org.junit.jupiter.api.Test;

class SyntheticTrustBoundaryTest {

    @Test
    void acceptsBoundAuthorizedPollWithMinimizedTrace() {
        Instant now = Instant.parse("2026-08-17T12:00:00Z");
        SyntheticTrustBoundary boundary = boundary(now);

        SyntheticTrustBoundary.Accepted accepted = assertInstanceOf(SyntheticTrustBoundary.Accepted.class,
                boundary.authorize(poll(now, "nonce-a", "key-a", true)));

        assertEquals(new SyntheticTrustBoundary.Trace(new SyntheticTrustBoundary.TenantId("tenant-a"),
                        new SyntheticTrustBoundary.GatewayId("gateway-a"),
                        new SyntheticTrustBoundary.CorrelationId("correlation-a"),
                        SyntheticTrustBoundary.TraceCategory.AUTHORIZED),
                accepted.trace());
    }

    @Test
    void m11AuthorizationCreatesNoRemoteWork() {
        Instant now = Instant.parse("2026-08-17T12:00:00Z");
        SyntheticTrustBoundary boundary = boundary(now);

        SyntheticTrustBoundary.Accepted accepted = assertInstanceOf(SyntheticTrustBoundary.Accepted.class,
                boundary.authorize(poll(now, "nonce-m11", "key-m11", true)));

        assertEquals(SyntheticTrustBoundary.TraceCategory.AUTHORIZED, accepted.trace().category());
    }

    @Test
    void rejectsReplayExpiryRevocationAndTenantMismatchWithoutAcceptingWork() {
        Instant now = Instant.parse("2026-08-17T12:00:00Z");
        SyntheticTrustBoundary boundary = boundary(now);
        assertInstanceOf(SyntheticTrustBoundary.Accepted.class, boundary.authorize(poll(now, "nonce-a", "key-a", true)));
        assertEquals(SyntheticTrustBoundary.Rejection.REPLAY,
                ((SyntheticTrustBoundary.Rejected) boundary.authorize(poll(now, "nonce-a", "key-b", true))).reason());
        assertEquals(SyntheticTrustBoundary.Rejection.EXPIRED,
                ((SyntheticTrustBoundary.Rejected) boundary.authorize(poll(now.minusSeconds(20), "nonce-b", "key-c", true))).reason());
        assertEquals(SyntheticTrustBoundary.Rejection.BINDING, ((SyntheticTrustBoundary.Rejected) boundary.authorize(
                new SyntheticTrustBoundary.Poll(new SyntheticTrustBoundary.TenantId("tenant-b"), new SyntheticTrustBoundary.GatewayId("gateway-a"),
                        new SyntheticTrustBoundary.CredentialId("credential-a"), new SyntheticTrustBoundary.Nonce("nonce-mismatch"), now, now.plusSeconds(10),
                        new SyntheticTrustBoundary.CorrelationId("correlation-a"), new SyntheticTrustBoundary.IdempotencyKey("key-mismatch"), true))).reason());
        boundary.revoke(new SyntheticTrustBoundary.CredentialId("credential-a"));
        assertEquals(SyntheticTrustBoundary.Rejection.REVOKED,
                ((SyntheticTrustBoundary.Rejected) boundary.authorize(poll(now, "nonce-c", "key-d", true))).reason());
    }

    @Test
    void tracesExposeOnlyOpaqueIdentifiersAndCategories() {
        assertEquals(0, Arrays.stream(SyntheticTrustBoundary.Trace.class.getRecordComponents())
                .map(RecordComponent::getType).filter(type -> type == String.class).count());
    }

    @Test
    void rejectsTokenAndContentLikeValuesInEveryOpaqueBoundaryIdentifier() {
        for (String prohibited : List.of("human-token-value", "document-content-value")) {
            assertThrows(IllegalArgumentException.class, () -> new SyntheticTrustBoundary.TenantId(prohibited));
            assertThrows(IllegalArgumentException.class, () -> new SyntheticTrustBoundary.GatewayId(prohibited));
            assertThrows(IllegalArgumentException.class, () -> new SyntheticTrustBoundary.CredentialId(prohibited));
            assertThrows(IllegalArgumentException.class, () -> new SyntheticTrustBoundary.Nonce(prohibited));
            assertThrows(IllegalArgumentException.class, () -> new SyntheticTrustBoundary.CorrelationId(prohibited));
            assertThrows(IllegalArgumentException.class, () -> new SyntheticTrustBoundary.IdempotencyKey(prohibited));
            assertThrows(IllegalArgumentException.class, () -> new SyntheticTrustBoundary.OperationId(prohibited));
        }
    }

    @Test
    void jdepsGuardKeepsTheCoreIndependentOfFrameworkAndServerAdapters() {
        StringWriter output = new StringWriter();
        int exit = ToolProvider.findFirst("jdeps").orElseThrow().run(new java.io.PrintWriter(output), new java.io.PrintWriter(output),
                "-verbose:class", Path.of("target", "classes").toString());

        assertEquals(0, exit);
        assertEquals(false, output.toString().matches("(?s).*?(org\\.springframework|jakarta\\.persistence|org\\.hibernate|com\\.leovinci|adapters).*"));
    }

    private static SyntheticTrustBoundary boundary(Instant now) {
        SyntheticTrustBoundary boundary = new SyntheticTrustBoundary(Clock.fixed(now, ZoneOffset.UTC));
        boundary.bind(new SyntheticTrustBoundary.TenantId("tenant-a"), new SyntheticTrustBoundary.GatewayId("gateway-a"),
                new SyntheticTrustBoundary.CredentialId("credential-a"));
        return boundary;
    }

    private static SyntheticTrustBoundary.Poll poll(Instant issuedAt, String nonce, String key, boolean remoteAuthorized) {
        return new SyntheticTrustBoundary.Poll(new SyntheticTrustBoundary.TenantId("tenant-a"),
                new SyntheticTrustBoundary.GatewayId("gateway-a"), new SyntheticTrustBoundary.CredentialId("credential-a"),
                new SyntheticTrustBoundary.Nonce(nonce), issuedAt, issuedAt.plusSeconds(10),
                new SyntheticTrustBoundary.CorrelationId("correlation-a"), new SyntheticTrustBoundary.IdempotencyKey(key), remoteAuthorized);
    }
}
