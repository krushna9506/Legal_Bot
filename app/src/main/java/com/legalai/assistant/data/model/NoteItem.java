package com.legalai.assistant.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "notes")
public class NoteItem {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String title;
    private String content;
    private long createdAt;
    private long updatedAt;
    private String tags;

    public NoteItem(String title, String content, long createdAt, long updatedAt, String tags) {
        this.title = title;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.tags = tags;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
}
