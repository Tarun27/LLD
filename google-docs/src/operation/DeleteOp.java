package operation;

public class DeleteOp extends Operation {
    private int pos;
    private int length;

    public DeleteOp(String opId, String authorId, String siteId,
                    long baseRevision, int pos, int length) {
        super(opId, authorId, siteId, baseRevision, OpType.DELETE);
        this.pos    = pos;
        this.length = length;
    }

    @Override
    public DeleteOp copy() {
        DeleteOp c = new DeleteOp(opId, authorId, siteId, baseRevision, pos, length);
        c.assignedRevision = this.assignedRevision;
        return c;
    }

    public int getPos()    { return pos; }
    public int getLength() { return length; }
    public void setPos(int pos)       { this.pos    = pos; }
    public void setLength(int length) { this.length = length; }

    @Override
    public String toString() {
        return "DeleteOp{author='" + authorId + "', pos=" + pos +
               ", len=" + length + ", base=" + baseRevision + "}";
    }
}
