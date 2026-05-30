package model;

public class Permission {
    private final String userId;
    private final String docId;
    private Role role;

    public Permission(String userId, String docId, Role role) {
        this.userId = userId;
        this.docId  = docId;
        this.role   = role;
    }

    public String getUserId() { return userId; }
    public String getDocId()  { return docId; }
    public Role   getRole()   { return role; }
    public void   setRole(Role role) { this.role = role; }

    @Override
    public String toString() {
        return "Permission{user='" + userId + "', doc='" + docId + "', role=" + role + "}";
    }
}
