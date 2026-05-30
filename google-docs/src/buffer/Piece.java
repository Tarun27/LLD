package buffer;

/**
 * One node in the piece table: points into either the original or add buffer.
 */
class Piece {
    boolean isOriginal;
    int     start;
    int     length;

    Piece(boolean isOriginal, int start, int length) {
        this.isOriginal = isOriginal;
        this.start      = start;
        this.length     = length;
    }

    Piece copy() {
        return new Piece(isOriginal, start, length);
    }

    @Override
    public String toString() {
        return (isOriginal ? "orig" : "add") + "[" + start + "," + (start + length) + ")";
    }
}
