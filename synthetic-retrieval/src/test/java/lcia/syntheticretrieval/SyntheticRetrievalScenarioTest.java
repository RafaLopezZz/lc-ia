package lcia.syntheticretrieval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Test;

class SyntheticRetrievalScenarioTest {

    @Test
    void runnerDiscoversJUnitJupiter() {
        assertTrue(true);
    }

    @Test
    void rejectsNonSyntheticProvenanceBeforeInspectingFixtures() {
        AtomicBoolean inspected = new AtomicBoolean();

        assertThrows(IllegalArgumentException.class, () -> SyntheticOnlyGuard.inspectOnlySynthetic(
                SyntheticRetrieval.InputProvenance.NON_SYNTHETIC,
                SyntheticRetrieval.SimulationKind.IN_MEMORY_SYNTHETIC,
                () -> {
                    inspected.set(true);
                    return "fixture";
                }));

        assertFalse(inspected.get());
    }

    @Test
    void rejectsNonSyntheticSimulationBeforeInspectingFixtures() {
        AtomicBoolean inspected = new AtomicBoolean();

        assertThrows(IllegalArgumentException.class, () -> SyntheticOnlyGuard.inspectOnlySynthetic(
                SyntheticRetrieval.InputProvenance.SYNTHETIC,
                SyntheticRetrieval.SimulationKind.REAL_ADAPTER,
                () -> {
                    inspected.set(true);
                    return "fixture";
                }));

        assertFalse(inspected.get());
    }

    @Test
    void acceptsSyntheticInMemoryFixtures() {
        assertEquals("fixture", SyntheticOnlyGuard.inspectOnlySynthetic(
                SyntheticRetrieval.InputProvenance.SYNTHETIC,
                SyntheticRetrieval.SimulationKind.IN_MEMORY_SYNTHETIC,
                () -> "fixture"));
    }

    @Test
    void keepsScopeSourcesOrderedAndImmutable() {
        List<RetrievalModel.SourceId> mutableSources = new ArrayList<>(List.of(
                new RetrievalModel.SourceId("source-b"),
                new RetrievalModel.SourceId("source-a")));

        RetrievalModel.Scope scope = new RetrievalModel.Scope(
                new RetrievalModel.ScopeId("scope-1"), mutableSources);
        mutableSources.clear();

        assertEquals(List.of(new RetrievalModel.SourceId("source-a"), new RetrievalModel.SourceId("source-b")),
                scope.sources());
        assertThrows(UnsupportedOperationException.class,
                () -> scope.sources().add(new RetrievalModel.SourceId("source-c")));
    }

    @Test
    void keepsCandidatesOrderedAndImmutable() {
        RetrievalModel.EvaluatedOutcome outcome = new RetrievalModel.EvaluatedOutcome(
                RetrievalModel.Coverage.PARTIAL,
                RetrievalModel.Decision.INSUFFICIENT,
                Optional.of(RetrievalModel.Impediment.UNAVAILABLE),
                List.of(new RetrievalModel.CandidateId("candidate-b"), new RetrievalModel.CandidateId("candidate-a")));

        assertEquals(List.of(new RetrievalModel.CandidateId("candidate-a"), new RetrievalModel.CandidateId("candidate-b")),
                outcome.candidates());
        assertThrows(UnsupportedOperationException.class,
                () -> outcome.candidates().add(new RetrievalModel.CandidateId("candidate-c")));
    }

