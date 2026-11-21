package com.legalai.assistant.network;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.legalai.assistant.knowledge.IndianLegalKnowledge;
import com.legalai.assistant.util.AppPreferences;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AiClientManager {

    public interface AiResponseCallback {
        void onSuccess(String responseText, String detectedCitation);
        void onError(String errorMessage);
    }

    private final Context context;
    private final AppPreferences preferences;
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final Handler mainHandler;

    public AiClientManager(Context context) {
        this.context = context;
        this.preferences = new AppPreferences(context);
        okhttp3.Dns resilientDns = hostname -> {
            try {
                java.util.List<java.net.InetAddress> systemAddresses = okhttp3.Dns.SYSTEM.lookup(hostname);
                if (systemAddresses != null && !systemAddresses.isEmpty()) {
                    return systemAddresses;
                }
            } catch (Exception ignored) {
            }

            // Fallback for Android emulators and local network environments with DNS resolution failure
            if ("generativelanguage.googleapis.com".equalsIgnoreCase(hostname)) {
                java.util.List<java.net.InetAddress> fallback = new java.util.ArrayList<>();
                byte[][] googleIps = new byte[][]{
                        {(byte) 172, (byte) 217, (byte) 116, 4},
                        {(byte) 172, (byte) 217, (byte) 112, 4},
                        {(byte) 172, (byte) 217, (byte) 115, 4},
                        {(byte) 172, (byte) 217, (byte) 119, 4},
                        {(byte) 142, (byte) 250, (byte) 193, 10},
                        {(byte) 142, (byte) 250, (byte) 192, (byte) 202}
                };
                for (byte[] ip : googleIps) {
                    try {
                        fallback.add(java.net.InetAddress.getByAddress(hostname, ip));
                    } catch (Exception ignored) {}
                }
                if (!fallback.isEmpty()) {
                    return fallback;
                }
            } else if ("api.groq.com".equalsIgnoreCase(hostname)) {
                java.util.List<java.net.InetAddress> fallback = new java.util.ArrayList<>();
                byte[][] groqIps = new byte[][]{
                        {104, 18, 2, 107},
                        {104, 18, 3, 107}
                };
                for (byte[] ip : groqIps) {
                    try {
                        fallback.add(java.net.InetAddress.getByAddress(hostname, ip));
                    } catch (Exception ignored) {}
                }
                if (!fallback.isEmpty()) {
                    return fallback;
                }
            }

            throw new java.net.UnknownHostException("Unable to resolve host: " + hostname);
        };

        this.httpClient = new OkHttpClient.Builder()
                .dns(resilientDns)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(25, TimeUnit.SECONDS)
                .writeTimeout(25, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void sendLegalQuery(String userQuery, Uri attachedImageUri, AiResponseCallback callback) {
        String provider = preferences.getProvider();
        String apiKey = preferences.getApiKey(provider);
        String statuteMapping = IndianLegalKnowledge.getStatuteMappingForQuery(userQuery);

        // When offline mode is explicitly selected or no API key is provided, use the offline legal DB
        if (AppPreferences.PROVIDER_OFFLINE.equals(provider) || apiKey.isEmpty()) {
            if (attachedImageUri == null && IndianLegalKnowledge.isGreeting(userQuery)) {
                mainHandler.postDelayed(() -> callback.onSuccess(IndianLegalKnowledge.GREETING_MESSAGE, null), 150);
                return;
            }
            mainHandler.postDelayed(() -> {
                String offlineResponse = IndianLegalKnowledge.generateOfflineLegalResponse(userQuery);
                callback.onSuccess(offlineResponse, statuteMapping);
            }, 300);
            return;
        }

        // Live AI engines: Send query directly to LLM for dynamic, context-aware responses
        switch (provider) {
            case AppPreferences.PROVIDER_GROQ:
                callGroqApi(userQuery, apiKey, statuteMapping, callback);
                break;
            case AppPreferences.PROVIDER_OPENAI:
                callOpenAiApi(userQuery, attachedImageUri, apiKey, statuteMapping, callback);
                break;
            case AppPreferences.PROVIDER_ANTHROPIC:
                callAnthropicApi(userQuery, attachedImageUri, apiKey, statuteMapping, callback);
                break;
            case AppPreferences.PROVIDER_GEMINI:
            default:
                callGeminiApi(userQuery, attachedImageUri, apiKey, statuteMapping, callback);
                break;
        }
    }

    private void callGeminiApi(String query, Uri imageUri, String apiKey, String detectedCitation, AiResponseCallback callback) {
        String chosenModel = preferences.getModel(AppPreferences.PROVIDER_GEMINI);
        if (chosenModel == null || chosenModel.trim().isEmpty() ||
                "gemini-1.5-flash".equalsIgnoreCase(chosenModel) ||
                "gemini-2.0-flash".equalsIgnoreCase(chosenModel)) {
            chosenModel = "gemini-3.1-flash-lite";
        }

        // Priority list: user's chosen model first, then active ultra-fast models
        java.util.List<String> candidateList = new java.util.ArrayList<>();
        candidateList.add(chosenModel);
        if (!candidateList.contains("gemini-3.1-flash-lite")) {
            candidateList.add("gemini-3.1-flash-lite");
        }
        if (!candidateList.contains("gemini-3.5-flash-lite")) {
            candidateList.add("gemini-3.5-flash-lite");
        }
        if (!candidateList.contains("gemini-3-flash-preview")) {
            candidateList.add("gemini-3-flash-preview");
        }
        if (!candidateList.contains("gemini-3.8-flash")) {
            candidateList.add("gemini-3.8-flash");
        }

        final String[] candidateModels = candidateList.toArray(new String[0]);
        tryGeminiModel(query, imageUri, apiKey, detectedCitation, candidateModels, 0, callback);
    }

    private void tryGeminiModel(String query, Uri imageUri, String apiKey, String detectedCitation, String[] models, int index, AiResponseCallback callback) {
        if (index >= models.length) {
            // All remote endpoints unavailable -> Deliver grounded offline analysis with clear indicator
            mainHandler.post(() -> {
                String offlineResponse = IndianLegalKnowledge.generateOfflineLegalResponse(query);
                String note = "*(Offline Fallback - AI network temporarily unavailable)*\n\n" + offlineResponse;
                callback.onSuccess(note, detectedCitation);
            });
            return;
        }

        String model = models[index];
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;
        android.util.Log.e("AiClientManager", "tryGeminiModel: idx=" + index + " model=" + model);

        try {
            JsonObject requestBodyJson = new JsonObject();

            // System instruction
            JsonObject systemInstruction = new JsonObject();
            JsonArray systemParts = new JsonArray();
            JsonObject systemPart = new JsonObject();
            systemPart.addProperty("text", IndianLegalKnowledge.SYSTEM_PROMPT);
            systemParts.add(systemPart);
            systemInstruction.add("parts", systemParts);
            requestBodyJson.add("systemInstruction", systemInstruction);

            // User contents
            JsonArray contents = new JsonArray();
            JsonObject content = new JsonObject();
            content.addProperty("role", "user");
            JsonArray parts = new JsonArray();

            // Multimodal image attachment
            if (imageUri != null) {
                String base64Image = encodeImageToBase64(imageUri);
                if (base64Image != null) {
                    JsonObject imagePart = new JsonObject();
                    JsonObject inlineData = new JsonObject();
                    inlineData.addProperty("mimeType", "image/jpeg");
                    inlineData.addProperty("data", base64Image);
                    imagePart.add("inlineData", inlineData);
                    parts.add(imagePart);
                }
            }

            JsonObject textPart = new JsonObject();
            textPart.addProperty("text", query);
            parts.add(textPart);

            content.add("parts", parts);
            contents.add(content);
            requestBodyJson.add("contents", contents);

            RequestBody body = RequestBody.create(
                    gson.toJson(requestBodyJson),
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("x-goog-api-key", apiKey)
                    .post(body)
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    android.util.Log.e("AiClientManager", "tryGeminiModel onFailure for " + model + ": " + e.getMessage(), e);
                    tryGeminiModel(query, imageUri, apiKey, detectedCitation, models, index + 1, callback);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    int code = response.code();
                    String respStr = response.body() != null ? response.body().string() : "";
                    android.util.Log.e("AiClientManager", "tryGeminiModel response code=" + code + " for " + model + ", body=" + (respStr.length() > 200 ? respStr.substring(0, 200) : respStr));
                    if (!response.isSuccessful()) {
                        response.close();
                        tryGeminiModel(query, imageUri, apiKey, detectedCitation, models, index + 1, callback);
                        return;
                    }

                    try {
                        JsonObject root = gson.fromJson(respStr, JsonObject.class);
                        JsonArray candidates = root != null ? root.getAsJsonArray("candidates") : null;
                        if (candidates != null && candidates.size() > 0) {
                            JsonObject cand = candidates.get(0).getAsJsonObject();
                            JsonObject contentObj = cand != null && cand.has("content") ? cand.getAsJsonObject("content") : null;
                            JsonArray respParts = contentObj != null && contentObj.has("parts") ? contentObj.getAsJsonArray("parts") : null;
                            if (respParts != null && respParts.size() > 0) {
                                StringBuilder answerBuilder = new StringBuilder();
                                for (int i = 0; i < respParts.size(); i++) {
                                    JsonObject part = respParts.get(i).getAsJsonObject();
                                    if (part != null && part.has("text")) {
                                        answerBuilder.append(part.get("text").getAsString());
                                    }
                                }
                                if (answerBuilder.length() > 0) {
                                    String answer = answerBuilder.toString().trim();
                                    android.util.Log.e("AiClientManager", "tryGeminiModel SUCCESS answer length=" + answer.length());
                                    mainHandler.post(() -> callback.onSuccess(answer, detectedCitation));
                                    return;
                                }
                            }
                        }
                        android.util.Log.e("AiClientManager", "tryGeminiModel no candidate parts in response for " + model);
                        tryGeminiModel(query, imageUri, apiKey, detectedCitation, models, index + 1, callback);
                    } catch (Exception ex) {
                        android.util.Log.e("AiClientManager", "tryGeminiModel parse error: " + ex.getMessage(), ex);
                        tryGeminiModel(query, imageUri, apiKey, detectedCitation, models, index + 1, callback);
                    }
                }
            });

        } catch (Exception e) {
            tryGeminiModel(query, imageUri, apiKey, detectedCitation, models, index + 1, callback);
        }
    }

    private void callGroqApi(String query, String apiKey, String detectedCitation, AiResponseCallback callback) {
        String model = preferences.getModel(AppPreferences.PROVIDER_GROQ);
        if (model == null || model.isEmpty() || model.contains("llama-3.3")) {
            model = "openai/gpt-oss-120b";
        }

        String url = "https://api.groq.com/openai/v1/chat/completions";

        try {
            JsonObject requestBodyJson = new JsonObject();
            requestBodyJson.addProperty("model", model);

            JsonArray messages = new JsonArray();

            JsonObject sysMsg = new JsonObject();
            sysMsg.addProperty("role", "system");
            sysMsg.addProperty("content", IndianLegalKnowledge.SYSTEM_PROMPT);
            messages.add(sysMsg);

            JsonObject userMsg = new JsonObject();
            userMsg.addProperty("role", "user");
            userMsg.addProperty("content", query);
            messages.add(userMsg);

            requestBodyJson.add("messages", messages);

            RequestBody body = RequestBody.create(
                    gson.toJson(requestBodyJson),
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .post(body)
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    // Fallback to Gemini
                    String geminiKey = preferences.getApiKey(AppPreferences.PROVIDER_GEMINI);
                    if (!geminiKey.isEmpty()) {
                        callGeminiApi(query, null, geminiKey, detectedCitation, callback);
                    } else {
                        mainHandler.post(() -> callback.onSuccess(IndianLegalKnowledge.generateOfflineLegalResponse(query), detectedCitation));
                    }
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        response.close();
                        String geminiKey = preferences.getApiKey(AppPreferences.PROVIDER_GEMINI);
                        if (!geminiKey.isEmpty()) {
                            callGeminiApi(query, null, geminiKey, detectedCitation, callback);
                        } else {
                            mainHandler.post(() -> callback.onSuccess(IndianLegalKnowledge.generateOfflineLegalResponse(query), detectedCitation));
                        }
                        return;
                    }

                    String respStr = response.body() != null ? response.body().string() : "";
                    try {
                        JsonObject root = gson.fromJson(respStr, JsonObject.class);
                        JsonArray choices = root.getAsJsonArray("choices");
                        if (choices != null && choices.size() > 0) {
                            JsonObject message = choices.get(0).getAsJsonObject().getAsJsonObject("message");
                            String content = message.get("content").getAsString();
                            mainHandler.post(() -> callback.onSuccess(content, detectedCitation));
                            return;
                        }
                        mainHandler.post(() -> callback.onError("No choices returned from Groq."));
                    } catch (Exception ex) {
                        mainHandler.post(() -> callback.onError("Parsing error: " + ex.getMessage()));
                    }
                }
            });

        } catch (Exception e) {
            callback.onError("Request error: " + e.getMessage());
        }
    }

    private void callOpenAiApi(String query, Uri imageUri, String apiKey, String detectedCitation, AiResponseCallback callback) {
        String model = preferences.getModel(AppPreferences.PROVIDER_OPENAI);
        if (model == null || model.isEmpty()) {
            model = "gpt-4o-mini";
        }

        String url = "https://api.openai.com/v1/chat/completions";

        try {
            JsonObject requestBodyJson = new JsonObject();
            requestBodyJson.addProperty("model", model);

            JsonArray messages = new JsonArray();

            JsonObject sysMsg = new JsonObject();
            sysMsg.addProperty("role", "system");
            sysMsg.addProperty("content", IndianLegalKnowledge.SYSTEM_PROMPT);
            messages.add(sysMsg);

            JsonObject userMsg = new JsonObject();
            userMsg.addProperty("role", "user");

            if (imageUri != null) {
                String base64 = encodeImageToBase64(imageUri);
                JsonArray contentArray = new JsonArray();
                JsonObject textObj = new JsonObject();
                textObj.addProperty("type", "text");
                textObj.addProperty("text", query);
                contentArray.add(textObj);

                if (base64 != null) {
                    JsonObject imgObj = new JsonObject();
                    imgObj.addProperty("type", "image_url");
                    JsonObject urlObj = new JsonObject();
                    urlObj.addProperty("url", "data:image/jpeg;base64," + base64);
                    imgObj.add("image_url", urlObj);
                    contentArray.add(imgObj);
                }
                userMsg.add("content", contentArray);
            } else {
                userMsg.addProperty("content", query);
            }

            messages.add(userMsg);
            requestBodyJson.add("messages", messages);

            RequestBody body = RequestBody.create(
                    gson.toJson(requestBodyJson),
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .post(body)
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    mainHandler.post(() -> callback.onError("Network error: " + e.getMessage()));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String respStr = response.body() != null ? response.body().string() : "";
                    if (!response.isSuccessful()) {
                        mainHandler.post(() -> callback.onError("OpenAI error (" + response.code() + "): " + respStr));
                        return;
                    }

                    try {
                        JsonObject root = gson.fromJson(respStr, JsonObject.class);
                        JsonArray choices = root.getAsJsonArray("choices");
                        if (choices != null && choices.size() > 0) {
                            JsonObject message = choices.get(0).getAsJsonObject().getAsJsonObject("message");
                            String content = message.get("content").getAsString();
                            mainHandler.post(() -> callback.onSuccess(content, detectedCitation));
                            return;
                        }
                        mainHandler.post(() -> callback.onError("No content returned from OpenAI."));
                    } catch (Exception ex) {
                        mainHandler.post(() -> callback.onError("Parsing error: " + ex.getMessage()));
                    }
                }
            });

        } catch (Exception e) {
            callback.onError("Request error: " + e.getMessage());
        }
    }

    private void callAnthropicApi(String query, Uri imageUri, String apiKey, String detectedCitation, AiResponseCallback callback) {
        String model = preferences.getModel(AppPreferences.PROVIDER_ANTHROPIC);
        if (model == null || model.isEmpty()) {
            model = "claude-3-5-sonnet-20241022";
        }

        String url = "https://api.anthropic.com/v1/messages";

        try {
            JsonObject requestBodyJson = new JsonObject();
            requestBodyJson.addProperty("model", model);
            requestBodyJson.addProperty("max_tokens", 2048);
            requestBodyJson.addProperty("system", IndianLegalKnowledge.SYSTEM_PROMPT);

            JsonArray messages = new JsonArray();
            JsonObject userMsg = new JsonObject();
            userMsg.addProperty("role", "user");
            userMsg.addProperty("content", query);
            messages.add(userMsg);
            requestBodyJson.add("messages", messages);

            RequestBody body = RequestBody.create(
                    gson.toJson(requestBodyJson),
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("x-api-key", apiKey)
                    .addHeader("anthropic-version", "2023-06-01")
                    .post(body)
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    mainHandler.post(() -> callback.onError("Anthropic network error: " + e.getMessage()));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String respStr = response.body() != null ? response.body().string() : "";
                    if (!response.isSuccessful()) {
                        mainHandler.post(() -> callback.onError("Claude error (" + response.code() + "): " + respStr));
                        return;
                    }

                    try {
                        JsonObject root = gson.fromJson(respStr, JsonObject.class);
                        JsonArray contentArr = root.getAsJsonArray("content");
                        if (contentArr != null && contentArr.size() > 0) {
                            StringBuilder sb = new StringBuilder();
                            for (int i = 0; i < contentArr.size(); i++) {
                                JsonObject block = contentArr.get(i).getAsJsonObject();
                                if (block.has("text")) {
                                    sb.append(block.get("text").getAsString());
                                }
                            }
                            if (sb.length() > 0) {
                                String result = sb.toString().trim();
                                mainHandler.post(() -> callback.onSuccess(result, detectedCitation));
                                return;
                            }
                        }
                        mainHandler.post(() -> callback.onError("Empty response from Claude."));
                    } catch (Exception ex) {
                        mainHandler.post(() -> callback.onError("Parsing error: " + ex.getMessage()));
                    }
                }
            });

        } catch (Exception e) {
            callback.onError("Request error: " + e.getMessage());
        }
    }

    private String encodeImageToBase64(Uri imageUri) {
        try {
            InputStream is = context.getContentResolver().openInputStream(imageUri);
            if (is == null) return null;

            Bitmap bitmap = BitmapFactory.decodeStream(is);
            is.close();

            if (bitmap == null) return null;

            // Compress if dimensions are large
            int maxDim = 1280;
            if (bitmap.getWidth() > maxDim || bitmap.getHeight() > maxDim) {
                float ratio = Math.min((float) maxDim / bitmap.getWidth(), (float) maxDim / bitmap.getHeight());
                int targetW = Math.round(ratio * bitmap.getWidth());
                int targetH = Math.round(ratio * bitmap.getHeight());
                bitmap = Bitmap.createScaledBitmap(bitmap, targetW, targetH, true);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            byte[] bytes = baos.toByteArray();
            return Base64.encodeToString(bytes, Base64.NO_WRAP);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
