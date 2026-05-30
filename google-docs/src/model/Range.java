package model;

public class Range {
    private final int start;
    private final int end;

    public Range(int start, int end) {
        if (start > end) throw new IllegalArgumentException("start must be <= end");
        this.start = start;
        this.end   = end;
    }

    public int  getStart()  { return start; }
    public int  getEnd()    { return end; }
    public int  length()    { return end - start; }
    public boolean isEmpty(){ return start == end; }

    @Override
    public String toString() {
        return "[" + start + ", " + end + ")";
    }
}
