package service;

import operation.*;
import java.util.List;

/**
 * Operational Transformation engine — Jupiter / Google Wave model.
 *
 * Supported pairs:
 *   Insert  vs Insert   — position shift + deterministic tie-break
 *   Insert  vs Delete   — position clamped to deletion start if inside range
 *   Delete  vs Insert   — deletion range expanded for inserted chars
 *   Delete  vs Delete   — overlapping deletes reduce remaining length
 *   Format  vs Insert/Delete — position/length adjusted like Delete
 *
 * CCI correctness properties targeted:
 *   Convergence (C)   — all replicas reach the same state
 *   Causality (C)     — server assigns a total order; clients respect it
 *   Intention (I)     — each user's edit applies at the logically intended location
 */
public class OTEngineImpl implements OTEngine {

    @Override
    public Operation transform(Operation incoming, Operation against) {
        if (incoming instanceof InsertOp) {
            if (against instanceof InsertOp) return ii((InsertOp) incoming, (InsertOp) against);
            if (against instanceof DeleteOp) return id((InsertOp) incoming, (DeleteOp) against);
        }
        if (incoming instanceof DeleteOp) {
            if (against instanceof InsertOp) return di((DeleteOp) incoming, (InsertOp) against);
            if (against instanceof DeleteOp) return dd((DeleteOp) incoming, (DeleteOp) against);
        }
        if (incoming instanceof FormatOp) {
            if (against instanceof InsertOp) return fi((FormatOp) incoming, (InsertOp) against);
            if (against instanceof DeleteOp) return fd((FormatOp) incoming, (DeleteOp) against);
        }
        return incoming;   // no-op for same-type format or unrecognised combos
    }

    @Override
    public Operation transformAgainstAll(Operation op, List<Operation> concurrent) {
        Operation current = op;
        for (Operation against : concurrent) {
            current = transform(current, against);
        }
        return current;
    }

    // ---------------------------------------------------------------- Insert vs Insert
    /**
     * If `against` inserts before (or at the same position with lower siteId),
     * shift `incoming` right by the length of the inserted text.
     *
     * Tie-break: lexicographically smaller siteId goes first (its insert stays
     * at the lower index).  This is the deterministic rule that ensures every
     * replica makes the same choice and TP1 holds.
     */
    private InsertOp ii(InsertOp incoming, InsertOp against) {
        InsertOp result = incoming.copy();
        if (against.getPos() < incoming.getPos()) {
            result.setPos(incoming.getPos() + against.getText().length());
        } else if (against.getPos() == incoming.getPos()
                && against.getSiteId().compareTo(incoming.getSiteId()) < 0) {
            // against wins the tie: it effectively occupies this position first
            result.setPos(incoming.getPos() + against.getText().length());
        }
        return result;
    }

    // ---------------------------------------------------------------- Insert vs Delete
    /**
     * If `against` deleted text before the insert position, shift left.
     * If the insert position is inside the deleted range, clamp to the start
     * of the deletion (the closest surviving position).
     */
    private InsertOp id(InsertOp incoming, DeleteOp against) {
        InsertOp result = incoming.copy();
        int agStart = against.getPos();
        int agEnd   = agStart + against.getLength();

        if (agEnd <= incoming.getPos()) {
            result.setPos(incoming.getPos() - against.getLength());
        } else if (agStart < incoming.getPos()) {
            result.setPos(agStart);           // clamp to deletion start
        }
        return result;
    }

    // ---------------------------------------------------------------- Delete vs Insert
    /**
     * If `against` inserts before the deletion, shift right.
     * If `against` inserts inside the deletion range, expand the deletion to
     * include the newly inserted text (standard OT intent: "delete this span").
     */
    private DeleteOp di(DeleteOp incoming, InsertOp against) {
        DeleteOp result = incoming.copy();
        int agPos = against.getPos();
        int agLen = against.getText().length();

        if (agPos <= incoming.getPos()) {
            result.setPos(incoming.getPos() + agLen);
        } else if (agPos < incoming.getPos() + incoming.getLength()) {
            result.setLength(incoming.getLength() + agLen);
        }
        return result;
    }

    // ---------------------------------------------------------------- Delete vs Delete
    /**
     * Chars that `against` already deleted no longer exist; trim them out of
     * `incoming`'s range.
     *
     * Three cases:
     *   1. against entirely before incoming  → shift incoming left
     *   2. against entirely after  incoming  → no change
     *   3. overlap                           → shrink incoming by the overlap,
     *                                          adjust start if against began earlier
     */
    private DeleteOp dd(DeleteOp incoming, DeleteOp against) {
        DeleteOp result  = incoming.copy();
        int inStart = incoming.getPos();
        int inEnd   = inStart + incoming.getLength();
        int agStart = against.getPos();
        int agEnd   = agStart + against.getLength();

        if (agEnd <= inStart) {
            // against is entirely to the left
            result.setPos(inStart - against.getLength());
        } else if (agStart >= inEnd) {
            // against is entirely to the right — no change
        } else {
            // overlap: compute how many chars incoming still needs to delete
            int overlapStart = Math.max(inStart, agStart);
            int overlapEnd   = Math.min(inEnd,   agEnd);
            int overlap      = overlapEnd - overlapStart;

            result.setPos(Math.min(inStart, agStart));
            result.setLength(Math.max(0, incoming.getLength() - overlap));
        }
        return result;
    }

    // ---------------------------------------------------------------- Format vs Insert
    private FormatOp fi(FormatOp incoming, InsertOp against) {
        FormatOp result = incoming.copy();
        int agPos = against.getPos();
        int agLen = against.getText().length();

        if (agPos <= incoming.getPos()) {
            result.setPos(incoming.getPos() + agLen);
        } else if (agPos < incoming.getPos() + incoming.getLength()) {
            result.setLength(incoming.getLength() + agLen);
        }
        return result;
    }

    // ---------------------------------------------------------------- Format vs Delete
    private FormatOp fd(FormatOp incoming, DeleteOp against) {
        FormatOp result = incoming.copy();
        int agStart = against.getPos();
        int agEnd   = agStart + against.getLength();
        int inStart = incoming.getPos();
        int inEnd   = inStart + incoming.getLength();

        if (agEnd <= inStart) {
            result.setPos(inStart - against.getLength());
        } else if (agStart < inEnd) {
            int overlapStart = Math.max(inStart, agStart);
            int overlapEnd   = Math.min(inEnd,   agEnd);
            result.setPos(Math.min(inStart, agStart));
            result.setLength(Math.max(0, incoming.getLength() - (overlapEnd - overlapStart)));
        }
        return result;
    }
}
