package com.legalai.assistant.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "ai_models")
public class AiModel {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String modelId;
    private String name;
    private String provider;
    private String description;
    private boolean isPinned;
    private boolean isFree;

    public AiModel(String modelId, String name, String provider, String description, boolean isPinned, boolean isFree) {
        this.modelId = modelId;
        this.name = name;
        this.provider = provider;
        this.description = description;
        this.isPinned = isPinned;
        this.isFree = isFree;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getModelId() { return modelId; }
    public void setModelId(String modelId) { this.modelId = modelId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isPinned() { return isPinned; }
    public void setPinned(boolean pinned) { isPinned = pinned; }

    public boolean isFree() { return isFree; }
    public void setFree(boolean free) { isFree = free; }
}
