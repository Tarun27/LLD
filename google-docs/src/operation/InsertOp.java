package operation;

public class InsertOp extends Operation {
    private int    pos;
    private final String text;

    public InsertOp(String opId, String authorId, String siteId,
                    long baseRevision, int pos, String text) {
        super(opId, authorId, siteId, baseRevision, OpType.INSERT);
        this.pos  = pos;
        this.text = text;
    }

    @Override
    public InsertOp copy() {
        InsertOp c = new InsertOp(opId, authorId, siteId, baseRevision, pos, text);
        c.assignedRevision = this.assignedRevision;
        return c;
    }

    public int    getPos()  { return pos; }
    public String getText() { return text; }
    public void   setPos(int pos) { this.pos = pos; }

    @Override
    public String toString() {
        return "InsertOp{author='" + authorId + "', pos=" + pos +
               ", text='" + text + "', base=" + baseRevision + "}";
    }
}
