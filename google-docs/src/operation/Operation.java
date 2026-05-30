package operation;

/**
 * Base class for all document operations.
 *
 * Key fields for OT:
 *   - baseRevision : the document revision the client had when it created this op.
 *                    The server transforms the op against everything that happened
 *                    between baseRevision and the current head.
 *   - siteId       : used as a deterministic tie-break when two concurrent inserts
 *                    land at the same position (lower siteId wins = goes first).
 *   - assignedRevision : set by the server after sequencing; 0 until then.
 */
public abstract class Operation {
    protected final String opId;
    protected final String authorId;
    protected final String siteId;
    protected final long   baseRevision;
    protected final OpType type;
    protected       long   assignedRevision;   // filled in by DocumentSession

    protected Operation(String opId, String authorId, String siteId,
                        long baseRevision, OpType type) {
        this.opId             = opId;
        this.authorId         = authorId;
        this.siteId           = siteId;
        this.baseRevision     = baseRevision;
        this.type             = type;
        this.assignedRevision = 0;
    }

    public abstract Operation copy();

    public String getOpId()             { return opId; }
    public String getAuthorId()         { return authorId; }
    public String getSiteId()           { return siteId; }
    public long   getBaseRevision()     { return baseRevision; }
    public OpType getType()             { return type; }
    public long   getAssignedRevision() { return assignedRevision; }
    public void   setAssignedRevision(long r) { this.assignedRevision = r; }
}
