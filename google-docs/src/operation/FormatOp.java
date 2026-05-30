package operation;

import java.util.HashMap;
import java.util.Map;

public class FormatOp extends Operation {
    private int pos;
    private int length;
    private final Map<String, Object> attrs;  // e.g. {"bold": true, "color": "#ff0000"}

    public FormatOp(String opId, String authorId, String siteId,
                    long baseRevision, int pos, int length, Map<String, Object> attrs) {
        super(opId, authorId, siteId, baseRevision, OpType.FORMAT);
        this.pos    = pos;
        this.length = length;
        this.attrs  = new HashMap<>(attrs);
    }

    @Override
    public FormatOp copy() {
        FormatOp c = new FormatOp(opId, authorId, siteId, baseRevision, pos, length, attrs);
        c.assignedRevision = this.assignedRevision;
        return c;
    }

    public int               getPos()    { return pos; }
    public int               getLength() { return length; }
    public Map<String,Object>getAttrs()  { return attrs; }
    public void setPos(int pos)          { this.pos    = pos; }
    public void setLength(int length)    { this.length = length; }

    @Override
    public String toString() {
        return "FormatOp{author='" + authorId + "', pos=" + pos +
               ", len=" + length + ", attrs=" + attrs + ", base=" + baseRevision + "}";
    }
}
