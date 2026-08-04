package lcia.syntheticretrieval;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class RetrievalModel {

    private RetrievalModel() {
    }

    public record TenantId(String value) {
        public TenantId {
            requireOpaque(value, "tenantId");
        }
    }

    public record ActorId(String value) {
        public ActorId {
            requireOpaque(value, "actorId");
        }
    }

    public record SourceId(String value) {
        public SourceId {
            requireOpaque(value, "sourceId");
        }
    }

    public record ScopeId(String value) {
        public ScopeId {
            requireOpaque(value, "scopeId");
        }
    }

    public record CandidateId(String value) {
        public CandidateId {
            requireOpaque(value, "candidateId");
        }
    }

    public record SnapshotId(String value) {
        public SnapshotId {
            requireOpaque(value, "snapshotId");
        }
    }

    public record Scope(ScopeId id, List<SourceId> sources) {
        public Scope {
            id = Objects.requireNonNull(id, "id");
            sources = orderedSources(sources);
        }
    }

    public record Gateway(SourceId sourceId, boolean required) {
        public Gateway {
            sourceId = Objects.requireNonNull(sourceId, "sourceId");
        }
    }

    public record RetrievalSnapshot(SnapshotId id, TenantId tenantId, Scope scope, List<Gateway> gateways) {
        public RetrievalSnapshot {
            id = Objects.requireNonNull(id, "id");
            tenantId = Objects.requireNonNull(tenantId, "tenantId");
            scope = Objects.requireNonNull(scope, "scope");
            gateways = gateways.stream()
                    .map(gateway -> Objects.requireNonNull(gateway, "gateway"))
                    .sorted(Comparator.comparing(gateway -> gateway.sourceId().value()))
                    .toList();
            if (!scope.sources().equals(gateways.stream().map(Gateway::sourceId).distinct().sorted(
                    Comparator.comparing(SourceId::value)).toList())) {
                throw new IllegalArgumentException("snapshot gateways must match scope sources");
            }
        }
    }

    public enum Coverage {
        COMPLETE,
        PARTIAL
    }

    public enum Decision {
        AMBIGUOUS,
        INSUFFICIENT,
        STALE,
        NOT_LOCATED_IN_SCOPE
    }

    public enum Impediment {
        DENIED,
        UNAVAILABLE
    }

    public sealed interface RetrievalOutcome permits DeniedOutcome, EvaluatedOutcome {
    }

    public record DeniedOutcome() implements RetrievalOutcome {
    }

    public record EvaluatedOutcome(
            Coverage coverage,
            Decision decision,
            Optional<Impediment> impediment,
            List<CandidateId> candidates) implements RetrievalOutcome {
        public EvaluatedOutcome {
            coverage = Objects.requireNonNull(coverage, "coverage");
            decision = Objects.requireNonNull(decision, "decision");
            impediment = Objects.requireNonNull(impediment, "impediment");
            candidates = orderedCandidates(candidates);
            if (decision == Decision.NOT_LOCATED_IN_SCOPE
                    && (coverage != Coverage.COMPLETE || impediment.isPresent() || !candidates.isEmpty())) {
                throw new IllegalArgumentException("NOT_LOCATED_IN_SCOPE requires complete coverage and no candidates or impediment");
            }
        }
    }

    public record MinimizedTrace(
            SnapshotId snapshotId,
            ScopeId scopeId,
            Optional<Coverage> coverage,
            Optional<Decision> decision,
            Optional<Impediment> impediment,
            List<CandidateId> candidates) {
        public MinimizedTrace {
            snapshotId = Objects.requireNonNull(snapshotId, "snapshotId");
            scopeId = Objects.requireNonNull(scopeId, "scopeId");
            coverage = Objects.requireNonNull(coverage, "coverage");
            decision = Objects.requireNonNull(decision, "decision");
            impediment = Objects.requireNonNull(impediment, "impediment");
            candidates = orderedCandidates(candidates);
            boolean denied = coverage.isEmpty() && decision.isEmpty() && candidates.isEmpty()
                    && impediment.equals(Optional.of(Impediment.DENIED));
            boolean evaluated = coverage.isPresent() && decision.isPresent() && impediment.orElse(null) != Impediment.DENIED;
            if (!denied && !evaluated) {
                throw new IllegalArgumentException("trace dimensions are incompatible");
            }
            if (evaluated) {
                new EvaluatedOutcome(coverage.orElseThrow(), decision.orElseThrow(), impediment, candidates);
            }
        }

        static MinimizedTrace from(RetrievalSnapshot snapshot, RetrievalOutcome outcome) {
            if (outcome instanceof DeniedOutcome) {
                return new MinimizedTrace(snapshot.id(), snapshot.scope().id(), Optional.empty(), Optional.empty(),
                        Optional.of(Impediment.DENIED), List.of());
            }
            EvaluatedOutcome evaluated = (EvaluatedOutcome) outcome;
            return new MinimizedTrace(snapshot.id(), snapshot.scope().id(), Optional.of(evaluated.coverage()),
                    Optional.of(evaluated.decision()), evaluated.impediment(), evaluated.candidates());
        }
    }

    private static void requireOpaque(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must be a non-blank opaque identifier");
        }
    }

    private static List<SourceId> orderedSources(List<SourceId> sources) {
        return sources.stream()
                .map(source -> Objects.requireNonNull(source, "source"))
                .sorted(Comparator.comparing(SourceId::value))
                .distinct()
                .toList();
    }

    private static List<CandidateId> orderedCandidates(List<CandidateId> candidates) {
        return candidates.stream()
                .map(candidate -> Objects.requireNonNull(candidate, "candidate"))
                .sorted(Comparator.comparing(CandidateId::value))
                .toList();
    }
}
