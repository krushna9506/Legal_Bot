package com.legalai.assistant.model;

import android.net.Uri;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ChatMessage {
    public static final int TYPE_USER = 1;
    public static final int TYPE_ASSISTANT = 2;

    private int type;
    private String text;
    private String timestamp;
    private Uri imageUri;
    private String statuteCitation;
    private boolean isGrounded;
    private String providerName;
    private boolean isLoading = false;
    private boolean isAnimated = true; // By default existing messages are treated as already animated

    public ChatMessage(int type, String text) {
        this.type = type;
        this.text = text;
        this.timestamp = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
        this.isGrounded = (type == TYPE_ASSISTANT);
        this.isLoading = (type == TYPE_ASSISTANT && (text == null || text.trim().isEmpty()));
        this.isAnimated = !isLoading;
    }

    public int getType() {
        return type;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public Uri getImageUri() {
        return imageUri;
    }

    public void setImageUri(Uri imageUri) {
        this.imageUri = imageUri;
    }

    public String getStatuteCitation() {
        return statuteCitation;
    }

    public void setStatuteCitation(String statuteCitation) {
        this.statuteCitation = statuteCitation;
    }

    public boolean isGrounded() {
        return isGrounded;
    }

    public void setGrounded(boolean grounded) {
        isGrounded = grounded;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public boolean isLoading() {
        return isLoading;
    }

    public void setLoading(boolean loading) {
        this.isLoading = loading;
    }

    public boolean isAnimated() {
        return isAnimated;
    }

    public void setAnimated(boolean animated) {
        this.isAnimated = animated;
    }
}