    @Test
    void rejectsInvalidNotLocatedCombinations() {
        assertThrows(IllegalArgumentException.class, () -> new RetrievalModel.EvaluatedOutcome(
                RetrievalModel.Coverage.PARTIAL,
                RetrievalModel.Decision.NOT_LOCATED_IN_SCOPE,
                Optional.empty(),
                List.of()));
        assertThrows(IllegalArgumentException.class, () -> new RetrievalModel.EvaluatedOutcome(
                RetrievalModel.Coverage.COMPLETE,
                RetrievalModel.Decision.NOT_LOCATED_IN_SCOPE,
                Optional.of(RetrievalModel.Impediment.UNAVAILABLE),
                List.of()));
        assertThrows(IllegalArgumentException.class, () -> new RetrievalModel.EvaluatedOutcome(
                RetrievalModel.Coverage.COMPLETE,
                RetrievalModel.Decision.NOT_LOCATED_IN_SCOPE,
                Optional.empty(),
                List.of(new RetrievalModel.CandidateId("candidate-1"))));
    }

    @Test
    void deniedOutcomeHasNoCoverageDecisionOrCandidates() {
        RetrievalModel.RetrievalOutcome outcome = new RetrievalModel.DeniedOutcome();

        assertTrue(outcome instanceof RetrievalModel.DeniedOutcome);
        assertEquals(0, RetrievalModel.DeniedOutcome.class.getRecordComponents().length);
    }

    @Test
    void excludesUnequivocalFromDecisionDomain() {
        assertThrows(IllegalArgumentException.class,
                () -> RetrievalModel.Decision.valueOf("UNEQUIVOCAL"));
    }

    @Test
    void resolvesOnlyGrantedScopesWithinTheActiveTenant() {
        RetrievalModel.TenantId tenant = new RetrievalModel.TenantId("tenant-a");
        RetrievalModel.ActorId actor = new RetrievalModel.ActorId("actor-a");
        RetrievalModel.SourceId source = new RetrievalModel.SourceId("source-a");
        InMemorySimulation resolver = resolver(tenant, scope("scope-a", source));

        ScopeResolution resolution = resolver.resolve(context(actor, tenant, true),
                List.of(grant(actor, tenant, source, true)));

        assertEquals(new ScopeResolution.Selected(new RetrievalModel.Scope(
                new RetrievalModel.ScopeId("scope-a"), List.of(source))), resolution);
    }

    @Test
    void deniesInvalidContextAndRevokedGrantWithoutDetails() {
        RetrievalModel.TenantId tenant = new RetrievalModel.TenantId("tenant-a");
        RetrievalModel.ActorId actor = new RetrievalModel.ActorId("actor-a");
        RetrievalModel.SourceId source = new RetrievalModel.SourceId("source-a");
        InMemorySimulation resolver = resolver(tenant, scope("scope-a", source));

        assertEquals(new ScopeResolution.Denied(), resolver.resolve(context(actor, tenant, false), List.of()));
        assertEquals(new ScopeResolution.Denied(), resolver.resolve(context(actor, tenant, true),
                List.of(grant(actor, tenant, source, false))));
        assertEquals(0, ScopeResolution.Denied.class.getRecordComponents().length);
    }

    @Test
    void collectionRequiresGrantsForOptionalSourcesAndDeduplicatesSharedSources() {
        RetrievalModel.TenantId tenant = new RetrievalModel.TenantId("tenant-a");
        RetrievalModel.ActorId actor = new RetrievalModel.ActorId("actor-a");
        RetrievalModel.SourceId required = new RetrievalModel.SourceId("source-required");
        RetrievalModel.SourceId optional = new RetrievalModel.SourceId("source-optional");
        InMemorySimulation resolver = resolver(tenant, new RetrievalModel.Scope(
                new RetrievalModel.ScopeId("collection-a"), List.of(required, optional, required)));

        assertEquals(new ScopeResolution.Denied(), resolver.resolve(context(actor, tenant, true),
                List.of(grant(actor, tenant, required, true))));
        assertEquals(new ScopeResolution.Selected(new RetrievalModel.Scope(
                new RetrievalModel.ScopeId("collection-a"), List.of(optional, required))),
                resolver.resolve(context(actor, tenant, true), List.of(
                        grant(actor, tenant, required, true), grant(actor, tenant, optional, true))));
    }

