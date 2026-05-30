package service;

import model.Snapshot;
import operation.Operation;
import java.util.List;

/**
 * Append-only operation log + periodic snapshots.
 *
 * In production this would be backed by a per-doc Kafka partition (offset ==
 * revision number) or a time-series table in Cassandra/DynamoDB.  Periodic
 * snapshots limit how far back a catch-up client must replay.
 */
public interface OperationStore {

    /** Append `op` and return the server-assigned revision number. */
    long append(String docId, Operation op);

    /** All ops for `docId` with assignedRevision > `fromRevision`, in order. */
    List<Operation> opsSince(String docId, long fromRevision);

    /** Most recent snapshot at or before `maxRevision`; null if none exists. */
    Snapshot latestSnapshot(String docId);

    /** Persist a snapshot (called periodically by DocumentSession). */
    void saveSnapshot(Snapshot snapshot);
}
