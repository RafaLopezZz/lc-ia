package lcia.syntheticretrieval;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

record Membership(RetrievalModel.TenantId tenantId, boolean active) {
    Membership {
        tenantId = Objects.requireNonNull(tenantId, "tenantId");
    }
}

record AuthorizationContext(
        RetrievalModel.ActorId actorId,
        RetrievalModel.TenantId activeTenantId,
        Membership membership,
        boolean operational) {
    AuthorizationContext {
        actorId = Objects.requireNonNull(actorId, "actorId");
        activeTenantId = Objects.requireNonNull(activeTenantId, "activeTenantId");
        membership = Objects.requireNonNull(membership, "membership");
    }

    boolean isValid() {
        return operational && membership.active() && activeTenantId.equals(membership.tenantId());
    }
}

record SourceGrant(
        RetrievalModel.ActorId actorId,
        RetrievalModel.TenantId tenantId,
        RetrievalModel.SourceId sourceId,
        boolean active) {
    SourceGrant {
        actorId = Objects.requireNonNull(actorId, "actorId");
        tenantId = Objects.requireNonNull(tenantId, "tenantId");
        sourceId = Objects.requireNonNull(sourceId, "sourceId");
    }
}

record RetrievalIntent(List<RetrievalModel.SourceId> requiredSources) {
    RetrievalIntent {
        requiredSources = requiredSources.stream().map(source -> Objects.requireNonNull(source, "source"))
                .sorted(Comparator.comparing(RetrievalModel.SourceId::value)).distinct().toList();
    }
}

record TenantCatalog(RetrievalModel.TenantId tenantId, List<RetrievalModel.Scope> scopes) {
    TenantCatalog {
        tenantId = Objects.requireNonNull(tenantId, "tenantId");
        scopes = scopes.stream()
                .map(scope -> Objects.requireNonNull(scope, "scope"))
                .sorted(Comparator.comparing(scope -> scope.id().value()))
                .toList();
    }
}

sealed interface ScopeResolution
        permits ScopeResolution.Selected, ScopeResolution.Clarification, ScopeResolution.Denied {
    record Selected(RetrievalModel.Scope scope) implements ScopeResolution {
        public Selected {
            scope = Objects.requireNonNull(scope, "scope");
        }
    }

    record Clarification(List<RetrievalModel.ScopeId> options) implements ScopeResolution {
        public Clarification {
            options = options.stream()
                    .map(option -> Objects.requireNonNull(option, "option"))
                    .sorted(Comparator.comparing(RetrievalModel.ScopeId::value))
                    .toList();
        }
    }

    record Denied() implements ScopeResolution {
    }
}

final class InMemorySimulation {
    private final Map<RetrievalModel.TenantId, TenantCatalog> catalogByTenant;

    InMemorySimulation(List<TenantCatalog> catalogs) {
        catalogByTenant = catalogs.stream().collect(Collectors.toUnmodifiableMap(
                TenantCatalog::tenantId,
                catalog -> catalog,
                (left, right) -> {
                    throw new IllegalArgumentException("duplicate tenant catalog");
                }));
    }

    ScopeResolution resolve(AuthorizationContext context, List<SourceGrant> grants) {
        return resolve(context, grants, new RetrievalIntent(List.of()));
    }

    ScopeResolution resolve(AuthorizationContext context, List<SourceGrant> grants, RetrievalIntent intent) {
        Objects.requireNonNull(intent, "intent");
        if (!context.isValid()) {
            return new ScopeResolution.Denied();
        }
        TenantCatalog activeCatalog = catalogByTenant.get(context.activeTenantId());
        if (activeCatalog == null) {
            return new ScopeResolution.Denied();
        }
        Set<RetrievalModel.SourceId> grantedSources = grants.stream()
                .filter(SourceGrant::active)
                .filter(grant -> grant.actorId().equals(context.actorId()))
                .filter(grant -> grant.tenantId().equals(context.activeTenantId()))
                .map(SourceGrant::sourceId)
                .collect(Collectors.toUnmodifiableSet());
        List<RetrievalModel.Scope> eligible = activeCatalog.scopes().stream()
                .filter(scope -> grantedSources.containsAll(scope.sources()))
                .filter(scope -> scope.sources().containsAll(intent.requiredSources()))
                .toList();
        if (eligible.isEmpty()) {
            return new ScopeResolution.Denied();
        }
        int smallest = eligible.stream().mapToInt(scope -> scope.sources().size()).min().orElseThrow();
        List<RetrievalModel.Scope> minimal = eligible.stream()
                .filter(scope -> scope.sources().size() == smallest)
                .toList();
        return minimal.size() == 1
                ? new ScopeResolution.Selected(minimal.getFirst())
                : new ScopeResolution.Clarification(minimal.stream().map(RetrievalModel.Scope::id).toList());
    }

    record Contribution(
            RetrievalModel.TenantId tenantId,
            RetrievalModel.SourceId sourceId,
            boolean terminal,
            boolean stale,
            List<RetrievalModel.CandidateId> candidates) {
        Contribution {
            tenantId = Objects.requireNonNull(tenantId, "tenantId");
            sourceId = Objects.requireNonNull(sourceId, "sourceId");
            candidates = List.copyOf(candidates);
        }
    }

    RetrievalModel.RetrievalOutcome consolidate(
            RetrievalModel.RetrievalSnapshot snapshot, List<Contribution> contributions) {
        Map<RetrievalModel.SourceId, Contribution> allowed = contributions.stream()
                .filter(contribution -> contribution.tenantId().equals(snapshot.tenantId()))
                .filter(contribution -> snapshot.scope().sources().contains(contribution.sourceId()))
                .collect(Collectors.toMap(Contribution::sourceId, contribution -> contribution,
                        (left, right) -> left, java.util.LinkedHashMap::new));
        boolean requiredUnavailable = snapshot.gateways().stream().filter(RetrievalModel.Gateway::required)
                .anyMatch(gateway -> !isTerminal(allowed, gateway.sourceId()));
        boolean optionalUnavailable = snapshot.gateways().stream().filter(gateway -> !gateway.required())
                .anyMatch(gateway -> !isTerminal(allowed, gateway.sourceId()));
        boolean unavailable = requiredUnavailable || optionalUnavailable;
        boolean complete = !unavailable;
        List<RetrievalModel.CandidateId> candidates = allowed.values().stream()
                .filter(Contribution::terminal).flatMap(contribution -> contribution.candidates().stream()).toList();
        boolean stale = allowed.values().stream().anyMatch(Contribution::stale);
        RetrievalModel.Decision decision = stale ? RetrievalModel.Decision.STALE
                : candidates.size() > 1 ? RetrievalModel.Decision.AMBIGUOUS
                        : candidates.isEmpty() && complete ? RetrievalModel.Decision.NOT_LOCATED_IN_SCOPE
                                : RetrievalModel.Decision.INSUFFICIENT;
        return new RetrievalModel.EvaluatedOutcome(
                complete ? RetrievalModel.Coverage.COMPLETE : RetrievalModel.Coverage.PARTIAL,
                decision,
                unavailable ? java.util.Optional.of(RetrievalModel.Impediment.UNAVAILABLE) : java.util.Optional.empty(),
                candidates);
    }

    private static boolean isTerminal(Map<RetrievalModel.SourceId, Contribution> contributions,
            RetrievalModel.SourceId sourceId) {
        return contributions.containsKey(sourceId) && contributions.get(sourceId).terminal();
    }
}
