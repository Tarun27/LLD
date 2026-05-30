package model;

public enum Role {
    OWNER,
    EDITOR,
    COMMENTER,
    VIEWER;

    public boolean canEdit() {
        return this == OWNER || this == EDITOR;
    }

    public boolean canComment() {
        return this == OWNER || this == EDITOR || this == COMMENTER;
    }

    public boolean canView() {
        return true;
    }
}