    @Test
    void selectsTheSmallestAuthorizedScopeAndClarifiesEquivalentScopesDeterministically() {
        RetrievalModel.TenantId tenant = new RetrievalModel.TenantId("tenant-a");
        RetrievalModel.ActorId actor = new RetrievalModel.ActorId("actor-a");
        RetrievalModel.SourceId sourceA = new RetrievalModel.SourceId("source-a");
        RetrievalModel.SourceId sourceB = new RetrievalModel.SourceId("source-b");
        List<SourceGrant> grants = List.of(grant(actor, tenant, sourceA, true), grant(actor, tenant, sourceB, true));

        InMemorySimulation smallest = resolver(tenant, scope("collection-wide", sourceA, sourceB), scope("source-a", sourceA));
        assertEquals(new ScopeResolution.Selected(new RetrievalModel.Scope(
                new RetrievalModel.ScopeId("source-a"), List.of(sourceA))),
                smallest.resolve(context(actor, tenant, true), grants));

        InMemorySimulation equivalent = resolver(tenant, scope("scope-b", sourceB), scope("scope-a", sourceA));
        ScopeResolution expected = new ScopeResolution.Clarification(List.of(
                new RetrievalModel.ScopeId("scope-a"), new RetrievalModel.ScopeId("scope-b")));
        assertEquals(expected, equivalent.resolve(context(actor, tenant, true), grants));
        assertEquals(expected, equivalent.resolve(context(actor, tenant, true), grants));
    }

    @Test
    void crossTenantEntitiesNeverEnterResolutionOrChangeTheSafeResult() {
        RetrievalModel.TenantId activeTenant = new RetrievalModel.TenantId("tenant-a");
        RetrievalModel.TenantId otherTenant = new RetrievalModel.TenantId("tenant-b");
        RetrievalModel.ActorId actor = new RetrievalModel.ActorId("actor-a");
        RetrievalModel.SourceId activeSource = new RetrievalModel.SourceId("source-a");
        RetrievalModel.SourceId crossTenantSource = new RetrievalModel.SourceId("source-cross");
        AuthorizationContext context = context(actor, activeTenant, true);
        List<SourceGrant> grants = List.of(grant(actor, activeTenant, activeSource, true));
        InMemorySimulation withoutCrossTenant = resolver(activeTenant, scope("scope-a", activeSource));
        InMemorySimulation withCrossTenant = new InMemorySimulation(List.of(
                new TenantCatalog(activeTenant, List.of(scope("scope-a", activeSource))),
                new TenantCatalog(otherTenant, List.of(scope("scope-cross", crossTenantSource)))));

        ScopeResolution expected = withoutCrossTenant.resolve(context, grants);
        assertEquals(expected, withCrossTenant.resolve(context, grants));
        assertEquals(new ScopeResolution.Denied(), withCrossTenant.resolve(context,
                List.of(grant(actor, activeTenant, activeSource, false), grant(actor, otherTenant, crossTenantSource, true))));
    }

