package com.legalai.assistant.util;

import android.content.Context;
import android.content.SharedPreferences;

public class AppPreferences {
    private static final String PREF_NAME = "legal_ai_prefs";
    public static final String PROVIDER_GEMINI = "gemini";
    public static final String PROVIDER_GROQ = "groq";
    public static final String PROVIDER_OPENAI = "openai";
    public static final String PROVIDER_ANTHROPIC = "anthropic";
    public static final String PROVIDER_OFFLINE = "offline";

    private static final String KEY_PROVIDER = "selected_provider";
    private static final String KEY_GEMINI_KEY = "gemini_api_key";
    private static final String KEY_GROQ_KEY = "groq_api_key";
    private static final String KEY_OPENAI_KEY = "openai_api_key";
    private static final String KEY_ANTHROPIC_KEY = "anthropic_api_key";
    private static final String KEY_MODEL = "selected_model";
    private static final String KEY_DISCLAIMER_DISMISSED = "disclaimer_dismissed";

    private final SharedPreferences prefs;

    public AppPreferences(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public String getProvider() {
        return prefs.getString(KEY_PROVIDER, PROVIDER_GEMINI);
    }

    public void setProvider(String provider) {
        prefs.edit().putString(KEY_PROVIDER, provider).apply();
    }

    public String getApiKey(String provider) {
        // Priority 1: User's custom key if entered
        String customKey = getCustomApiKey(provider);
        if (!customKey.isEmpty()) {
            return customKey;
        }

        // Priority 2: Built-in default key from backend
        switch (provider) {
            case PROVIDER_GROQ:
                return com.legalai.assistant.BuildConfig.DEFAULT_GROQ_KEY != null ? com.legalai.assistant.BuildConfig.DEFAULT_GROQ_KEY : "";
            case PROVIDER_OPENAI:
                return com.legalai.assistant.BuildConfig.DEFAULT_OPENAI_KEY != null ? com.legalai.assistant.BuildConfig.DEFAULT_OPENAI_KEY : "";
            case PROVIDER_GEMINI:
            default:
                return com.legalai.assistant.BuildConfig.DEFAULT_GEMINI_KEY != null ? com.legalai.assistant.BuildConfig.DEFAULT_GEMINI_KEY : "";
        }
    }

    public String getCustomApiKey(String provider) {
        switch (provider) {
            case PROVIDER_GROQ:
                return prefs.getString(KEY_GROQ_KEY, "").trim();
            case PROVIDER_OPENAI:
                return prefs.getString(KEY_OPENAI_KEY, "").trim();
            case PROVIDER_ANTHROPIC:
                return prefs.getString(KEY_ANTHROPIC_KEY, "").trim();
            case PROVIDER_GEMINI:
            default:
                return prefs.getString(KEY_GEMINI_KEY, "").trim();
        }
    }

    public boolean hasCustomApiKey(String provider) {
        return !getCustomApiKey(provider).isEmpty();
    }

    public void setApiKey(String provider, String key) {
        String cleanKey = key != null ? key.trim() : "";
        switch (provider) {
            case PROVIDER_GROQ:
                prefs.edit().putString(KEY_GROQ_KEY, cleanKey).apply();
                break;
            case PROVIDER_OPENAI:
                prefs.edit().putString(KEY_OPENAI_KEY, cleanKey).apply();
                break;
            case PROVIDER_ANTHROPIC:
                prefs.edit().putString(KEY_ANTHROPIC_KEY, cleanKey).apply();
                break;
            case PROVIDER_GEMINI:
            default:
                prefs.edit().putString(KEY_GEMINI_KEY, cleanKey).apply();
                break;
        }
    }

    public void clearCustomApiKey(String provider) {
        setApiKey(provider, "");
    }

    public String getModel(String provider) {
        String defaultModel;
        switch (provider) {
            case PROVIDER_GROQ:
                defaultModel = "openai/gpt-oss-120b";
                break;
            case PROVIDER_OPENAI:
                defaultModel = "gpt-4o-mini";
                break;
            case PROVIDER_ANTHROPIC:
                defaultModel = "claude-3-5-sonnet-20241022";
                break;
            case PROVIDER_GEMINI:
            default:
                defaultModel = "gemini-3.1-flash-lite";
                break;
        }
        String model = prefs.getString(KEY_MODEL + "_" + provider, defaultModel);
        if ("gemini-1.5-flash".equals(model) || "gemini-2.0-flash".equals(model) || "llama-3.3-70b-versatile".equals(model)) {
            model = PROVIDER_GROQ.equals(provider) ? "openai/gpt-oss-120b" : "gemini-3.1-flash-lite";
            prefs.edit().putString(KEY_MODEL + "_" + provider, model).apply();
        }
        return model;
    }

    public void setModel(String provider, String model) {
        prefs.edit().putString(KEY_MODEL + "_" + provider, model != null ? model.trim() : "").apply();
    }

    public boolean isDisclaimerDismissed() {
        return prefs.getBoolean(KEY_DISCLAIMER_DISMISSED, false);
    }

    public void setDisclaimerDismissed(boolean dismissed) {
        prefs.edit().putBoolean(KEY_DISCLAIMER_DISMISSED, dismissed).apply();
    }
}
