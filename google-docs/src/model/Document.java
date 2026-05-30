package model;

import buffer.TextBuffer;
import java.time.Instant;

public class Document {
    private final String     docId;
    private       String     title;
    private final String     ownerId;
    private       long       currentRevision;
    private final TextBuffer buffer;
    private final Instant    createdAt;

    public Document(String docId, String title, String ownerId) {
        this.docId           = docId;
        this.title           = title;
        this.ownerId         = ownerId;
        this.currentRevision = 0L;
        this.buffer          = new TextBuffer("");
        this.createdAt       = Instant.now();
    }

    public long incrementRevision() {
        return ++currentRevision;
    }

    public String     getDocId()          { return docId; }
    public String     getTitle()          { return title; }
    public void       setTitle(String t)  { this.title = t; }
    public String     getOwnerId()        { return ownerId; }
    public long       getCurrentRevision(){ return currentRevision; }
    public TextBuffer getBuffer()         { return buffer; }
    public Instant    getCreatedAt()      { return createdAt; }

    @Override
    public String toString() {
        return "Document{id='" + docId + "', title='" + title +
               "', rev=" + currentRevision + ", content='" + buffer.getText() + "'}";
    }
}
