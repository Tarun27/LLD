package service;

import model.Document;
import operation.*;
import session.Session;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Authoritative, single-writer session for ONE open document.
 *
 * This is the "actor mailbox" described in the architecture section.
 * Routing all ops for a docId to one instance (via consistent-hashing on
 * docId) means we never need a distributed lock — the instance's call stack
 * is the serialisation point.
 *
 * Responsibilities:
 *   1. Transform the incoming op against any concurrent ops (those with
 *      assignedRevision > op.baseRevision).
 *   2. Apply the transformed op to the in-memory TextBuffer.
 *   3. Assign the next revision.
 *   4. Persist to the OperationStore (append-only log).
 *   5. Broadcast the transformed op to every other live session so their
 *      clients can apply it locally.
 *
 * Thread safety: applyAndSequence is synchronized — only one op at a time
 * gets sequenced, which is the invariant OT requires.
 */
public class DocumentSession {

    private final Document                   doc;
    private final Map<String, Session>       collaborators = new ConcurrentHashMap<>();
    private final OTEngine                   otEngine;
    private final OperationStore             store;

    // Simple broadcast sink — in production this is a WebSocket write per session
    public interface BroadcastSink {
        void send(String sessionId, Operation op, long revision);
    }
    private BroadcastSink broadcastSink = (sid, op, rev) -> {};  // no-op default

    public DocumentSession(Document doc, OTEngine otEngine, OperationStore store) {
        this.doc      = doc;
        this.otEngine = otEngine;
        this.store    = store;
    }

    public void setBroadcastSink(BroadcastSink sink) {
        this.broadcastSink = sink;
    }

    // ---------------------------------------------------------------- join / leave

    public void join(Session session) {
        collaborators.put(session.getSessionId(), session);
        System.out.println("[DocumentSession] " + session.getUserId() + " joined doc=" + doc.getDocId());
    }

    public void leave(String sessionId) {
        Session s = collaborators.remove(sessionId);
        if (s != null) {
            System.out.println("[DocumentSession] " + s.getUserId() + " left doc=" + doc.getDocId());
        }
    }

    // ---------------------------------------------------------------- submit op (the hot path)

    /**
     * Called when a client sends SUBMIT_OP over the WebSocket.
     *
     * @return the server-assigned revision number
     */
    public synchronized long applyAndSequence(Operation clientOp) {
        // 1. Find ops the client hasn't seen yet (concurrent ops)
        List<Operation> concurrent = store.opsSince(doc.getDocId(), clientOp.getBaseRevision());

        // 2. Transform incoming op against each concurrent op (in revision order)
        Operation transformed = otEngine.transformAgainstAll(clientOp, concurrent);

        // 3. Apply to the live TextBuffer
        applyToBuffer(transformed);

        // 4. Persist + assign revision
        long rev = store.append(doc.getDocId(), transformed);
        doc.incrementRevision();

        System.out.printf("[DocumentSession] doc=%s rev=%d applied %s → \"%s\"%n",
            doc.getDocId(), rev, transformed, doc.getBuffer().getText());

        // 5. Broadcast transformed op to all other collaborators
        for (Session s : collaborators.values()) {
            if (!s.getUserId().equals(clientOp.getAuthorId())) {
                broadcastSink.send(s.getSessionId(), transformed, rev);
            }
        }

        return rev;
    }

    // ---------------------------------------------------------------- helpers

    private void applyToBuffer(Operation op) {
        if (op instanceof InsertOp) {
            InsertOp ins = (InsertOp) op;
            doc.getBuffer().insert(ins.getPos(), ins.getText());
        } else if (op instanceof DeleteOp) {
            DeleteOp del = (DeleteOp) op;
            doc.getBuffer().delete(del.getPos(), del.getLength());
        }
        // FormatOp: would update a parallel attribute map; text unchanged
    }

    // ---------------------------------------------------------------- accessors

    public Document             getDoc()           { return doc; }
    public Map<String, Session> getCollaborators() { return Collections.unmodifiableMap(collaborators); }
    public int                  liveCount()        { return collaborators.size(); }
}
