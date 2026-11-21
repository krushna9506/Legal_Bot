package com.legalai.assistant.adapter;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.legalai.assistant.R;
import com.legalai.assistant.model.ChatMessage;
import com.legalai.assistant.util.LegalTextFormatter;

import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnSpeakListener {
        void onSpeak(String textToSpeak);
    }

    private final Context context;
    private final List<ChatMessage> messageList;
    private final OnSpeakListener speakListener;
    private RecyclerView recyclerView;

    public ChatAdapter(Context context, List<ChatMessage> messageList, OnSpeakListener speakListener) {
        this.context = context;
        this.messageList = messageList;
        this.speakListener = speakListener;
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
    }

    @Override
    public int getItemViewType(int position) {
        return messageList.get(position).getType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        if (viewType == ChatMessage.TYPE_USER) {
            View view = inflater.inflate(R.layout.item_message_user, parent, false);
            return new UserViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_message_assistant, parent, false);
            return new AssistantViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messageList.get(position);

        if (holder instanceof UserViewHolder) {
            UserViewHolder userHolder = (UserViewHolder) holder;
            userHolder.tvUserMessage.setText(message.getText());
            userHolder.tvUserTimestamp.setText(message.getTimestamp());

            if (message.getImageUri() != null) {
                userHolder.ivUserAttachment.setVisibility(View.VISIBLE);
                userHolder.ivUserAttachment.setImageURI(message.getImageUri());
            } else {
                userHolder.ivUserAttachment.setVisibility(View.GONE);
            }

        } else if (holder instanceof AssistantViewHolder) {
            AssistantViewHolder asstHolder = (AssistantViewHolder) holder;
            asstHolder.cancelPendingAnimations();

            asstHolder.tvAssistantTimestamp.setText(message.getTimestamp());

            if (message.getProviderName() != null && !message.getProviderName().isEmpty()) {
                asstHolder.tvAssistantHeader.setText("NyayaAI Counsel (" + message.getProviderName() + ")");
            } else {
                asstHolder.tvAssistantHeader.setText("NyayaAI Counsel");
            }

            if (message.isLoading()) {
                // Step 1: Real-time drafting state
                asstHolder.layoutDraftingState.setVisibility(View.VISIBLE);
                asstHolder.layoutMessageLines.removeAllViews();
                asstHolder.tvAssistantMessage.setVisibility(View.GONE);
                asstHolder.layoutCitations.setVisibility(View.GONE);
                asstHolder.layoutActions.setVisibility(View.GONE);
                return;
            }

            // Step 2: Content ready
            asstHolder.layoutDraftingState.setVisibility(View.GONE);

            if (message.getStatuteCitation() != null && !message.getStatuteCitation().isEmpty()) {
                asstHolder.layoutCitations.setVisibility(View.VISIBLE);
                asstHolder.tvStatuteTag.setText(message.getStatuteCitation());
            } else {
                asstHolder.layoutCitations.setVisibility(View.GONE);
            }

            List<LegalTextFormatter.Block> blocks = LegalTextFormatter.parseToBlocks(context, message.getText());

            if (message.isAnimated() || blocks.isEmpty()) {
                // Already animated: Render all blocks immediately without animation delay
                asstHolder.layoutMessageLines.removeAllViews();
                for (LegalTextFormatter.Block block : blocks) {
                    TextView tv = LegalTextFormatter.createBlockView(context, block);
                    tv.setVisibility(View.VISIBLE);
                    tv.setAlpha(1f);
                    tv.setTranslationY(0f);
                    asstHolder.layoutMessageLines.addView(tv);
                }
                asstHolder.layoutActions.setVisibility(View.VISIBLE);
                asstHolder.layoutActions.setAlpha(1f);
            } else {
                // Fresh response: Animate line-by-line fading in
                asstHolder.layoutMessageLines.removeAllViews();
                for (LegalTextFormatter.Block block : blocks) {
                    TextView tv = LegalTextFormatter.createBlockView(context, block);
                    tv.setVisibility(View.INVISIBLE);
                    tv.setAlpha(0f);
                    tv.setTranslationY(16f);
                    asstHolder.layoutMessageLines.addView(tv);
                }

                asstHolder.layoutActions.setVisibility(View.GONE);
                asstHolder.layoutActions.setAlpha(0f);

                int childCount = asstHolder.layoutMessageLines.getChildCount();
                long lineDelayMs = 70L;

                for (int i = 0; i < childCount; i++) {
                    final int idx = i;
                    final View child = asstHolder.layoutMessageLines.getChildAt(i);
                    Runnable lineRunnable = () -> {
                        child.setVisibility(View.VISIBLE);
                        child.animate()
                                .alpha(1f)
                                .translationY(0f)
                                .setDuration(220)
                                .setInterpolator(new DecelerateInterpolator())
                                .start();

                        if (recyclerView != null) {
                            recyclerView.smoothScrollToPosition(position);
                        }
                    };
                    asstHolder.pendingRunnables.add(lineRunnable);
                    asstHolder.handler.postDelayed(lineRunnable, idx * lineDelayMs);
                }

                // Completion callback: Reveal action buttons & mark message as animated
                long totalDuration = (childCount * lineDelayMs) + 140L;
                Runnable finishRunnable = () -> {
                    message.setAnimated(true);
                    asstHolder.layoutActions.setVisibility(View.VISIBLE);
                    asstHolder.layoutActions.animate()
                            .alpha(1f)
                            .setDuration(200)
                            .start();
                    if (recyclerView != null) {
                        recyclerView.smoothScrollToPosition(position);
                    }
                };
                asstHolder.pendingRunnables.add(finishRunnable);
                asstHolder.handler.postDelayed(finishRunnable, totalDuration);

                // Quick Tap on bubble to skip animation and instantly reveal all lines
                asstHolder.itemView.setOnClickListener(v -> {
                    if (!message.isAnimated()) {
                        asstHolder.cancelPendingAnimations();
                        for (int k = 0; k < asstHolder.layoutMessageLines.getChildCount(); k++) {
                            View c = asstHolder.layoutMessageLines.getChildAt(k);
                            c.setVisibility(View.VISIBLE);
                            c.setAlpha(1f);
                            c.setTranslationY(0f);
                        }
                        message.setAnimated(true);
                        asstHolder.layoutActions.setVisibility(View.VISIBLE);
                        asstHolder.layoutActions.setAlpha(1f);
                    }
                });
            }

            asstHolder.btnSpeak.setOnClickListener(v -> {
                if (speakListener != null) {
                    speakListener.onSpeak(message.getText());
                }
            });

            asstHolder.btnCopy.setOnClickListener(v -> {
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Legal Draft", message.getText());
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show();
                }
            });

            asstHolder.btnShare.setOnClickListener(v -> {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "NyayaAI Legal Analysis");
                shareIntent.putExtra(Intent.EXTRA_TEXT, message.getText());
                context.startActivity(Intent.createChooser(shareIntent, "Share Legal Instrument"));
            });
        }
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder instanceof AssistantViewHolder) {
            ((AssistantViewHolder) holder).cancelPendingAnimations();
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserMessage;
        TextView tvUserTimestamp;
        ImageView ivUserAttachment;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserMessage = itemView.findViewById(R.id.tvUserMessage);
            tvUserTimestamp = itemView.findViewById(R.id.tvUserTimestamp);
            ivUserAttachment = itemView.findViewById(R.id.ivUserAttachment);
        }
    }

    static class AssistantViewHolder extends RecyclerView.ViewHolder {
        TextView tvAssistantHeader;
        TextView tvAssistantMessage;
        TextView tvAssistantTimestamp;
        TextView tvStatuteTag;
        LinearLayout layoutCitations;
        LinearLayout layoutDraftingState;
        LinearLayout layoutMessageLines;
        LinearLayout layoutActions;
        ProgressBar pbDrafting;
        TextView tvDraftingStatus;
        ImageButton btnSpeak;
        ImageButton btnCopy;
        ImageButton btnShare;

        final Handler handler = new Handler(Looper.getMainLooper());
        final List<Runnable> pendingRunnables = new ArrayList<>();

        AssistantViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAssistantHeader = itemView.findViewById(R.id.tvAssistantHeader);
            tvAssistantMessage = itemView.findViewById(R.id.tvAssistantMessage);
            tvAssistantTimestamp = itemView.findViewById(R.id.tvAssistantTimestamp);
            tvStatuteTag = itemView.findViewById(R.id.tvStatuteTag);
            layoutCitations = itemView.findViewById(R.id.layoutCitations);
            layoutDraftingState = itemView.findViewById(R.id.layoutDraftingState);
            layoutMessageLines = itemView.findViewById(R.id.layoutMessageLines);
            layoutActions = itemView.findViewById(R.id.layoutActions);
            pbDrafting = itemView.findViewById(R.id.pbDrafting);
            tvDraftingStatus = itemView.findViewById(R.id.tvDraftingStatus);
            btnSpeak = itemView.findViewById(R.id.btnSpeak);
            btnCopy = itemView.findViewById(R.id.btnCopy);
            btnShare = itemView.findViewById(R.id.btnShare);
        }

        void cancelPendingAnimations() {
            for (Runnable r : pendingRunnables) {
                handler.removeCallbacks(r);
            }
            pendingRunnables.clear();
        }
    }
}
