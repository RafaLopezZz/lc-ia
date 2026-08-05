package lcia.syntheticretrieval;

import java.util.Objects;
import java.util.List;
import java.util.function.Supplier;

public final class SyntheticRetrieval {

    private SyntheticRetrieval() {
    }

    public enum InputProvenance {
        SYNTHETIC,
        NON_SYNTHETIC
    }

    public enum SimulationKind {
        IN_MEMORY_SYNTHETIC,
        REAL_ADAPTER
    }

    public record Scenario(
            InputProvenance provenance,
            SimulationKind simulationKind,
            AuthorizationContext authorization,
            List<SourceGrant> grants,
            RetrievalIntent intent,
            RetrievalModel.SnapshotId correlationId,
            List<RetrievalModel.Gateway> gateways,
            List<InMemorySimulation.Contribution> contributions) {
        public Scenario {
            provenance = Objects.requireNonNull(provenance, "provenance");
            simulationKind = Objects.requireNonNull(simulationKind, "simulationKind");
            authorization = Objects.requireNonNull(authorization, "authorization");
            grants = List.copyOf(grants);
            intent = Objects.requireNonNull(intent, "intent");
            correlationId = Objects.requireNonNull(correlationId, "correlationId");
            gateways = List.copyOf(gateways);
            contributions = List.copyOf(contributions);
        }
    }

    public sealed interface OperationResult permits Completed, Clarification, Denied {
    }

    public record Completed(RetrievalModel.RetrievalSnapshot snapshot, RetrievalModel.RetrievalOutcome outcome,
            RetrievalModel.MinimizedTrace trace) implements OperationResult {
    }

    public record Clarification(RetrievalModel.MinimizedTrace trace) implements OperationResult {
    }

    public record Denied(RetrievalModel.MinimizedTrace trace) implements OperationResult {
    }

    public static final class Operation {
        private final InMemorySimulation simulation;

        public Operation(InMemorySimulation simulation) {
            this.simulation = Objects.requireNonNull(simulation, "simulation");
        }

        public OperationResult execute(Scenario scenario) {
            return SyntheticOnlyGuard.inspectOnlySynthetic(scenario.provenance(), scenario.simulationKind(), () -> executeSynthetic(scenario));
        }

        private OperationResult executeSynthetic(Scenario scenario) {
            ScopeResolution resolution = simulation.resolve(scenario.authorization(), scenario.grants(), scenario.intent());
            if (resolution instanceof ScopeResolution.Denied) {
                return new Denied(new RetrievalModel.MinimizedTrace(scenario.correlationId()));
            }
            if (resolution instanceof ScopeResolution.Clarification clarification) {
                return new Clarification(RetrievalModel.MinimizedTrace.clarification(scenario.correlationId(), clarification.options()));
            }
            RetrievalModel.Scope scope = ((ScopeResolution.Selected) resolution).scope();
            RetrievalModel.RetrievalSnapshot snapshot = new RetrievalModel.RetrievalSnapshot(scenario.correlationId(),
                    scenario.authorization().activeTenantId(), scope, scenario.gateways());
            RetrievalModel.RetrievalOutcome outcome = simulation.consolidate(snapshot, scenario.contributions());
            return new Completed(snapshot, outcome, RetrievalModel.MinimizedTrace.from(snapshot, outcome));
        }
    }
}

final class SyntheticOnlyGuard {

    private SyntheticOnlyGuard() {
    }

    static <T> T inspectOnlySynthetic(
            SyntheticRetrieval.InputProvenance provenance,
            SyntheticRetrieval.SimulationKind simulationKind,
            Supplier<T> fixtureInspection) {
        if (provenance != SyntheticRetrieval.InputProvenance.SYNTHETIC
                || simulationKind != SyntheticRetrieval.SimulationKind.IN_MEMORY_SYNTHETIC) {
            throw new IllegalArgumentException("Synthetic in-memory fixtures are required");
        }
        return Objects.requireNonNull(fixtureInspection, "fixtureInspection").get();
    }
}
