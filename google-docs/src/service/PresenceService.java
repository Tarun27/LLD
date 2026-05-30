package service;

import model.Range;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks cursor positions and selections for all live collaborators.
 *
 * In production this would be backed by Redis (low TTL, pub/sub for fan-out)
 * so presence state doesn't survive a server restart and is never persisted to
 * the durable op log.
 */
public class PresenceService {

    public static class PresenceInfo {
        public final String userId;
        public       int    cursorPos;
        public       Range  selection;

        public PresenceInfo(String userId, int cursorPos, Range selection) {
            this.userId    = userId;
            this.cursorPos = cursorPos;
            this.selection = selection;
        }

        @Override
        public String toString() {
            return userId + "@" + cursorPos + " sel=" + selection;
        }
    }

    // docId → (userId → PresenceInfo)
    private final Map<String, Map<String, PresenceInfo>> docPresence = new ConcurrentHashMap<>();

    public void updatePresence(String docId, String userId, int cursorPos, Range selection) {
        docPresence
            .computeIfAbsent(docId, k -> new ConcurrentHashMap<>())
            .put(userId, new PresenceInfo(userId, cursorPos, selection));
    }

    /** Returns a snapshot of all collaborators' presence for `docId`. */
    public Map<String, PresenceInfo> getPresence(String docId) {
        return Collections.unmodifiableMap(
            docPresence.getOrDefault(docId, Collections.emptyMap())
        );
    }

    public void removeUser(String docId, String userId) {
        Map<String, PresenceInfo> doc = docPresence.get(docId);
        if (doc != null) doc.remove(userId);
    }

    public void removeDoc(String docId) {
        docPresence.remove(docId);
    }
}
