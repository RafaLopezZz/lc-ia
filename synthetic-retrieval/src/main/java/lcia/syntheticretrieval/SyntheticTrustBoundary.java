package lcia.syntheticretrieval;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Synthetic-only, in-memory protocol boundary; it is not production security. */
public final class SyntheticTrustBoundary {
    private final Clock clock;
    private final Map<CredentialId, Binding> bindings = new HashMap<>();
    private final Map<Key, Work> operations = new HashMap<>();
    private final Set<Nonce> usedNonces = new HashSet<>();

    public SyntheticTrustBoundary(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public void bind(TenantId tenantId, GatewayId gatewayId, CredentialId credentialId) {
        bindings.put(credentialId, new Binding(tenantId, gatewayId, true));
    }

    public void revoke(CredentialId credentialId) {
        Binding binding = bindings.get(credentialId);
        if (binding != null) {
            bindings.put(credentialId, new Binding(binding.tenantId, binding.gatewayId, false));
        }
    }

    public Decision authorize(Poll poll) {
        Objects.requireNonNull(poll, "poll");
        Binding binding = bindings.get(poll.credentialId());
        if (binding == null || !binding.active) {
            return new Rejected(Rejection.REVOKED);
        }
        if (!binding.tenantId.equals(poll.tenantId()) || !binding.gatewayId.equals(poll.gatewayId())) {
            return new Rejected(Rejection.BINDING);
        }
        Instant now = clock.instant();
        if (poll.issuedAt().isAfter(now) || !poll.expiresAt().isAfter(now) || poll.expiresAt().isBefore(poll.issuedAt())) {
            return new Rejected(Rejection.EXPIRED);
        }
        if (!poll.remoteAuthorized()) {
            return new Rejected(Rejection.REMOTE_DENIED);
        }
        if (!usedNonces.add(poll.nonce())) {
            return new Rejected(Rejection.REPLAY);
        }
        Work work = operations.computeIfAbsent(new Key(poll.tenantId(), poll.idempotencyKey()), ignored ->
                new Work(new OperationId("operation-" + (operations.size() + 1)), poll.correlationId(),
                        new AttemptId("attempt-" + (operations.size() + 1)), poll.expiresAt(),
                        List.of(new CandidateId("candidate-synthetic"))));
        return new Accepted(work, new Trace(poll.tenantId(), poll.gatewayId(), work.operationId(), work.attemptId(),
                work.correlationId(), TraceCategory.AUTHORIZED));
    }

    public sealed interface Decision permits Accepted, Rejected { }
    public record Accepted(Work work, Trace trace) implements Decision { }
    public record Rejected(Rejection reason) implements Decision { }
    public enum Rejection { REVOKED, BINDING, EXPIRED, REPLAY, REMOTE_DENIED, LOCAL_DENIED, PROHIBITED }
    public enum TraceCategory { AUTHORIZED, REJECTED }
    public record TenantId(String value) { public TenantId { opaque(value, "tenantId"); } }
    public record GatewayId(String value) { public GatewayId { opaque(value, "gatewayId"); } }
    public record CredentialId(String value) { public CredentialId { opaque(value, "credentialId"); } }
    public record Nonce(String value) { public Nonce { opaque(value, "nonce"); } }
    public record CorrelationId(String value) { public CorrelationId { opaque(value, "correlationId"); } }
    public record IdempotencyKey(String value) { public IdempotencyKey { opaque(value, "idempotencyKey"); } }
    public record OperationId(String value) { public OperationId { opaque(value, "operationId"); } }
    public record AttemptId(String value) { public AttemptId { opaque(value, "attemptId"); } }
    public record CandidateId(String value) { public CandidateId { opaque(value, "candidateId"); } }
    public record Poll(TenantId tenantId, GatewayId gatewayId, CredentialId credentialId, Nonce nonce,
                       Instant issuedAt, Instant expiresAt, CorrelationId correlationId, IdempotencyKey idempotencyKey,
                       boolean remoteAuthorized) {
        public Poll {
            Objects.requireNonNull(tenantId, "tenantId"); Objects.requireNonNull(gatewayId, "gatewayId");
            Objects.requireNonNull(credentialId, "credentialId"); Objects.requireNonNull(nonce, "nonce");
            Objects.requireNonNull(issuedAt, "issuedAt"); Objects.requireNonNull(expiresAt, "expiresAt");
            Objects.requireNonNull(correlationId, "correlationId"); Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        }
    }
    public record Work(OperationId operationId, CorrelationId correlationId, AttemptId attemptId, Instant expiresAt,
                       List<CandidateId> candidates) {
        public Work { candidates = List.copyOf(candidates); }
    }
    public record Trace(TenantId tenantId, GatewayId gatewayId, OperationId operationId, AttemptId attemptId,
                        CorrelationId correlationId, TraceCategory category) { }
    private record Binding(TenantId tenantId, GatewayId gatewayId, boolean active) { }
    private record Key(TenantId tenantId, IdempotencyKey idempotencyKey) { }

    private static void opaque(String value, String name) {
        String normalized = value == null ? "" : value.toLowerCase();
        if (value == null || value.isBlank() || !value.matches("[a-z][a-z0-9-]*")
                || normalized.matches(".*(token|content|document|path|uri|file|password|secret|bytes).*$")) {
            throw new IllegalArgumentException(name + " must be an opaque synthetic identifier");
        }
    }
}
