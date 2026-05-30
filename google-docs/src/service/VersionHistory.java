package service;

import buffer.TextBuffer;
import model.Snapshot;
import operation.*;

import java.time.Instant;
import java.util.List;

/**
 * Reconstructs document content at any historical revision.
 *
 * Algorithm:
 *   1. Find the latest snapshot at or before the target revision.
 *   2. Replay ops from snapshot.revision up to targetRevision.
 *
 * This is why snapshots matter: without them every restore replays from rev 0.
 */
public class VersionHistory {

    private final OperationStore store;

    public VersionHistory(OperationStore store) {
        this.store = store;
    }

    /**
     * Returns the document text as it was at `targetRevision`.
     */
    public String getContentAtRevision(String docId, long targetRevision) {
        Snapshot snapshot = store.latestSnapshot(docId);

        String  baseContent  = "";
        long    baseRevision = 0;

        if (snapshot != null && snapshot.getRevision() <= targetRevision) {
            baseContent  = snapshot.getContent();
            baseRevision = snapshot.getRevision();
        }

        TextBuffer buffer = new TextBuffer(baseContent);

        List<Operation> ops = store.opsSince(docId, baseRevision);
        for (Operation op : ops) {
            if (op.getAssignedRevision() > targetRevision) break;
            applyToBuffer(buffer, op);
        }

        return buffer.getText();
    }

    /**
     * Lists revision numbers and their content summaries.
     */
    public void printHistory(String docId, long upToRevision) {
        List<Operation> ops = store.opsSince(docId, 0);
        TextBuffer buffer = new TextBuffer("");
        System.out.println("=== Version History for doc=" + docId + " ===");
        for (Operation op : ops) {
            if (op.getAssignedRevision() > upToRevision) break;
            applyToBuffer(buffer, op);
            System.out.printf("  rev=%d  %-50s → \"%s\"%n",
                op.getAssignedRevision(), op, buffer.getText());
        }
        System.out.println("=== end ===");
    }

    /** Trigger a snapshot — call this every N ops to bound replay cost. */
    public void takeSnapshot(String docId, long currentRevision, String currentContent) {
        store.saveSnapshot(new Snapshot(docId, currentRevision, currentContent, Instant.now()));
    }

    private void applyToBuffer(TextBuffer buf, Operation op) {
        if (op instanceof InsertOp) {
            InsertOp ins = (InsertOp) op;
            buf.insert(ins.getPos(), ins.getText());
        } else if (op instanceof DeleteOp) {
            DeleteOp del = (DeleteOp) op;
            buf.delete(del.getPos(), del.getLength());
        }
        // FormatOp: no text change, skip in text-only replay
    }
}
