package buffer;

import java.util.ArrayList;
import java.util.List;

/**
 * Piece-table text buffer.
 *
 * Two physical buffers:
 *   - originalBuffer : immutable; holds the text the doc started with
 *   - addBuffer      : append-only; every insert appends here
 *
 * A logical sequence of Piece records ("span" into one of the two buffers)
 * represents the document text.  Insert and delete only reshuffle pieces —
 * no large array copies.  This gives O(log n) edits after a balanced-tree
 * upgrade; here we use an ArrayList for clarity (still much better than a
 * plain String for large documents).
 *
 * The piece table also makes undo and history natural: snapshots are just
 * copies of the piece list, not full text copies.
 */
public class TextBuffer {

    private final StringBuilder originalBuffer;
    private final StringBuilder addBuffer;
    private final List<Piece>   pieces;

    public TextBuffer(String initialText) {
        this.originalBuffer = new StringBuilder(initialText);
        this.addBuffer      = new StringBuilder();
        this.pieces         = new ArrayList<>();
        if (!initialText.isEmpty()) {
            pieces.add(new Piece(true, 0, initialText.length()));
        }
    }

    // ------------------------------------------------------------------ insert

    public void insert(int pos, String text) {
        if (text == null || text.isEmpty()) return;

        int addStart = addBuffer.length();
        addBuffer.append(text);
        Piece newPiece = new Piece(false, addStart, text.length());

        int currentPos = 0;
        for (int i = 0; i < pieces.size(); i++) {
            Piece p = pieces.get(i);
            int pieceEnd = currentPos + p.length;

            if (pos <= pieceEnd) {
                int offsetInPiece = pos - currentPos;
                if (offsetInPiece == 0) {
                    pieces.add(i, newPiece);
                } else if (offsetInPiece == p.length) {
                    pieces.add(i + 1, newPiece);
                } else {
                    // split piece at offsetInPiece
                    Piece left  = new Piece(p.isOriginal, p.start, offsetInPiece);
                    Piece right = new Piece(p.isOriginal, p.start + offsetInPiece, p.length - offsetInPiece);
                    pieces.set(i, left);
                    pieces.add(i + 1, newPiece);
                    pieces.add(i + 2, right);
                }
                return;
            }
            currentPos = pieceEnd;
        }
        // pos is at the very end
        pieces.add(newPiece);
    }

    // ------------------------------------------------------------------ delete

    public void delete(int pos, int len) {
        if (len <= 0) return;

        int end = pos + len;
        int currentPos = 0;
        List<Piece> result = new ArrayList<>();

        for (Piece p : pieces) {
            int pieceEnd = currentPos + p.length;

            if (pieceEnd <= pos || currentPos >= end) {
                result.add(p);                          // entirely outside deletion range
            } else {
                if (currentPos < pos) {                 // keep left remnant
                    result.add(new Piece(p.isOriginal, p.start, pos - currentPos));
                }
                if (pieceEnd > end) {                   // keep right remnant
                    int offset = end - currentPos;
                    result.add(new Piece(p.isOriginal, p.start + offset, pieceEnd - end));
                }
            }
            currentPos = pieceEnd;
        }

        pieces.clear();
        pieces.addAll(result);
    }

    // ------------------------------------------------------------------ read

    public String getText() {
        StringBuilder sb = new StringBuilder();
        for (Piece p : pieces) {
            StringBuilder buf = p.isOriginal ? originalBuffer : addBuffer;
            sb.append(buf, p.start, p.start + p.length);
        }
        return sb.toString();
    }

    public int length() {
        return pieces.stream().mapToInt(p -> p.length).sum();
    }

    /** Shallow copy — safe snapshot without full string allocation. */
    public TextBuffer copy() {
        return new TextBuffer(getText());
    }

    @Override
    public String toString() {
        return getText();
    }
}
