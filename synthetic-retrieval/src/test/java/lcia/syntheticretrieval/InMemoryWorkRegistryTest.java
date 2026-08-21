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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class InMemoryWorkRegistryTest {

        @Test
        void concurrentRegistrationsWithSameTenantAndKeyCreateOneLogicalOperation() throws Exception {
                SyntheticTrustBoundary.TenantId tenant = new SyntheticTrustBoundary.TenantId("tenant-a");
                SyntheticTrustBoundary.GatewayId gateway = new SyntheticTrustBoundary.GatewayId("gateway-a");
                SyntheticTrustBoundary.IdempotencyKey key = new SyntheticTrustBoundary.IdempotencyKey("key-a");
                InMemoryWorkRegistry registry = new InMemoryWorkRegistry(Clock.fixed(
                                Instant.parse("2026-08-19T12:00:00Z"), ZoneOffset.UTC));
                InMemoryWorkRegistry.Registration request = new InMemoryWorkRegistry.Registration(
                                tenant, gateway, key, new SyntheticTrustBoundary.CorrelationId("correlation-a"),
                                Instant.parse("2026-08-19T12:05:00Z"));
                CountDownLatch ready = new CountDownLatch(4);
                CyclicBarrier start = new CyclicBarrier(5);
                ExecutorService executor = Executors.newFixedThreadPool(4);

                try {
                        List<Future<InMemoryWorkRegistry.Work>> registrations = List.of(
                                        executor.submit(() -> registerAfterSimultaneousStart(registry, request, ready,
                                                        start)),
                                        executor.submit(() -> registerAfterSimultaneousStart(registry, request, ready,
                                                        start)),
                                        executor.submit(() -> registerAfterSimultaneousStart(registry, request, ready,
                                                        start)),
                                        executor.submit(() -> registerAfterSimultaneousStart(registry, request, ready,
                                                        start)));

                        assertEquals(true, ready.await(5, TimeUnit.SECONDS));
                        start.await(5, TimeUnit.SECONDS);
                        List<InMemoryWorkRegistry.Work> works = List.of(
                                        registrations.get(0).get(5, TimeUnit.SECONDS),
                                        registrations.get(1).get(5, TimeUnit.SECONDS),
                                        registrations.get(2).get(5, TimeUnit.SECONDS),
                                        registrations.get(3).get(5, TimeUnit.SECONDS));

                        assertAll(
                                        () -> assertEquals(1,
                                                        works.stream().map(InMemoryWorkRegistry.Work::operationId)
                                                                        .distinct().count()),
                                        () -> assertEquals("operation-1", works.get(0).operationId().value()),
                                        () -> assertEquals(List.of(works.get(0)),
                                                        registry.pendingFor(tenant, gateway)));

                        InMemoryWorkRegistry.Work distinct = registry.register(new InMemoryWorkRegistry.Registration(
                                        tenant, gateway, new SyntheticTrustBoundary.IdempotencyKey("key-b"),
                                        new SyntheticTrustBoundary.CorrelationId("correlation-b"),
                                        Instant.parse("2026-08-19T12:05:00Z")));

                        assertEquals("operation-2", distinct.operationId().value());
                } finally {
                        executor.shutdownNow();
                }
        }

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
                SyntheticTrustBoundary.IdempotencyKey idempotencyKey = new SyntheticTrustBoundary.IdempotencyKey(
                                "idempotency-1");
                InMemoryWorkRegistry registry = new InMemoryWorkRegistry(Clock.fixed(
                                Instant.parse("2026-08-19T12:00:00Z"), ZoneOffset.UTC));

                InMemoryWorkRegistry.Work first = registry.register(new InMemoryWorkRegistry.Registration(
                                tenantA, gatewayA, idempotencyKey,
                                new SyntheticTrustBoundary.CorrelationId("correlation-a"),
                                Instant.parse("2026-08-19T12:05:00Z")));
                InMemoryWorkRegistry.Work repeated = registry.register(new InMemoryWorkRegistry.Registration(
                                tenantA, gatewayA, idempotencyKey,
                                new SyntheticTrustBoundary.CorrelationId("correlation-a"),
                                Instant.parse("2026-08-19T12:05:00Z")));
                InMemoryWorkRegistry.Work otherTenant = registry.register(new InMemoryWorkRegistry.Registration(
                                tenantB, gatewayA, idempotencyKey,
                                new SyntheticTrustBoundary.CorrelationId("correlation-b"),
                                Instant.parse("2026-08-19T12:05:00Z")));

                assertAll(
                                () -> assertEquals(first.operationId(), repeated.operationId()),
                                () -> assertNotEquals(first.operationId(), otherTenant.operationId()),
                                () -> assertEquals(Optional.of(first),
                                                registry.findByIdempotency(tenantA, idempotencyKey)),
                                () -> assertEquals(Optional.of(otherTenant),
                                                registry.findByIdempotency(tenantB, idempotencyKey)));
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
        void expiredWorkRemainsFindableByIdempotencyButIsNotDeliverable() {
                SyntheticTrustBoundary.TenantId tenantA = new SyntheticTrustBoundary.TenantId("tenant-a");
                SyntheticTrustBoundary.TenantId tenantB = new SyntheticTrustBoundary.TenantId("tenant-b");
                SyntheticTrustBoundary.GatewayId gatewayA = new SyntheticTrustBoundary.GatewayId("gateway-a");
                SyntheticTrustBoundary.IdempotencyKey keyA = new SyntheticTrustBoundary.IdempotencyKey("key-a");
                MutableClock clock = new MutableClock(Instant.parse("2026-08-19T12:05:01Z"));
                InMemoryWorkRegistry registry = new InMemoryWorkRegistry(clock);
                InMemoryWorkRegistry.Work work = registry.register(new InMemoryWorkRegistry.Registration(
                                tenantA, gatewayA, keyA, new SyntheticTrustBoundary.CorrelationId("correlation-a"),
                                Instant.parse("2026-08-19T12:05:00Z")));

                Optional<InMemoryWorkRegistry.Work> found = registry.findByIdempotency(tenantA, keyA);

                assertAll(
                                () -> assertEquals(Optional.of(work), found),
                                () -> assertEquals(List.of(), registry.pendingFor(tenantA, gatewayA)),
                                () -> assertEquals(Optional.empty(), registry.findByIdempotency(tenantB, keyA)));
        }

        @Test
        void storedDeliveryStateRemainsPendingWhileEffectiveStatusExpiresWithClock() {
                Instant expiresAt = Instant.parse("2026-08-19T12:05:00Z");
                MutableClock clock = new MutableClock(Instant.parse("2026-08-19T12:00:00Z"));
                InMemoryWorkRegistry registry = new InMemoryWorkRegistry(clock);
                InMemoryWorkRegistry.Work work = registry.register(new InMemoryWorkRegistry.Registration(
                                new SyntheticTrustBoundary.TenantId("tenant-a"),
                                new SyntheticTrustBoundary.GatewayId("gateway-a"),
                                new SyntheticTrustBoundary.IdempotencyKey("key-a"),
                                new SyntheticTrustBoundary.CorrelationId("correlation-a"), expiresAt));

                assertAll(
                                () -> assertEquals(InMemoryWorkRegistry.DeliveryState.PENDING, work.deliveryState()),
                                () -> assertEquals(InMemoryWorkRegistry.EffectiveStatus.PENDING,
                                                registry.effectiveStatusFor(work)));

                clock.setInstant(expiresAt);

                assertAll(
                                () -> assertEquals(InMemoryWorkRegistry.DeliveryState.PENDING, work.deliveryState()),
                                () -> assertEquals(InMemoryWorkRegistry.EffectiveStatus.EXPIRED,
                                                registry.effectiveStatusFor(work)));
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
        void returnsEffectiveStatusByCorrelationIdOnlyWithinTheOwningTenant() {
                SyntheticTrustBoundary.TenantId tenantA = new SyntheticTrustBoundary.TenantId("tenant-a");
                SyntheticTrustBoundary.TenantId tenantB = new SyntheticTrustBoundary.TenantId("tenant-b");
                SyntheticTrustBoundary.CorrelationId correlationA = new SyntheticTrustBoundary.CorrelationId(
                                "correlation-a");
                Instant expiresAt = Instant.parse("2026-08-19T12:05:00Z");
                MutableClock clock = new MutableClock(Instant.parse("2026-08-19T12:00:00Z"));
                InMemoryWorkRegistry registry = new InMemoryWorkRegistry(clock);
                registry.register(new InMemoryWorkRegistry.Registration(
                                tenantA, new SyntheticTrustBoundary.GatewayId("gateway-a"),
                                new SyntheticTrustBoundary.IdempotencyKey("key-a"), correlationA, expiresAt));

                assertAll(
                                () -> assertEquals(Optional.of(InMemoryWorkRegistry.EffectiveStatus.PENDING),
                                                registry.effectiveStatusFor(tenantA, correlationA)),
                                () -> assertEquals(Optional.empty(), registry.effectiveStatusFor(tenantB, correlationA)));

                clock.setInstant(expiresAt);

                assertEquals(Optional.of(InMemoryWorkRegistry.EffectiveStatus.EXPIRED),
                                registry.effectiveStatusFor(tenantA, correlationA));
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

        @Test
        void pendingQueryUsesOneClockSnapshot() {
                Instant now = Instant.parse("2026-08-19T12:00:00Z");
                SyntheticTrustBoundary.TenantId tenant = new SyntheticTrustBoundary.TenantId("tenant-a");
                SyntheticTrustBoundary.GatewayId gateway = new SyntheticTrustBoundary.GatewayId("gateway-a");
                CountingClock clock = new CountingClock(now);
                InMemoryWorkRegistry registry = new InMemoryWorkRegistry(clock);

                registry.register(new InMemoryWorkRegistry.Registration(tenant, gateway,
                                new SyntheticTrustBoundary.IdempotencyKey("key-a"),
                                new SyntheticTrustBoundary.CorrelationId("correlation-a"), now.plusSeconds(1)));
                registry.register(new InMemoryWorkRegistry.Registration(tenant, gateway,
                                new SyntheticTrustBoundary.IdempotencyKey("key-b"),
                                new SyntheticTrustBoundary.CorrelationId("correlation-b"), now.plusSeconds(1)));
                registry.register(new InMemoryWorkRegistry.Registration(tenant, gateway,
                                new SyntheticTrustBoundary.IdempotencyKey("key-c"),
                                new SyntheticTrustBoundary.CorrelationId("correlation-c"), now.plusSeconds(1)));

                clock.resetInstantCalls();
                List<InMemoryWorkRegistry.Work> pending = registry.pendingFor(tenant, gateway);

                assertAll(
                                () -> assertEquals(3, pending.size()),
                                () -> assertEquals(1, clock.instantCalls()));
        }

        @Test
        void pendingQueryUsesOneSnapshotAcrossExpiryBoundary() {
                Instant now = Instant.parse("2026-08-19T12:00:00Z");
                SyntheticTrustBoundary.TenantId tenant = new SyntheticTrustBoundary.TenantId("tenant-a");
                SyntheticTrustBoundary.GatewayId gateway = new SyntheticTrustBoundary.GatewayId("gateway-a");
                CountingClock clock = new CountingClock(now);
                InMemoryWorkRegistry registry = new InMemoryWorkRegistry(clock);

                registry.register(new InMemoryWorkRegistry.Registration(tenant, gateway,
                                new SyntheticTrustBoundary.IdempotencyKey("key-a"),
                                new SyntheticTrustBoundary.CorrelationId("correlation-a"), now));
                InMemoryWorkRegistry.Work included = registry.register(new InMemoryWorkRegistry.Registration(tenant,
                                gateway, new SyntheticTrustBoundary.IdempotencyKey("key-b"),
                                new SyntheticTrustBoundary.CorrelationId("correlation-b"), now.plusSeconds(1)));

                clock.resetInstantCalls();
                List<InMemoryWorkRegistry.Work> pending = registry.pendingFor(tenant, gateway);

                assertAll(
                                () -> assertEquals(List.of(included), pending),
                                () -> assertEquals(1, clock.instantCalls()));
        }

        private static InMemoryWorkRegistry.Work registerAfterSimultaneousStart(InMemoryWorkRegistry registry,
                        InMemoryWorkRegistry.Registration request,
                        CountDownLatch ready,
                        CyclicBarrier start) throws Exception {
                ready.countDown();
                start.await(5, TimeUnit.SECONDS);
                return registry.register(request);
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

        private static final class CountingClock extends Clock {
                private final Instant instant;
                private int instantCalls;

                private CountingClock(Instant instant) {
                        this.instant = instant;
                }

                void resetInstantCalls() {
                        instantCalls = 0;
                }

                int instantCalls() {
                        return instantCalls;
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
                        instantCalls++;
                        return instant;
                }
        }
}
