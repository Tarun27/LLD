package service;

import model.Snapshot;
import operation.Operation;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * In-memory implementation — suitable for single-node demos and unit tests.
 * A production store would be backed by Kafka or a durable DB.
 */
public class InMemoryOperationStore implements OperationStore {

    private final Map<String, List<Operation>> opLog       = new ConcurrentHashMap<>();
    private final Map<String, Snapshot>        snapshots   = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong>      revCounters = new ConcurrentHashMap<>();

    @Override
    public long append(String docId, Operation op) {
        opLog.computeIfAbsent(docId, k -> Collections.synchronizedList(new ArrayList<>()));
        revCounters.computeIfAbsent(docId, k -> new AtomicLong(0));

        long revision = revCounters.get(docId).incrementAndGet();
        op.setAssignedRevision(revision);
        opLog.get(docId).add(op);
        return revision;
    }

    @Override
    public List<Operation> opsSince(String docId, long fromRevision) {
        List<Operation> all = opLog.getOrDefault(docId, Collections.emptyList());
        return all.stream()
                  .filter(op -> op.getAssignedRevision() > fromRevision)
                  .collect(Collectors.toList());
    }

    @Override
    public Snapshot latestSnapshot(String docId) {
        return snapshots.get(docId);
    }

    @Override
    public void saveSnapshot(Snapshot snapshot) {
        snapshots.put(snapshot.getDocId(), snapshot);
    }
}
