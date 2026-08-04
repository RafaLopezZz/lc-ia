package lcia.syntheticretrieval;

import java.util.Objects;
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
