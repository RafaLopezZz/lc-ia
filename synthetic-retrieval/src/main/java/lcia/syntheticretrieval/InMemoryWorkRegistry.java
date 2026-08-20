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

    Work register(Registration request) {
        Objects.requireNonNull(request, "request");
        for (Work work : works) {
            if (work.tenant().equals(request.tenant()) && work.idempotencyKey().equals(request.idempotencyKey())) {
                return work;
            }
        }
        Work work = new Work(request.tenant(), request.targetGateway(),
                new SyntheticTrustBoundary.OperationId("operation-" + nextOperation++),
                request.idempotencyKey(), request.correlationId(), request.expiresAt(),
                DeliveryState.PENDING, Status.PENDING);
        works.add(work);
        return work;
    }

    List<Work> pendingFor(SyntheticTrustBoundary.TenantId tenant, SyntheticTrustBoundary.GatewayId gateway) {
        return works.stream()
                .filter(work -> work.tenant().equals(tenant)
                        && work.targetGateway().equals(gateway)
                        && work.deliveryState() == DeliveryState.PENDING
                        && work.expiresAt().isAfter(clock.instant()))
                .toList();
    }

    List<Work> pendingFor(SyntheticTrustBoundary.TenantId tenant, SyntheticTrustBoundary.IdempotencyKey idempotencyKey) {
        return works.stream()
                .filter(work -> work.tenant().equals(tenant) && work.idempotencyKey().equals(idempotencyKey))
                .toList();
    }

    Optional<Status> statusFor(SyntheticTrustBoundary.TenantId tenant,
                               SyntheticTrustBoundary.CorrelationId correlationId) {
        return works.stream()
                .filter(work -> work.tenant().equals(tenant) && work.correlationId().equals(correlationId))
                .findFirst()
                .map(work -> work.expiresAt().isAfter(clock.instant()) ? Status.PENDING : Status.EXPIRED);
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
                SyntheticTrustBoundary.CorrelationId correlationId, Instant expiresAt, DeliveryState deliveryState,
                Status status) { }

    enum DeliveryState { PENDING }
    enum Status { PENDING, EXPIRED }
}
