package service;

import operation.Operation;
import java.util.List;

/**
 * Pure transform logic — no I/O, no state.
 *
 * transform(incoming, against) adjusts `incoming` so it can be applied
 * *after* `against` has already been applied, while preserving the original
 * intention of `incoming`.
 *
 * The transform must satisfy TP1 (convergence under server-ordered OT):
 *   apply(apply(doc, opA), transform(opB, opA))
 *     == apply(apply(doc, opB), transform(opA, opB))
 */
public interface OTEngine {
    Operation transform(Operation incoming, Operation against);

    /**
     * Convenience: transform `op` sequentially against each op in `concurrent`
     * (which must be in server-assigned revision order).
     */
    Operation transformAgainstAll(Operation op, List<Operation> concurrent);
}
