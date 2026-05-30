import model.*;
import operation.*;
import service.*;
import session.Session;

import java.util.UUID;

/**
 * End-to-end demo of the Google-Docs LLD.
 *
 * Scenario (straight from the LLD worked example):
 *   Base doc = "HAT", revision 5
 *
 *   User A (siteId="A") inserts "C" at pos 0 → intends "CHAT"   (base rev 5)
 *   User B (siteId="B") inserts "S" at pos 3 → intends "HATS"   (base rev 5)
 *
 *   Both ops are concurrent (same baseRevision).
 *   Server receives A first → applies → "CHAT", rev 6.
 *   Server receives B, transforms against A's op (pos 3 → 4), applies → "CHATS", rev 7.
 *   Both clients converge to "CHATS". Both intentions preserved. No data lost.
 *
 * Additional scenarios:
 *   - Delete / Delete overlap
 *   - Permission enforcement (Viewer cannot edit)
 *   - Version history replay
 */
public class Main {

    public static void main(String[] args) {
        OTEngine       engine  = new OTEngineImpl();
        OperationStore store   = new InMemoryOperationStore();
        DocumentService svc    = new DocumentService(engine, store);

        // -------------------------------------------------------- scenario 1: HAT → CHATS
        System.out.println("══════════════════════════════════════════");
        System.out.println(" Scenario 1 — Concurrent inserts (OT)    ");
        System.out.println("══════════════════════════════════════════");

        Document doc = svc.createDocument("doc-1", "Demo Doc", "owner-1");
        svc.grantPermission("doc-1", "user-A", Role.EDITOR);
        svc.grantPermission("doc-1", "user-B", Role.EDITOR);

        Session sessA = new Session(UUID.randomUUID().toString(), "user-A", "doc-1", "conn-A");
        Session sessB = new Session(UUID.randomUUID().toString(), "user-B", "doc-1", "conn-B");

        DocumentSession ds = svc.openSession("doc-1", sessA);
        svc.openSession("doc-1", sessB);

        // Seed the doc with "HAT" — treated as three sequential inserts from owner
        seedText(svc, ds, "doc-1", "owner-1", "HAT");

        // After seeding, doc is "HAT" at revision 3 (one op per char for clarity).
        // For the worked example, pretend the doc is at revision 5 by adding two more
        // no-content ops — here we just note that baseRevision = currentRevision.
        long baseRev = doc.getCurrentRevision();
        System.out.println("\nDoc after seeding: \"" + doc.getBuffer().getText() +
                           "\" at revision " + baseRev);

        // Concurrent ops, both created against the same baseRevision
        InsertOp opA = new InsertOp(UUID.randomUUID().toString(), "user-A", "A",
                                     baseRev, 0, "C");   // insert "C" at pos 0
        InsertOp opB = new InsertOp(UUID.randomUUID().toString(), "user-B", "B",
                                     baseRev, 3, "S");   // insert "S" at pos 3

        System.out.println("\nUser A submits: " + opA);
        long revA = svc.submitOp("doc-1", "user-A", opA);
        System.out.println("→ ACK rev=" + revA + "  doc=\"" + doc.getBuffer().getText() + "\"");

        System.out.println("\nUser B submits: " + opB);
        long revB = svc.submitOp("doc-1", "user-B", opB);
        System.out.println("→ ACK rev=" + revB + "  doc=\"" + doc.getBuffer().getText() + "\"");

        System.out.println("\nFinal document: \"" + doc.getBuffer().getText() + "\"");
        System.out.println("Expected:       \"CHATS\"");
        System.out.println("Match: " + "CHATS".equals(doc.getBuffer().getText()));

        // -------------------------------------------------------- scenario 2: Delete / Delete overlap
        System.out.println("\n══════════════════════════════════════════");
        System.out.println(" Scenario 2 — Concurrent overlapping deletes");
        System.out.println("══════════════════════════════════════════");

        Document doc2 = svc.createDocument("doc-2", "Delete Test", "owner-1");
        svc.grantPermission("doc-2", "user-A", Role.EDITOR);
        svc.grantPermission("doc-2", "user-B", Role.EDITOR);

        Session sess2A = new Session(UUID.randomUUID().toString(), "user-A", "doc-2", "conn-2A");
        Session sess2B = new Session(UUID.randomUUID().toString(), "user-B", "doc-2", "conn-2B");
        DocumentSession ds2 = svc.openSession("doc-2", sess2A);
        svc.openSession("doc-2", sess2B);

        seedText(svc, ds2, "doc-2", "owner-1", "ABCDE");
        long base2 = doc2.getCurrentRevision();
        System.out.println("\nDoc after seeding: \"" + doc2.getBuffer().getText() +
                           "\" at revision " + base2);

        // A deletes "BCD" (pos=1, len=3), B deletes "CD" (pos=2, len=2) — overlap on "CD"
        DeleteOp delA = new DeleteOp(UUID.randomUUID().toString(), "user-A", "A",
                                      base2, 1, 3);
        DeleteOp delB = new DeleteOp(UUID.randomUUID().toString(), "user-B", "B",
                                      base2, 2, 2);

        System.out.println("\nUser A deletes [1,3): " + delA);
        svc.submitOp("doc-2", "user-A", delA);
        System.out.println("→ doc=\"" + doc2.getBuffer().getText() + "\"");

        System.out.println("\nUser B deletes [2,4) (concurrent, base=" + base2 + "): " + delB);
        svc.submitOp("doc-2", "user-B", delB);
        System.out.println("→ doc=\"" + doc2.getBuffer().getText() + "\"");

        System.out.println("\nFinal document: \"" + doc2.getBuffer().getText() + "\"");
        System.out.println("Expected:       \"AE\"  (A's delete removes BCD; B's CD overlap reduced to no-op)");

        // -------------------------------------------------------- scenario 3: Version history
        System.out.println("\n══════════════════════════════════════════");
        System.out.println(" Scenario 3 — Version history replay     ");
        System.out.println("══════════════════════════════════════════");

        svc.printHistory("doc-1");

        long midRev = revA;
        String atMid = svc.getContentAtRevision("doc-1", midRev);
        System.out.println("Content at rev=" + midRev + ": \"" + atMid + "\"  (should be \"CHAT\")");

        // -------------------------------------------------------- scenario 4: Permission denied
        System.out.println("\n══════════════════════════════════════════");
        System.out.println(" Scenario 4 — Permission enforcement     ");
        System.out.println("══════════════════════════════════════════");

        svc.grantPermission("doc-1", "viewer-X", Role.VIEWER);
        Session sessV = new Session(UUID.randomUUID().toString(), "viewer-X", "doc-1", "conn-V");
        svc.openSession("doc-1", sessV);

        try {
            InsertOp illegalOp = new InsertOp(UUID.randomUUID().toString(), "viewer-X", "X",
                                               doc.getCurrentRevision(), 0, "HACK");
            svc.submitOp("doc-1", "viewer-X", illegalOp);
            System.out.println("ERROR: should have thrown SecurityException");
        } catch (SecurityException e) {
            System.out.println("Correctly blocked viewer edit: " + e.getMessage());
        }

        // -------------------------------------------------------- scenario 5: Presence
        System.out.println("\n══════════════════════════════════════════");
        System.out.println(" Scenario 5 — Presence / cursors         ");
        System.out.println("══════════════════════════════════════════");

        svc.updatePresence("doc-1", "user-A", 3, new Range(3, 3));
        svc.updatePresence("doc-1", "user-B", 5, new Range(4, 6));
        System.out.println("Presence for doc-1:");
        svc.getPresence("doc-1").forEach((uid, info) ->
            System.out.println("  " + info));
    }

    /**
     * Seeds the document by inserting one character at a time from the system/owner,
     * so every character has its own revision.  In a real scenario the initial content
     * would be set via a single bulk-insert or a snapshot.
     */
    private static void seedText(DocumentService svc, DocumentSession ds,
                                  String docId, String ownerId, String text) {
        // Use the DocumentSession directly so we bypass the permission check for the
        // initial owner seed (owner already has OWNER role).
        long baseRev = ds.getDoc().getCurrentRevision();
        for (int i = 0; i < text.length(); i++) {
            InsertOp op = new InsertOp(UUID.randomUUID().toString(), ownerId, "owner",
                                        baseRev + i, i, String.valueOf(text.charAt(i)));
            ds.applyAndSequence(op);
        }
    }
}
