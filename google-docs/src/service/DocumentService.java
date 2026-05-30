package service;

import model.*;
import operation.Operation;
import session.Session;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * High-level facade for document lifecycle operations.
 *
 * Corresponds to the REST endpoints in the API section:
 *   POST   /documents
 *   GET    /documents/{id}
 *   PUT    /documents/{id}/permissions
 *   GET    /documents/{id}/history
 *   POST   /documents/{id}/restore
 *
 * DocumentService also owns the registry of DocumentSessions:
 * when a client connects via WebSocket, getOrCreateSession(docId) ensures
 * exactly one DocumentSession exists per live document.
 */
public class DocumentService {

    private final Map<String, Document>        documents   = new ConcurrentHashMap<>();
    private final Map<String, DocumentSession> sessions    = new ConcurrentHashMap<>();
    private final Map<String, List<Permission>>permissions = new ConcurrentHashMap<>();
    private final OTEngine                     otEngine;
    private final OperationStore               store;
    private final VersionHistory               history;
    private final PresenceService              presence;

    public DocumentService(OTEngine otEngine, OperationStore store) {
        this.otEngine = otEngine;
        this.store    = store;
        this.history  = new VersionHistory(store);
        this.presence = new PresenceService();
    }

    // ---------------------------------------------------------------- CRUD

    public Document createDocument(String docId, String title, String ownerId) {
        Document doc = new Document(docId, title, ownerId);
        documents.put(docId, doc);
        // Owner gets OWNER permission automatically
        grantPermission(docId, ownerId, Role.OWNER);
        System.out.println("[DocumentService] Created: " + doc);
        return doc;
    }

    public Optional<Document> getDocument(String docId) {
        return Optional.ofNullable(documents.get(docId));
    }

    public void updateTitle(String docId, String requesterId, String newTitle) {
        assertRole(docId, requesterId, Role.EDITOR);
        documents.get(docId).setTitle(newTitle);
    }

    // ---------------------------------------------------------------- Permissions

    public void grantPermission(String docId, String userId, Role role) {
        permissions.computeIfAbsent(docId, k -> new ArrayList<>())
                   .removeIf(p -> p.getUserId().equals(userId));
        permissions.get(docId).add(new Permission(userId, docId, role));
    }

    public Role getRole(String docId, String userId) {
        List<Permission> perms = permissions.getOrDefault(docId, Collections.emptyList());
        return perms.stream()
                    .filter(p -> p.getUserId().equals(userId))
                    .map(Permission::getRole)
                    .findFirst()
                    .orElse(null);
    }

    private void assertRole(String docId, String userId, Role minimum) {
        Role actual = getRole(docId, userId);
        if (actual == null || !hasAtLeast(actual, minimum)) {
            throw new SecurityException("User " + userId + " lacks " + minimum + " on doc " + docId);
        }
    }

    // Enum order: OWNER=0, EDITOR=1, COMMENTER=2, VIEWER=3  → lower ordinal = more privilege
    private boolean hasAtLeast(Role actual, Role required) {
        return actual.ordinal() <= required.ordinal();
    }

    // ---------------------------------------------------------------- WebSocket lifecycle

    /** Called when a client opens a WebSocket to a document. */
    public DocumentSession openSession(String docId, Session clientSession) {
        assertRole(docId, clientSession.getUserId(), Role.VIEWER);

        DocumentSession docSession = sessions.computeIfAbsent(docId, id -> {
            Document doc = documents.get(id);
            if (doc == null) throw new NoSuchElementException("Doc not found: " + id);
            return new DocumentSession(doc, otEngine, store);
        });

        docSession.join(clientSession);
        presence.updatePresence(docId, clientSession.getUserId(),
                                clientSession.getCursorPos(), clientSession.getSelection());
        return docSession;
    }

    /** Called when a client's WebSocket disconnects. */
    public void closeSession(String docId, String sessionId, String userId) {
        DocumentSession ds = sessions.get(docId);
        if (ds != null) {
            ds.leave(sessionId);
            presence.removeUser(docId, userId);
            if (ds.liveCount() == 0) {
                sessions.remove(docId);
                System.out.println("[DocumentService] doc=" + docId + " is now idle");
            }
        }
    }

    // ---------------------------------------------------------------- Edit (SUBMIT_OP)

    /**
     * Authorization-checked wrapper around DocumentSession.applyAndSequence.
     * This is the server handler for the WebSocket SUBMIT_OP message.
     */
    public long submitOp(String docId, String userId, Operation op) {
        assertRole(docId, userId, Role.EDITOR);
        DocumentSession ds = sessions.get(docId);
        if (ds == null) throw new IllegalStateException("No active session for doc: " + docId);
        return ds.applyAndSequence(op);
    }

    // ---------------------------------------------------------------- History / Restore

    public String getContentAtRevision(String docId, long revision) {
        return history.getContentAtRevision(docId, revision);
    }

    public void printHistory(String docId) {
        Document doc = documents.get(docId);
        if (doc != null) history.printHistory(docId, doc.getCurrentRevision());
    }

    public void restoreToRevision(String docId, String requesterId, long targetRevision) {
        assertRole(docId, requesterId, Role.EDITOR);
        String content = history.getContentAtRevision(docId, targetRevision);
        System.out.println("[DocumentService] Restored doc=" + docId +
                           " to rev=" + targetRevision + " content='" + content + "'");
    }

    // ---------------------------------------------------------------- Presence

    public Map<String, PresenceService.PresenceInfo> getPresence(String docId) {
        return presence.getPresence(docId);
    }

    public void updatePresence(String docId, String userId, int cursorPos, model.Range selection) {
        presence.updatePresence(docId, userId, cursorPos, selection);
    }

    // ---------------------------------------------------------------- accessors

    public VersionHistory    getVersionHistory()  { return history; }
    public PresenceService   getPresenceService() { return presence; }
}
