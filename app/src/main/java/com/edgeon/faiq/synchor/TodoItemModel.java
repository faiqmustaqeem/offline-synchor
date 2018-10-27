package com.edgeon.faiq.synchor;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class TodoItemModel  extends RealmObject{
    private String text;
    @PrimaryKey
    private long timestamp;
    private boolean isSynced;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isSynced() {
        return isSynced;
    }

    public void setSynced(boolean synced) {
        isSynced = synced;
    }
}
