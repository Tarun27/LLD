package model;

import java.time.Instant;

/**
 * A point-in-time snapshot of the document content at a given revision.
 * Used to seed version-history replay without replaying from revision 0.
 */
public class Snapshot {
    private final String  docId;
    private final long    revision;
    private final String  content;
    private final Instant createdAt;

    public Snapshot(String docId, long revision, String content, Instant createdAt) {
        this.docId     = docId;
        this.revision  = revision;
        this.content   = content;
        this.createdAt = createdAt;
    }

    public String  getDocId()    { return docId; }
    public long    getRevision() { return revision; }
    public String  getContent()  { return content; }
    public Instant getCreatedAt(){ return createdAt; }

    @Override
    public String toString() {
        return "Snapshot{doc='" + docId + "', rev=" + revision + ", content='" + content + "'}";
    }
}
