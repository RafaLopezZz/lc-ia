package lcia.syntheticretrieval;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class InMemoryWorkRegistryTest {

    @Test
    void registersPendingWorkBeforeAnyGatewayPoll() {
        Instant expiresAt = Instant.parse("2026-08-19T12:05:00Z");
        InMemoryWorkRegistry registry = new InMemoryWorkRegistry(Clock.fixed(
                Instant.parse("2026-08-19T12:00:00Z"), ZoneOffset.UTC));
        InMemoryWorkRegistry.Registration request = new InMemoryWorkRegistry.Registration(
                new SyntheticTrustBoundary.TenantId("tenant-a"),
                new SyntheticTrustBoundary.GatewayId("gateway-a"),
                new SyntheticTrustBoundary.IdempotencyKey("key-a"),
                new SyntheticTrustBoundary.CorrelationId("correlation-a"),
                expiresAt);

        InMemoryWorkRegistry.Work work = registry.register(request);

        assertAll(
                () -> assertNotNull(work.operationId()),
                () -> assertEquals(InMemoryWorkRegistry.Status.PENDING, work.status()),
                () -> assertEquals(InMemoryWorkRegistry.DeliveryState.PENDING, work.deliveryState()),
                () -> assertEquals(request.tenant(), work.tenant()),
                () -> assertEquals(request.targetGateway(), work.targetGateway()),
                () -> assertEquals(request.idempotencyKey(), work.idempotencyKey()),
                () -> assertEquals(request.correlationId(), work.correlationId()),
                () -> assertEquals(expiresAt, work.expiresAt()));
    }

    @Test
    void deliversPendingWorkOnlyToItsTargetGateway() {
        SyntheticTrustBoundary.TenantId tenantA = new SyntheticTrustBoundary.TenantId("tenant-a");
        SyntheticTrustBoundary.TenantId tenantB = new SyntheticTrustBoundary.TenantId("tenant-b");
        SyntheticTrustBoundary.GatewayId gatewayA = new SyntheticTrustBoundary.GatewayId("gateway-a");
        SyntheticTrustBoundary.GatewayId gatewayB = new SyntheticTrustBoundary.GatewayId("gateway-b");
        InMemoryWorkRegistry registry = new InMemoryWorkRegistry(Clock.fixed(
                Instant.parse("2026-08-19T12:00:00Z"), ZoneOffset.UTC));

        InMemoryWorkRegistry.Work work = registry.register(new InMemoryWorkRegistry.Registration(
                tenantA, gatewayA, new SyntheticTrustBoundary.IdempotencyKey("key-a"),
                new SyntheticTrustBoundary.CorrelationId("correlation-a"),
                Instant.parse("2026-08-19T12:05:00Z")));

        assertAll(
                () -> assertEquals(List.of(work), registry.pendingFor(tenantA, gatewayA)),
                () -> assertEquals(List.of(), registry.pendingFor(tenantA, gatewayB)),
                 () -> assertEquals(List.of(), registry.pendingFor(tenantB, gatewayA)));
    }

    @Test
    void reusesOneLogicalOperationForTenantAndIdempotencyKey() {
        SyntheticTrustBoundary.TenantId tenantA = new SyntheticTrustBoundary.TenantId("tenant-a");
        SyntheticTrustBoundary.TenantId tenantB = new SyntheticTrustBoundary.TenantId("tenant-b");
        SyntheticTrustBoundary.GatewayId gatewayA = new SyntheticTrustBoundary.GatewayId("gateway-a");
        SyntheticTrustBoundary.IdempotencyKey idempotencyKey = new SyntheticTrustBoundary.IdempotencyKey("idempotency-1");
        InMemoryWorkRegistry registry = new InMemoryWorkRegistry(Clock.fixed(
                Instant.parse("2026-08-19T12:00:00Z"), ZoneOffset.UTC));

        InMemoryWorkRegistry.Work first = registry.register(new InMemoryWorkRegistry.Registration(
                tenantA, gatewayA, idempotencyKey, new SyntheticTrustBoundary.CorrelationId("correlation-a"),
                Instant.parse("2026-08-19T12:05:00Z")));
        InMemoryWorkRegistry.Work repeated = registry.register(new InMemoryWorkRegistry.Registration(
                tenantA, gatewayA, idempotencyKey, new SyntheticTrustBoundary.CorrelationId("correlation-a"),
                Instant.parse("2026-08-19T12:05:00Z")));
        InMemoryWorkRegistry.Work otherTenant = registry.register(new InMemoryWorkRegistry.Registration(
                tenantB, gatewayA, idempotencyKey, new SyntheticTrustBoundary.CorrelationId("correlation-b"),
                Instant.parse("2026-08-19T12:05:00Z")));

        assertAll(
                () -> assertEquals(first.operationId(), repeated.operationId()),
                () -> assertNotEquals(first.operationId(), otherTenant.operationId()),
                 () -> assertEquals(List.of(first), registry.pendingFor(tenantA, idempotencyKey)),
                 () -> assertEquals(List.of(otherTenant), registry.pendingFor(tenantB, idempotencyKey)));
    }

    @Test
    void neverDeliversExpiredWorkWithControlledClock() {
        SyntheticTrustBoundary.TenantId tenant = new SyntheticTrustBoundary.TenantId("tenant-a");
        SyntheticTrustBoundary.GatewayId gateway = new SyntheticTrustBoundary.GatewayId("gateway-a");
        MutableClock clock = new MutableClock(Instant.parse("2026-08-19T12:00:00Z"));
        InMemoryWorkRegistry registry = new InMemoryWorkRegistry(clock);
        InMemoryWorkRegistry.Work work = registry.register(new InMemoryWorkRegistry.Registration(
                tenant, gateway, new SyntheticTrustBoundary.IdempotencyKey("key-a"),
                new SyntheticTrustBoundary.CorrelationId("correlation-a"),
                Instant.parse("2026-08-19T12:05:00Z")));

        assertEquals(List.of(work), registry.pendingFor(tenant, gateway));

        clock.setInstant(Instant.parse("2026-08-19T12:05:01Z"));

        assertEquals(List.of(), registry.pendingFor(tenant, gateway));
    }

    @Test
    void excludesWorkAtExactExpiryBoundary() {
        Instant now = Instant.parse("2026-08-19T12:05:00Z");
        SyntheticTrustBoundary.TenantId tenant = new SyntheticTrustBoundary.TenantId("tenant-a");
        SyntheticTrustBoundary.GatewayId gateway = new SyntheticTrustBoundary.GatewayId("gateway-a");
        InMemoryWorkRegistry registry = new InMemoryWorkRegistry(Clock.fixed(now, ZoneOffset.UTC));
        registry.register(new InMemoryWorkRegistry.Registration(
                tenant, gateway, new SyntheticTrustBoundary.IdempotencyKey("key-a"),
                new SyntheticTrustBoundary.CorrelationId("correlation-a"), now));

        assertEquals(List.of(), registry.pendingFor(tenant, gateway));
    }

    @Test
    void returnsStatusByCorrelationIdOnlyWithinTheOwningTenant() {
        SyntheticTrustBoundary.TenantId tenantA = new SyntheticTrustBoundary.TenantId("tenant-a");
        SyntheticTrustBoundary.TenantId tenantB = new SyntheticTrustBoundary.TenantId("tenant-b");
        SyntheticTrustBoundary.CorrelationId correlationA = new SyntheticTrustBoundary.CorrelationId("correlation-a");
        Instant expiresAt = Instant.parse("2026-08-19T12:05:00Z");
        MutableClock clock = new MutableClock(Instant.parse("2026-08-19T12:00:00Z"));
        InMemoryWorkRegistry registry = new InMemoryWorkRegistry(clock);
        registry.register(new InMemoryWorkRegistry.Registration(
                tenantA, new SyntheticTrustBoundary.GatewayId("gateway-a"),
                new SyntheticTrustBoundary.IdempotencyKey("key-a"), correlationA, expiresAt));

        assertAll(
                () -> assertEquals(Optional.of(InMemoryWorkRegistry.Status.PENDING), registry.statusFor(tenantA, correlationA)),
                () -> assertEquals(Optional.empty(), registry.statusFor(tenantB, correlationA)));

        clock.setInstant(expiresAt);

        assertEquals(Optional.of(InMemoryWorkRegistry.Status.EXPIRED), registry.statusFor(tenantA, correlationA));
    }

    @Test
    void deliversEligibleWorkInDeterministicOrderWithoutCrossTenantLeakage() {
        SyntheticTrustBoundary.TenantId tenantA = new SyntheticTrustBoundary.TenantId("tenant-a");
        SyntheticTrustBoundary.TenantId tenantB = new SyntheticTrustBoundary.TenantId("tenant-b");
        SyntheticTrustBoundary.GatewayId gateway = new SyntheticTrustBoundary.GatewayId("gateway-a");
        InMemoryWorkRegistry registry = new InMemoryWorkRegistry(Clock.fixed(
                Instant.parse("2026-08-19T12:00:00Z"), ZoneOffset.UTC));
        Instant expiresAt = Instant.parse("2026-08-19T12:05:00Z");

        InMemoryWorkRegistry.Work first = registry.register(new InMemoryWorkRegistry.Registration(
                tenantA, gateway, new SyntheticTrustBoundary.IdempotencyKey("key-a-1"),
                new SyntheticTrustBoundary.CorrelationId("correlation-a-1"), expiresAt));
        registry.register(new InMemoryWorkRegistry.Registration(
                tenantB, gateway, new SyntheticTrustBoundary.IdempotencyKey("key-b-1"),
                new SyntheticTrustBoundary.CorrelationId("correlation-b-1"), expiresAt));
        InMemoryWorkRegistry.Work second = registry.register(new InMemoryWorkRegistry.Registration(
                tenantA, gateway, new SyntheticTrustBoundary.IdempotencyKey("key-a-2"),
                new SyntheticTrustBoundary.CorrelationId("correlation-a-2"), expiresAt));
        InMemoryWorkRegistry.Work third = registry.register(new InMemoryWorkRegistry.Registration(
                tenantA, gateway, new SyntheticTrustBoundary.IdempotencyKey("key-a-3"),
                new SyntheticTrustBoundary.CorrelationId("correlation-a-3"), expiresAt));

        assertEquals(List.of(first, second, third), registry.pendingFor(tenantA, gateway));
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void setInstant(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
