package session;

import model.Range;

/**
 * Represents one live WebSocket connection from an editor.
 *
 * In a real system, Channel would be a WebSocket handle.
 * Here we model it as a simple string identifier for the connection.
 *
 * The client-side OT state machine tracks:
 *   - lastAckedRevision  : the last server revision the client has confirmed
 *   - pendingOps         : ops sent but not yet ACK'd (must be re-transformed
 *                          against incoming BROADCAST_OPs)
 */
public class Session {
    private final String sessionId;
    private final String userId;
    private final String docId;
    private       int    cursorPos;
    private       Range  selection;
    private       long   lastAckedRevision;
    private final String connectionId;        // stands in for WebSocket channel

    public Session(String sessionId, String userId, String docId, String connectionId) {
        this.sessionId        = sessionId;
        this.userId           = userId;
        this.docId            = docId;
        this.connectionId     = connectionId;
        this.cursorPos        = 0;
        this.selection        = new Range(0, 0);
        this.lastAckedRevision= 0;
    }

    public String getSessionId()        { return sessionId; }
    public String getUserId()           { return userId; }
    public String getDocId()            { return docId; }
    public int    getCursorPos()        { return cursorPos; }
    public void   setCursorPos(int p)   { this.cursorPos = p; }
    public Range  getSelection()        { return selection; }
    public void   setSelection(Range s) { this.selection = s; }
    public long   getLastAckedRevision()        { return lastAckedRevision; }
    public void   setLastAckedRevision(long r)  { this.lastAckedRevision = r; }
    public String getConnectionId()     { return connectionId; }

    @Override
    public String toString() {
        return "Session{id='" + sessionId + "', user='" + userId +
               "', doc='" + docId + "', cursor=" + cursorPos + "}";
    }
}
