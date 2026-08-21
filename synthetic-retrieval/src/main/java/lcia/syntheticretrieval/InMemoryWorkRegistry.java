package lcia.syntheticretrieval;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

final class InMemoryWorkRegistry {
    private final Clock clock;
    private int nextOperation = 1;
    private final List<Work> works = new ArrayList<>();

    InMemoryWorkRegistry(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    synchronized Work register(Registration request) {
        Objects.requireNonNull(request, "request");
        for (Work work : works) {
            if (work.tenant().equals(request.tenant()) && work.idempotencyKey().equals(request.idempotencyKey())) {
                return work;
            }
        }
        Work work = new Work(request.tenant(), request.targetGateway(),
                new SyntheticTrustBoundary.OperationId("operation-" + nextOperation++),
                request.idempotencyKey(), request.correlationId(), request.expiresAt(),
                DeliveryState.PENDING);
        works.add(work);
        return work;
    }

    List<Work> pendingFor(SyntheticTrustBoundary.TenantId tenant, SyntheticTrustBoundary.GatewayId gateway) {
        Instant now = clock.instant();
        return works.stream()
                .filter(work -> work.tenant().equals(tenant)
                        && work.targetGateway().equals(gateway)
                        && work.deliveryState() == DeliveryState.PENDING
                        && effectiveStatusFor(work, now) == EffectiveStatus.PENDING)
                .toList();
    }

    Optional<Work> findByIdempotency(SyntheticTrustBoundary.TenantId tenant,
            SyntheticTrustBoundary.IdempotencyKey idempotencyKey) {
        return works.stream()
                .filter(work -> work.tenant().equals(tenant) && work.idempotencyKey().equals(idempotencyKey))
                .findFirst();
    }

    Optional<EffectiveStatus> effectiveStatusFor(SyntheticTrustBoundary.TenantId tenant,
            SyntheticTrustBoundary.CorrelationId correlationId) {
        Optional<Work> work = works.stream()
                .filter(candidate -> candidate.tenant().equals(tenant)
                        && candidate.correlationId().equals(correlationId))
                .findFirst();
        Instant now = clock.instant();
        return work.map(candidate -> effectiveStatusFor(candidate, now));
    }

    EffectiveStatus effectiveStatusFor(Work work) {
        return effectiveStatusFor(work, clock.instant());
    }

    private static EffectiveStatus effectiveStatusFor(Work work, Instant now) {
        return work.deliveryState() == DeliveryState.PENDING && work.expiresAt().isAfter(now)
                ? EffectiveStatus.PENDING
                : EffectiveStatus.EXPIRED;
    }

    record Registration(SyntheticTrustBoundary.TenantId tenant, SyntheticTrustBoundary.GatewayId targetGateway,
            SyntheticTrustBoundary.IdempotencyKey idempotencyKey,
            SyntheticTrustBoundary.CorrelationId correlationId, Instant expiresAt) {
        Registration {
            Objects.requireNonNull(tenant, "tenant");
            Objects.requireNonNull(targetGateway, "targetGateway");
            Objects.requireNonNull(idempotencyKey, "idempotencyKey");
            Objects.requireNonNull(correlationId, "correlationId");
            Objects.requireNonNull(expiresAt, "expiresAt");
        }
    }

    record Work(SyntheticTrustBoundary.TenantId tenant, SyntheticTrustBoundary.GatewayId targetGateway,
            SyntheticTrustBoundary.OperationId operationId, SyntheticTrustBoundary.IdempotencyKey idempotencyKey,
            SyntheticTrustBoundary.CorrelationId correlationId, Instant expiresAt, DeliveryState deliveryState) {
    }

    enum DeliveryState {
        PENDING
    }

    enum EffectiveStatus {
        PENDING, EXPIRED
    }
}