    @Test
    void snapshotDefensivelyFreezesGatewaysBeforeFixtureMutation() {
        RetrievalModel.TenantId tenant = new RetrievalModel.TenantId("tenant-a");
        RetrievalModel.SourceId source = new RetrievalModel.SourceId("source-a");
        List<RetrievalModel.Gateway> mutable = new ArrayList<>(List.of(new RetrievalModel.Gateway(source, true)));
        RetrievalModel.RetrievalSnapshot snapshot = snapshot(tenant, "snapshot-a", mutable);
        mutable.clear();

        assertEquals(RetrievalModel.Coverage.COMPLETE, ((RetrievalModel.EvaluatedOutcome) new InMemorySimulation(List.of()).consolidate(snapshot,
                List.of(contribution(tenant, source, true, false, "candidate-a")))).coverage());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.gateways().clear());
    }

    @Test
    void missingGatewaysRemainPartialUnavailableWhilePreservingCandidates() {
        RetrievalModel.TenantId tenant = new RetrievalModel.TenantId("tenant-a");
        RetrievalModel.SourceId required = new RetrievalModel.SourceId("source-a");
        RetrievalModel.SourceId optional = new RetrievalModel.SourceId("source-b");
        InMemorySimulation simulation = new InMemorySimulation(List.of());
        RetrievalModel.EvaluatedOutcome requiredMissing = (RetrievalModel.EvaluatedOutcome) simulation.consolidate(
                snapshot(tenant, "snapshot-required", List.of(new RetrievalModel.Gateway(required, true), new RetrievalModel.Gateway(optional, false))),
                List.of(contribution(tenant, optional, true, false, "candidate-a")));
        RetrievalModel.EvaluatedOutcome optionalMissing = (RetrievalModel.EvaluatedOutcome) simulation.consolidate(
                snapshot(tenant, "snapshot-optional", List.of(new RetrievalModel.Gateway(required, true), new RetrievalModel.Gateway(optional, false))),
                List.of(contribution(tenant, required, true, false, "candidate-a")));

        assertEquals(new RetrievalModel.EvaluatedOutcome(RetrievalModel.Coverage.PARTIAL,
                RetrievalModel.Decision.INSUFFICIENT, Optional.of(RetrievalModel.Impediment.UNAVAILABLE),
                List.of(new RetrievalModel.CandidateId("candidate-a"))), requiredMissing);
        assertEquals(List.of(new RetrievalModel.CandidateId("candidate-a")), optionalMissing.candidates());
        assertEquals(Optional.of(RetrievalModel.Impediment.UNAVAILABLE), optionalMissing.impediment());
    }

    @Test
    void choosesOnlyConservativeDecisionsAndLimitsNotLocatedToCompleteScope() {
        RetrievalModel.TenantId tenant = new RetrievalModel.TenantId("tenant-a");
        RetrievalModel.SourceId source = new RetrievalModel.SourceId("source-a");
        InMemorySimulation simulation = new InMemorySimulation(List.of());
        RetrievalModel.RetrievalSnapshot snapshot = snapshot(tenant, "snapshot-a", List.of(new RetrievalModel.Gateway(source, true)));

        assertEquals(RetrievalModel.Decision.AMBIGUOUS, ((RetrievalModel.EvaluatedOutcome) simulation.consolidate(snapshot,
                List.of(contribution(tenant, source, true, false, "candidate-a", "candidate-b")))).decision());
        assertEquals(RetrievalModel.Decision.INSUFFICIENT, ((RetrievalModel.EvaluatedOutcome) simulation.consolidate(snapshot,
                List.of(contribution(tenant, source, true, false, "candidate-a")))).decision());
        assertEquals(RetrievalModel.Decision.STALE, ((RetrievalModel.EvaluatedOutcome) simulation.consolidate(snapshot,
                List.of(contribution(tenant, source, true, true, "candidate-a")))).decision());
        RetrievalModel.EvaluatedOutcome notLocated = (RetrievalModel.EvaluatedOutcome) simulation.consolidate(snapshot,
                List.of(contribution(tenant, source, true, false)));
        assertEquals(new RetrievalModel.EvaluatedOutcome(RetrievalModel.Coverage.COMPLETE,
                RetrievalModel.Decision.NOT_LOCATED_IN_SCOPE, Optional.empty(), List.of()), notLocated);
    }

    @ParameterizedTest
    @MethodSource("permutedContributions")
    void keepsCandidateAndTraceOrderStableAndExcludesCrossTenantContributions(List<InMemorySimulation.Contribution> contributions) {
        RetrievalModel.TenantId tenant = new RetrievalModel.TenantId("tenant-a");
        RetrievalModel.SourceId source = new RetrievalModel.SourceId("source-a");
        RetrievalModel.RetrievalSnapshot snapshot = snapshot(tenant, "snapshot-a", List.of(new RetrievalModel.Gateway(source, true)));
        RetrievalModel.RetrievalOutcome outcome = new InMemorySimulation(List.of()).consolidate(snapshot, contributions);

        assertEquals(List.of(new RetrievalModel.CandidateId("candidate-a"), new RetrievalModel.CandidateId("candidate-b")),
                ((RetrievalModel.EvaluatedOutcome) outcome).candidates());
        assertEquals(new RetrievalModel.MinimizedTrace(new RetrievalModel.SnapshotId("snapshot-a"), new RetrievalModel.ScopeId("scope-a"),
                Optional.of(RetrievalModel.Coverage.COMPLETE), Optional.of(RetrievalModel.Decision.AMBIGUOUS), Optional.empty(),
                List.of(new RetrievalModel.CandidateId("candidate-a"), new RetrievalModel.CandidateId("candidate-b"))),
                RetrievalModel.MinimizedTrace.from(snapshot, outcome));
    }

    @Test
    void traceOnlyCarriesAllowedOpaqueDimensionsAndRejectsInvalidState() {
        assertEquals(6, RetrievalModel.MinimizedTrace.class.getRecordComponents().length);
        assertThrows(IllegalArgumentException.class, () -> new RetrievalModel.MinimizedTrace(
                new RetrievalModel.SnapshotId("snapshot-a"), new RetrievalModel.ScopeId("scope-a"),
                Optional.of(RetrievalModel.Coverage.PARTIAL), Optional.of(RetrievalModel.Decision.NOT_LOCATED_IN_SCOPE),
                Optional.of(RetrievalModel.Impediment.UNAVAILABLE), List.of()));
    }

    private static Stream<List<InMemorySimulation.Contribution>> permutedContributions() {
        RetrievalModel.TenantId tenant = new RetrievalModel.TenantId("tenant-a");
        RetrievalModel.TenantId other = new RetrievalModel.TenantId("tenant-b");
        RetrievalModel.SourceId source = new RetrievalModel.SourceId("source-a");
        return Stream.of(List.of(contribution(other, source, true, false, "candidate-cross"), contribution(tenant, source, true, false, "candidate-b", "candidate-a")),
                List.of(contribution(tenant, source, true, false, "candidate-a", "candidate-b"), contribution(other, source, true, false, "candidate-cross")));
    }

    private static RetrievalModel.RetrievalSnapshot snapshot(RetrievalModel.TenantId tenant, String id, List<RetrievalModel.Gateway> gateways) {
        return new RetrievalModel.RetrievalSnapshot(new RetrievalModel.SnapshotId(id), tenant,
                new RetrievalModel.Scope(new RetrievalModel.ScopeId("scope-a"), gateways.stream().map(RetrievalModel.Gateway::sourceId).toList()), gateways);
    }

    private static InMemorySimulation.Contribution contribution(RetrievalModel.TenantId tenant, RetrievalModel.SourceId source,
            boolean terminal, boolean stale, String... candidates) {
        return new InMemorySimulation.Contribution(tenant, source, terminal, stale,
                Stream.of(candidates).map(RetrievalModel.CandidateId::new).toList());
    }

    private static InMemorySimulation resolver(RetrievalModel.TenantId tenant, RetrievalModel.Scope... scopes) {
        return new InMemorySimulation(List.of(new TenantCatalog(tenant, List.of(scopes))));
    }

    private static RetrievalModel.Scope scope(String id, RetrievalModel.SourceId... sources) {
        return new RetrievalModel.Scope(new RetrievalModel.ScopeId(id), List.of(sources));
    }

    private static AuthorizationContext context(
            RetrievalModel.ActorId actor, RetrievalModel.TenantId tenant, boolean membershipActive) {
        return new AuthorizationContext(actor, tenant, new Membership(tenant, membershipActive), true);
    }

    private static SourceGrant grant(
            RetrievalModel.ActorId actor, RetrievalModel.TenantId tenant, RetrievalModel.SourceId source, boolean active) {
        return new SourceGrant(actor, tenant, source, active);
    }
}
