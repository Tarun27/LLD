# Google Docs — Collaborative Text Editor (LLD)

A Java implementation of a real-time collaborative document editor modelled after Google Docs.  
The design centres on **Operational Transformation (OT)** for conflict-free concurrent editing, a **piece-table** text buffer for efficient edits, and a **single-writer actor** per document to eliminate distributed locking.

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Package Structure](#2-package-structure)
3. [Entities & Data Model](#3-entities--data-model)
4. [Text Buffer — Piece Table](#4-text-buffer--piece-table)
5. [Operations](#5-operations)
6. [Operational Transformation Engine](#6-operational-transformation-engine)
7. [Services](#7-services)
8. [APIs](#8-apis)
9. [State Transformations](#9-state-transformations)
10. [Concurrency Model](#10-concurrency-model)
11. [Running the Demo](#11-running-the-demo)

---

## 1. Project Overview

### Functional Requirements
| Feature | Description |
|---|---|
| Concurrent editing | Multiple users edit the same document simultaneously in real time |
| Conflict resolution | Concurrent edits to the same region both survive; neither is silently discarded |
| Presence | Each user's cursor position and selection is visible to all collaborators |
| Persistence | All operations are stored in an append-only log |
| Version history | Any past revision can be reconstructed via op-log replay from the nearest snapshot |
| Access control | Four roles — OWNER, EDITOR, COMMENTER, VIEWER — enforced at every write path |
| Restore | A document can be rolled back to any prior revision |

### Non-Functional Requirements
| Property | Mechanism |
|---|---|
| Low local latency | Edits apply **optimistically on the client** immediately; no server round-trip to type |
| Convergence | All clients reach the **same final document state** regardless of network reordering |
| No data loss | OT preserves every user's intention; LWW and locking are explicitly rejected |
| Scalability | Consistent-hash routing on `docId` → one `DocumentSession` actor per document |

---

## 2. Package Structure

```
google-docs/src/
├── buffer/
│   ├── Piece.java               ← span record in the piece table
│   └── TextBuffer.java          ← piece-table text buffer (O(log n) edits)
│
├── model/
│   ├── Document.java            ← docId, title, ownerId, revision counter, TextBuffer
│   ├── User.java                ← userId, name, email
│   ├── Role.java                ← OWNER | EDITOR | COMMENTER | VIEWER
│   ├── Permission.java          ← (userId, docId, Role) triple
│   ├── Range.java               ← [start, end) text range (cursor selections)
│   └── Snapshot.java            ← point-in-time content snapshot at a revision
│
├── operation/
│   ├── OpType.java              ← INSERT | DELETE | FORMAT
│   ├── Operation.java           ← abstract base: opId, authorId, siteId, baseRevision
│   ├── InsertOp.java            ← insert text at a position
│   ├── DeleteOp.java            ← delete a range of characters
│   └── FormatOp.java            ← apply formatting attributes to a range
│
├── session/
│   └── Session.java             ← one live WebSocket connection from an editor
│
├── service/
│   ├── OTEngine.java            ← transform interface
│   ├── OTEngineImpl.java        ← all six transform pairs (Insert/Delete/Format × Insert/Delete)
│   ├── DocumentSession.java     ← single-writer actor for one document
│   ├── OperationStore.java      ← append-only op log + snapshot interface
│   ├── InMemoryOperationStore.java ← in-memory implementation
│   ├── PresenceService.java     ← cursor and selection tracking (Redis in prod)
│   ├── VersionHistory.java      ← snapshot + op-replay to any past revision
│   └── DocumentService.java     ← REST + WebSocket facade; permission enforcement
│
└── Main.java                    ← runnable demo (5 scenarios)
```

---

## 3. Entities & Data Model

### 3.1 User

```java
class User {
    String userId;
    String name;
    String email;
}
```

The identity principal. Authentication (JWT / OAuth) is outside the scope of this LLD; `userId` is taken as already verified at the API gateway.

---

### 3.2 Role

```java
enum Role { OWNER, EDITOR, COMMENTER, VIEWER }
```

| Role | Create | Edit content | Comment | View | Manage permissions |
|---|---|---|---|---|---|
| OWNER | ✅ | ✅ | ✅ | ✅ | ✅ |
| EDITOR | — | ✅ | ✅ | ✅ | — |
| COMMENTER | — | — | ✅ | ✅ | — |
| VIEWER | — | — | — | ✅ | — |

Ordinal in the enum goes from highest privilege (OWNER = 0) to lowest (VIEWER = 3).  
`hasAtLeast(actual, required)` checks `actual.ordinal() <= required.ordinal()`.

---

### 3.3 Permission

```java
class Permission {
    String userId;
    String docId;
    Role   role;
}
```

One row per (user, document) pair. Updated by the owner via `PUT /documents/{id}/permissions`.

---

### 3.4 Document

```java
class Document {
    String     docId;
    String     title;
    String     ownerId;
    long       currentRevision;   // monotonically increasing; assigned by DocumentSession
    TextBuffer buffer;            // live in-memory text (piece table)
    Instant    createdAt;
}
```

`currentRevision` is the single sequence number that drives OT.  
Every call to `DocumentSession.applyAndSequence()` increments it by one.

---

### 3.5 Snapshot

```java
class Snapshot {
    String  docId;
    long    revision;
    String  content;    // full text at this revision
    Instant createdAt;
}
```

Taken periodically (e.g. every 100 ops) so that `VersionHistory` replay does not have to start from revision 0.

---

### 3.6 Session

```java
class Session {
    String sessionId;
    String userId;
    String docId;
    int    cursorPos;
    Range  selection;
    long   lastAckedRevision;
    String connectionId;          // stands in for a WebSocket channel
}
```

One `Session` object exists per live WebSocket connection.  
It is held inside `DocumentSession.collaborators` so the session can receive broadcast ops.

---

### 3.7 Range

```java
class Range {
    int start;   // inclusive
    int end;     // exclusive
}
```

Used for text selections in presence data and `FormatOp`.

---

## 4. Text Buffer — Piece Table

A plain `String` or `char[]` is `O(n)` per insert/delete on large documents.  
The **piece table** is the same data structure used by VS Code and many production editors.

### How it works

Two physical buffers are kept:

| Buffer | Mutability | Purpose |
|---|---|---|
| `originalBuffer` | Immutable after construction | The text the document started with |
| `addBuffer` | Append-only | Every insert appends new text here |

The logical document is represented as an ordered list of **Pieces**:

```
Piece { isOriginal: boolean, start: int, length: int }
```

Each piece points into one of the two physical buffers.  
The visible text is the concatenation of all pieces in order.

### Example

Initial doc: `"HAT"` → one piece: `orig[0,3)`

After `insert(0, "C")`:
- `"C"` appended to `addBuffer` at index 0
- Pieces: `add[0,1)` · `orig[0,3)` → `"CHAT"`

After `insert(4, "S")`:
- `"S"` appended to `addBuffer` at index 1
- Pieces: `add[0,1)` · `orig[0,3)` · `add[1,2)` → `"CHATS"`

### Complexity

| Operation | ArrayList pieces | Balanced-tree pieces |
|---|---|---|
| `insert` | O(n pieces) | O(log n) |
| `delete` | O(n pieces) | O(log n) |
| `getText` | O(n chars) | O(n chars) |

The ArrayList implementation here is clear and correct.  
Replacing it with a red-black tree or a gap buffer would give O(log n) for all mutations.

### Split on insert

```
Before:  [..., P, ...]
          P = orig[5, 10)   (length 5)

insert(7, "XY")   ← pos 7 falls inside P at offset 2

After:   [..., orig[5,7), add[k, k+2), orig[7,10), ...]
```

### Trim on delete

```
Before:  [..., P1, P2, P3, ...]
          P1 = orig[0,3)   P2 = orig[3,8)   P3 = add[0,2)

delete(2, 4)   ← deletes positions 2..5

After:   [..., orig[0,2), orig[5,8), add[0,2), ...]
          ↑ left remnant of P1   ↑ right remnant of P2
```

---

## 5. Operations

Every mutation is represented as an immutable **Operation** object.  
This is the unit of replication, transformation, persistence, and undo.

### 5.1 Base class

```java
abstract class Operation {
    String opId;             // client-generated UUID; used for ACK matching & idempotency
    String authorId;         // the user who created this op
    String siteId;           // deterministic tie-break for concurrent inserts at same position
    long   baseRevision;     // document revision the client was on when op was created
    OpType type;             // INSERT | DELETE | FORMAT
    long   assignedRevision; // set by the server in DocumentSession; 0 until then
}
```

`baseRevision` is the key OT field. The server finds every op with `assignedRevision > baseRevision` — those are the **concurrent ops** — and transforms the incoming op against each one in order.

---

### 5.2 InsertOp

```java
class InsertOp extends Operation {
    int    pos;    // character position in the document where text is inserted
    String text;   // the text to insert (may be multi-character)
}
```

**Example:** `insert(pos=0, text="C")` on `"HAT"` → `"CHAT"`

---

### 5.3 DeleteOp

```java
class DeleteOp extends Operation {
    int pos;     // start of deletion (inclusive)
    int length;  // number of characters to delete
}
```

**Example:** `delete(pos=1, length=3)` on `"ABCDE"` → `"AE"`

---

### 5.4 FormatOp

```java
class FormatOp extends Operation {
    int                 pos;
    int                 length;
    Map<String, Object> attrs;  // e.g. {"bold": true, "color": "#ff0000", "fontSize": 14}
}
```

Formatting ops do not change the text content; they update a parallel attribute map.  
They are transformed against Insert/Delete ops to keep the marked range correct.

---

## 6. Operational Transformation Engine

### Why OT?

| Approach | Simultaneous edit | Data loss | Coordination needed |
|---|---|---|---|
| Pessimistic lock (per block) | ❌ blocked | None | Distributed lock + TTL |
| Last-write-wins | ✅ | ❌ loses edits | Timestamp / clock |
| **OT (central sequencer)** | ✅ | **None** | Server revision counter |
| CRDT | ✅ | None | None |

OT wins for Google-Docs-style editing because it provides simultaneous editing with no data loss and only requires a central server to assign a total order (not to lock).

### CCI correctness model

The transform function must satisfy:

- **Convergence (C):** All replicas that apply the same set of ops end at the same state, regardless of delivery order.
- **Causality (C):** The server assigns a total order; clients respect it by always sending `baseRevision`.
- **Intention (I):** Each op applies at the logically intended position even after concurrent ops have shifted text.

Under server-ordered OT, satisfying **TP1** (transform property 1) is sufficient for convergence.

---

### 6.1 Transform matrix

`transform(incoming, against)` adjusts `incoming` so it can be applied **after** `against` while preserving its original intention.

| incoming \ against | InsertOp | DeleteOp |
|---|---|---|
| **InsertOp** | Position shift + siteId tie-break | Shift left or clamp to deletion start |
| **DeleteOp** | Shift right or expand for in-range inserts | Shrink by overlap |
| **FormatOp** | Shift/expand like DeleteOp | Shrink/shift like Delete×Delete |

---

### 6.2 Insert × Insert

**Rule:** If `against` inserts at a position ≤ `incoming.pos`, shift `incoming.pos` right by `against.text.length`.  
**Tie-break:** When both ops land at the exact same position, the one with the lexicographically **smaller `siteId`** goes first (its text gets the lower index). The other op shifts right by the inserted length. This deterministic rule ensures every replica makes the same choice.

```
Base: "HAT"

against:  insert("C" at 0, siteId="A")
incoming: insert("S" at 3, siteId="B")

against.pos(0) < incoming.pos(3)  →  incoming.pos = 3 + 1 = 4

Apply against → "CHAT"
Apply transformed incoming → "CHAT" + "S" at 4 = "CHATS"  ✓
```

```
// Edge case — same position, tie-break by siteId
against:  insert("X" at 2, siteId="A")
incoming: insert("Y" at 2, siteId="B")

"A" < "B"  →  "A" wins  →  "X" goes at 2, incoming shifts to 3
Result: ...X...Y...   (A's text is left of B's text on every replica)
```

---

### 6.3 Insert × Delete

**Rule:**  
- If the deletion ended **before** `incoming.pos`: shift `incoming.pos` left by `against.length`.  
- If the insertion point is **inside** the deleted range: clamp `incoming.pos` to `against.pos` (the closest surviving position).  
- If the deletion starts **at or after** `incoming.pos`: no change.

```
Base: "ABCDE"

against:  delete(pos=1, len=3)  →  "AE"
incoming: insert("X" at 3)      →  intended to insert after "C"

against covers [1, 4). incoming.pos=3 is inside → clamp to agStart=1.
Apply against → "AE"
Apply transformed incoming → "AXE"   (X lands at the closest surviving spot)
```

---

### 6.4 Delete × Insert

**Rule:**  
- `against` inserts at or **before** `incoming.pos`: shift `incoming.pos` right.  
- `against` inserts **inside** `incoming`'s range: expand `incoming.length` to include the new text (the intent was to delete this span; newly inserted chars inside it are also deleted).  
- `against` inserts **after** `incoming`'s range: no change.

```
Base: "ABCDE"

against:  insert("XY" at 2)     →  "ABXYCDE"
incoming: delete(pos=1, len=3)   →  intended to delete "BCD"

against.pos=2 is inside [1, 4) → expand length by 2
Transformed incoming: delete(pos=1, len=5) → deletes "BXYCD"
Apply against → "ABXYCDE"
Apply transformed incoming → "AE"
```

---

### 6.5 Delete × Delete

**Rule:** Characters that `against` already deleted no longer exist; remove them from `incoming`'s range.

Three sub-cases:

| Relationship | Effect on `incoming` |
|---|---|
| `against` entirely **before** `incoming` | shift `incoming.pos` left by `against.length` |
| `against` entirely **after** `incoming` | no change |
| **Overlap** | `incoming.pos = min(inStart, agStart)`, `incoming.length -= overlap` |

```
Base: "ABCDE"

against:  delete(pos=1, len=3)  →  deletes "BCD"  →  "AE"
incoming: delete(pos=2, len=2)  →  intended to delete "CD"  (base=same revision)

Overlap region: [max(2,1), min(4,4)) = [2,4) = 2 chars
incoming.pos = min(2,1) = 1
incoming.length = 2 - 2 = 0   (no-op; "CD" was already deleted by against)

Apply against → "AE"
Apply transformed incoming (len=0) → "AE"   ✓ idempotent
```

---

### 6.6 Format × Insert / Delete

FormatOp marks a text range with attributes. It follows the same positional logic as Delete:

- **Format × Insert:** if the insert is inside the format range, expand the format range to cover the new text; if before, shift the start.
- **Format × Delete:** reduce the format range by the overlap with the deletion.

---

### 6.7 `transformAgainstAll`

```java
Operation transformAgainstAll(Operation op, List<Operation> concurrent) {
    Operation current = op;
    for (Operation against : concurrent) {   // concurrent ops in revision order
        current = transform(current, against);
    }
    return current;
}
```

The server calls this with the op-log slice `opsSince(docId, op.baseRevision)` as `concurrent`.

---

## 7. Services

### 7.1 DocumentSession — the single-writer actor

This is the architectural centrepiece.

**Principle:** Route all ops for a given `docId` to exactly one `DocumentSession` instance (via consistent-hashing on `docId`). The instance's `synchronized` method call-stack is the serialisation point. No distributed lock is ever needed.

```
                  ┌─────────────────────────────────────┐
                  │         DocumentSession              │
 Client A  ──op──▶│  applyAndSequence(op)  ← synchronized│──▶ ACK(rev=N)
 Client B  ──op──▶│  1. opsSince(baseRev)                │──▶ BROADCAST_OP to A,B,...
                  │  2. transformAgainstAll               │
                  │  3. applyToBuffer                     │
                  │  4. store.append → assigns revision   │
                  │  5. broadcastSink.send                │
                  └─────────────────────────────────────┘
```

`applyAndSequence` in detail:

```java
public synchronized long applyAndSequence(Operation clientOp) {
    // 1. All ops the client hasn't seen (concurrent ops)
    List<Operation> concurrent = store.opsSince(docId, clientOp.getBaseRevision());

    // 2. Transform against each concurrent op in revision order
    Operation transformed = otEngine.transformAgainstAll(clientOp, concurrent);

    // 3. Apply to live TextBuffer
    applyToBuffer(transformed);

    // 4. Persist + assign revision
    long rev = store.append(docId, transformed);
    doc.incrementRevision();

    // 5. Push to every other collaborator's WebSocket
    collaborators.values()
        .filter(s -> !s.getUserId().equals(clientOp.getAuthorId()))
        .forEach(s -> broadcastSink.send(s.getSessionId(), transformed, rev));

    return rev;
}
```

---

### 7.2 OTEngine

Pure, stateless transform logic. No I/O, no dependencies.

```java
interface OTEngine {
    Operation transform(Operation incoming, Operation against);
    Operation transformAgainstAll(Operation op, List<Operation> concurrent);
}
```

`OTEngineImpl` implements all six transform pairs (see §6).

---

### 7.3 OperationStore

Append-only log of all ops, plus periodic snapshots.

```java
interface OperationStore {
    long            append(String docId, Operation op);          // returns assignedRevision
    List<Operation> opsSince(String docId, long fromRevision);   // for transform + history
    Snapshot        latestSnapshot(String docId);                 // seed for history replay
    void            saveSnapshot(Snapshot snapshot);
}
```

`InMemoryOperationStore` — in-memory implementation backed by `ConcurrentHashMap` and `AtomicLong` revision counters. In production this would be a Kafka partition keyed by `docId` (partition offset = revision) or a time-series Cassandra table.

---

### 7.4 PresenceService

Tracks cursor position and text selection for every live collaborator.

```java
void updatePresence(String docId, String userId, int cursorPos, Range selection);
Map<String, PresenceInfo> getPresence(String docId);
void removeUser(String docId, String userId);
```

State is `docId → (userId → PresenceInfo)` held in a `ConcurrentHashMap`.  
In production: backed by Redis with a short TTL so stale cursors expire automatically. Presence updates are broadcast over the same WebSocket channel as ops, but are **not** persisted to the op log.

---

### 7.5 VersionHistory

Reconstructs document content at any historical revision.

```
Algorithm:
  1. Find latestSnapshot(docId) with revision ≤ targetRevision.
  2. Create TextBuffer from snapshot.content (or empty string if no snapshot).
  3. Replay ops from opsSince(docId, snapshot.revision) up to targetRevision.
  4. Return buffer.getText().
```

```java
String getContentAtRevision(String docId, long targetRevision);
void   printHistory(String docId, long upToRevision);
void   takeSnapshot(String docId, long revision, String content);
```

---

### 7.6 DocumentService

The top-level facade. Maps REST endpoints and WebSocket events to the layer below.

```java
// Document lifecycle
Document createDocument(String docId, String title, String ownerId);
Optional<Document> getDocument(String docId);
void updateTitle(String docId, String requesterId, String newTitle);

// Access control
void grantPermission(String docId, String userId, Role role);
Role getRole(String docId, String userId);

// WebSocket lifecycle
DocumentSession openSession(String docId, Session clientSession);
void closeSession(String docId, String sessionId, String userId);

// Edit (SUBMIT_OP handler)
long submitOp(String docId, String userId, Operation op);

// History
String getContentAtRevision(String docId, long revision);
void printHistory(String docId);
void restoreToRevision(String docId, String requesterId, long targetRevision);

// Presence
Map<String, PresenceService.PresenceInfo> getPresence(String docId);
void updatePresence(String docId, String userId, int cursorPos, Range selection);
```

Every write call goes through `assertRole(docId, userId, minimum)` before reaching `DocumentSession`.

---

## 8. APIs

### REST — Document lifecycle

```
POST   /documents                     Body: { title }
                                      → 201 { docId, title, ownerId, createdAt }

GET    /documents/{id}                → 200 { docId, title, currentRevision, content }

PUT    /documents/{id}/title          Body: { title }
                                      → 200

PUT    /documents/{id}/permissions    Body: { userId, role }
                                      → 200  (OWNER only)

GET    /documents/{id}/history        → 200 [ { revision, timestamp, authorId } ]

POST   /documents/{id}/restore        Body: { revision }
                                      → 200 { content }  (EDITOR+ only)
```

### WebSocket — Real-time editing

All messages are JSON over a persistent WebSocket connection to `/ws/documents/{id}`.

**Client → Server**

| Message | Payload | When |
|---|---|---|
| `JOIN` | `{ docId, clientRevision }` | On WebSocket open; server sends back current content + all ops since `clientRevision` |
| `SUBMIT_OP` | `{ docId, op, baseRevision, opId }` | Every keystroke / paste / delete |
| `PRESENCE` | `{ cursorPos, selection }` | On every cursor move or selection change |

**Server → Client**

| Message | Payload | When |
|---|---|---|
| `ACK` | `{ opId, assignedRevision }` | After the server sequences the client's own op |
| `BROADCAST_OP` | `{ op, revision }` | Transformed op from another collaborator |
| `PRESENCE` | `{ userId, cursorPos, selection }` | Another user moved their cursor |
| `JOIN_NOTIFY` | `{ userId, name }` | Another user opened the document |
| `LEAVE_NOTIFY` | `{ userId }` | Another user closed the document |

---

## 9. State Transformations

### 9.1 Document lifecycle

```
                    ┌─────────────┐
                    │   Created   │◀── POST /documents
                    └──────┬──────┘
                           │ first collaborator joins (WebSocket)
                    ┌──────▼──────┐
              ┌────▶│   Active    │◀────────────────────────┐
              │     └──────┬──────┘                         │
              │      │     │ SUBMIT_OP (transform+apply)     │
              │      │     ▼                                 │
              │      │  ┌──────────┐                         │
              │      │  │ Persisted│◀── periodic snapshot    │
              │      │  └──────────┘                         │
              │      │                                        │
              │      │ all collaborators leave               │
              │      ▼                                        │
              │  ┌──────┐   collaborator rejoins             │
              │  │ Idle │────────────────────────────────────┘
              │  └──┬───┘
              │     │ TTL expires or explicit archive
              │  ┌──▼──────┐
              └──│ Archived │
                 └──────────┘
```

### 9.2 Client-side operation lifecycle (OT sync model)

The client maintains three queues:

| Queue | Description |
|---|---|
| **Acknowledged state** | Doc content confirmed by the server |
| **Sent buffer** | Ops submitted to the server, not yet ACK'd |
| **Pending buffer** | Ops typed locally but not yet sent (waiting for ACK of previous op) |

```
User types
     │
     ▼
┌────────────────┐
│  LocalApplied  │ ← applied optimistically to local UI (zero latency)
└───────┬────────┘
        │
  sent-buffer empty?
   ┌────┴────┐
  yes        no
   │          │
   ▼          ▼
┌──────┐  ┌──────────┐
│ Sent │  │ Buffered │ ← will be sent after current op is ACK'd
└──┬───┘  └────┬─────┘
   │            │ previous ACK'd
   │◀───────────┘
   │
   │ server ACK arrives
   ▼
┌─────────────┐
│ Acknowledged│
└─────────────┘

Note: incoming BROADCAST_OPs are transformed against
      (Sent ++ Buffered) before being applied to local UI.
```

### 9.3 Server-side sequencing (per document)

```
Client A                DocumentSession              Client B
   │                         │                          │
   │── SUBMIT_OP ────────────▶│                          │
   │   insert "C"@0, base=3   │                          │
   │                         │◀─ SUBMIT_OP ─────────────│
   │                         │   insert "S"@3, base=3   │
   │                         │                          │
   │                    [A arrives first]               │
   │                         │                          │
   │                    concurrent ops since rev 3: []  │
   │                    transform(A, []) → unchanged    │
   │                    apply "C"@0 → "CHAT", rev=4    │
   │◀── ACK(rev=4) ──────────│                          │
   │                         │── BROADCAST_OP ─────────▶│
   │                         │   insert "C"@0, rev=4   │
   │                         │                          │
   │                    [B arrives next]                │
   │                    concurrent ops since rev 3: [A] │
   │                    transform(B, A):                │
   │                      A.pos(0) < B.pos(3)           │
   │                      B.pos = 3 + 1 = 4             │
   │                    apply "S"@4 → "CHATS", rev=5   │
   │                         │── ACK(rev=5) ───────────▶│
   │◀── BROADCAST_OP ────────│                          │
   │    insert "S"@4, rev=5  │                          │
   │                         │                          │
[A applies B's broadcast,   [Both converge to "CHATS"]  [B sees ACK]
 doc: "CHAT" + "S"@4 = "CHATS"]
```

### 9.4 Version history replay

```
Revision:  0    1    2    3    4    5
Op:             H    A    T    C    S
                @0   @1   @2   @0  @4

Snapshot at rev=3: content="HAT"

Restore to rev=4:
  1. Load snapshot(rev=3, "HAT")
  2. Replay ops since rev=3: [ insert("C"@0, rev=4) ]
  3. Apply → "CHAT"
  4. Return "CHAT"
```

---

## 10. Concurrency Model

### Why not locking?

Locking a block while one user edits it prevents simultaneous editing — which is the defining feature of Google Docs. A distributed lock (Redis Redlock, ZooKeeper) also introduces:

- Lock-holder crash → TTL-based expiry, stale locks
- Network partition → lock unavailable
- Coarse granularity → two users cannot co-write the same paragraph

Locking is appropriate for **coarse, structured documents** (Notion blocks, spreadsheet cells). It is wrong for character-level free text.

### Why not Last-Write-Wins?

LWW resolves conflicts by **discarding** one user's edit. If A and B both edit the same paragraph, one person's changes vanish silently. Acceptable only for coarse metadata (document title, settings) — never for text content.

### OT with a central sequencer — the chosen design

```
"I'd serialize operations per document — either by routing each doc to a
single owning process (actor mailbox as the queue) or via a per-doc
partitioned log. That total order is what OT needs. I would not take a
per-block distributed lock, because that prevents two people editing the
same block, and simultaneous editing is the product."
```

**Serialisation options (increasing scale):**

| Level | Mechanism | Notes |
|---|---|---|
| 1 | `synchronized` method on `DocumentSession` | Single JVM; this implementation |
| 2 | Actor model (Akka, Erlang) | Actor mailbox is the queue; routes by `docId` hash |
| 3 | Kafka partition per docId | Partition offset = revision; durable + replayable |
| 4 | Optimistic CAS on store | `appendIfRevision == expected` as backstop for racing writers |

### What about CRDTs?

CRDTs (Yjs, Automerge, RGA) give convergence **without a central coordinator**.  
They are the right choice when:
- Strong offline editing is required (sync on reconnect)
- P2P / multi-region active-active is needed
- No server is available

Trade-off: per-character metadata (tombstones, unique IDs) uses significantly more memory, and text-position queries are more complex.

---

## 11. Running the Demo

```bash
# Compile
javac -d out -sourcepath src $(find src -name "*.java")

# Run
java -cp out Main
```

### Demo scenarios

| # | Scenario | Expected result |
|---|---|---|
| 1 | Concurrent inserts on "HAT": A inserts "C"@0, B inserts "S"@3 (same baseRevision) | Convergence to **"CHATS"** — both edits survive |
| 2 | Concurrent overlapping deletes on "ABCDE": A deletes [1,4), B deletes [2,4) | **"AE"** — B's op reduces to a no-op (overlap already deleted) |
| 3 | Version history replay — restore to rev after A's op | **"CHAT"** |
| 4 | Viewer attempts to submit an edit | `SecurityException` thrown before reaching `DocumentSession` |
| 5 | Presence update — two users move cursors | Correct cursor positions and selections returned by `PresenceService` |

### Sample output

```
══════════════════════════════════════════
 Scenario 1 — Concurrent inserts (OT)
══════════════════════════════════════════
[DocumentSession] doc=doc-1 rev=4 applied InsertOp{author='user-A', pos=0, text='C'} → "CHAT"
→ ACK rev=4  doc="CHAT"

[DocumentSession] doc=doc-1 rev=5 applied InsertOp{author='user-B', pos=4, text='S'} → "CHATS"
→ ACK rev=5  doc="CHATS"

Final document: "CHATS"
Expected:       "CHATS"
Match: true
```

Note that B's original `pos=3` was transformed to `pos=4` by the OT engine before being applied — the position shifted right to account for A's insertion of "C" at position 0.
