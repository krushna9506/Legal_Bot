package com.legalai.assistant.ui.chat;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.legalai.assistant.R;
import com.legalai.assistant.data.model.AiModel;
import com.legalai.assistant.util.AppPreferences;

import java.util.ArrayList;
import java.util.List;

public class ModelSelectorBottomSheet extends BottomSheetDialogFragment {

    public interface OnModelSelectedListener {
        void onModelSelected(AiModel model);
    }

    private OnModelSelectedListener listener;
    private AppPreferences preferences;
    private List<AiModel> allModels;
    private List<AiModel> displayedModels;
    private ModelAdapter adapter;

    private String selectedProvider;
    private String selectedModelId;

    private CardView cvApiKeyCard;
    private TextView tvProviderKeyTitle;
    private TextView tvKeyStatusBadge;
    private TextInputEditText etApiKeyInput;
    private TextView tvKeyHelperLink;

    public void setOnModelSelectedListener(OnModelSelectedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_model_selector, container, false);
        preferences = new AppPreferences(requireContext());

        selectedProvider = preferences.getProvider();
        selectedModelId = preferences.getModel(selectedProvider);

        allModels = getPresetModels();
        displayedModels = new ArrayList<>(allModels);

        RecyclerView rv = view.findViewById(R.id.rvModelSelector);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new ModelAdapter(requireContext(), displayedModels, selectedProvider, selectedModelId, model -> {
            selectedProvider = model.getProvider();
            selectedModelId = model.getModelId();
            preferences.setProvider(selectedProvider);
            preferences.setModel(selectedProvider, selectedModelId);
            adapter.setSelected(selectedProvider, selectedModelId);
            updateApiKeyCard();
            if (listener != null) {
                listener.onModelSelected(model);
            }
        });
        rv.setAdapter(adapter);

        // Filter chips
        ChipGroup chipGroup = view.findViewById(R.id.chipGroupTier);
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chipFilterFree)) {
                filterModels(true, false);
            } else if (checkedIds.contains(R.id.chipFilterPaid)) {
                filterModels(false, true);
            } else {
                filterModels(false, false);
            }
        });

        // API Key Card
        cvApiKeyCard = view.findViewById(R.id.cvApiKeyCard);
        tvProviderKeyTitle = view.findViewById(R.id.tvProviderKeyTitle);
        tvKeyStatusBadge = view.findViewById(R.id.tvKeyStatusBadge);
        etApiKeyInput = view.findViewById(R.id.etApiKeyInput);
        tvKeyHelperLink = view.findViewById(R.id.tvKeyHelperLink);

        Button btnSaveApiKey = view.findViewById(R.id.btnSaveApiKey);
        Button btnResetApiKey = view.findViewById(R.id.btnResetApiKey);

        btnSaveApiKey.setOnClickListener(v -> {
            if (etApiKeyInput.getText() != null) {
                String inputKey = etApiKeyInput.getText().toString().trim();
                if (!inputKey.isEmpty()) {
                    preferences.setApiKey(selectedProvider, inputKey);
                    Toast.makeText(requireContext(), "Custom API Key saved for " + getProviderDisplayName(selectedProvider), Toast.LENGTH_SHORT).show();
                } else {
                    preferences.clearCustomApiKey(selectedProvider);
                    Toast.makeText(requireContext(), "Reverted to default built-in key", Toast.LENGTH_SHORT).show();
                }
                updateApiKeyCard();
            }
        });

        btnResetApiKey.setOnClickListener(v -> {
            preferences.clearCustomApiKey(selectedProvider);
            etApiKeyInput.setText("");
            updateApiKeyCard();
            Toast.makeText(requireContext(), "Using built-in system key", Toast.LENGTH_SHORT).show();
        });

        tvKeyHelperLink.setOnClickListener(v -> openProviderConsole(selectedProvider));

        view.findViewById(R.id.btnApplyModel).setOnClickListener(v -> dismiss());

        updateApiKeyCard();

        return view;
    }

    private void filterModels(boolean freeOnly, boolean paidOnly) {
        displayedModels.clear();
        for (AiModel m : allModels) {
            if (freeOnly) {
                if (m.isFree()) displayedModels.add(m);
            } else if (paidOnly) {
                if (!m.isFree()) displayedModels.add(m);
            } else {
                displayedModels.add(m);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void updateApiKeyCard() {
        if (AppPreferences.PROVIDER_OFFLINE.equals(selectedProvider)) {
            cvApiKeyCard.setVisibility(View.GONE);
            return;
        }
        cvApiKeyCard.setVisibility(View.VISIBLE);

        String providerName = getProviderDisplayName(selectedProvider);
        tvProviderKeyTitle.setText(providerName + " API Key");

        boolean hasCustom = preferences.hasCustomApiKey(selectedProvider);
        if (hasCustom) {
            tvKeyStatusBadge.setText("✓ Custom User Key Active");
            tvKeyStatusBadge.setTextColor(requireContext().getColor(R.color.success_green));
            etApiKeyInput.setText(preferences.getCustomApiKey(selectedProvider));
        } else {
            tvKeyStatusBadge.setText("✓ Default App Key Active");
            tvKeyStatusBadge.setTextColor(requireContext().getColor(R.color.statute_tag_text));
            etApiKeyInput.setText("");
            etApiKeyInput.setHint("Built-in key is active • Paste custom key here");
        }

        switch (selectedProvider) {
            case AppPreferences.PROVIDER_GROQ:
                tvKeyHelperLink.setText("➔ Get 100% Free Groq Key (console.groq.com)");
                break;
            case AppPreferences.PROVIDER_OPENAI:
                tvKeyHelperLink.setText("➔ Get OpenAI Platform Key (platform.openai.com)");
                break;
            case AppPreferences.PROVIDER_ANTHROPIC:
                tvKeyHelperLink.setText("➔ Get Claude Anthropic Key (console.anthropic.com)");
                break;
            case AppPreferences.PROVIDER_GEMINI:
            default:
                tvKeyHelperLink.setText("➔ Get 100% Free Gemini Key from Google AI Studio");
                break;
        }
    }

    private void openProviderConsole(String provider) {
        String url;
        switch (provider) {
            case AppPreferences.PROVIDER_GROQ:
                url = "https://console.groq.com/";
                break;
            case AppPreferences.PROVIDER_OPENAI:
                url = "https://platform.openai.com/api-keys";
                break;
            case AppPreferences.PROVIDER_ANTHROPIC:
                url = "https://console.anthropic.com/";
                break;
            case AppPreferences.PROVIDER_GEMINI:
            default:
                url = "https://aistudio.google.com/";
                break;
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception ignored) {}
    }

    private String getProviderDisplayName(String provider) {
        switch (provider) {
            case AppPreferences.PROVIDER_GROQ:
                return "Groq Cloud";
            case AppPreferences.PROVIDER_OPENAI:
                return "OpenAI";
            case AppPreferences.PROVIDER_ANTHROPIC:
                return "Anthropic Claude";
            case AppPreferences.PROVIDER_OFFLINE:
                return "Offline DB";
            case AppPreferences.PROVIDER_GEMINI:
            default:
                return "Google Gemini";
        }
    }

    public static List<AiModel> getPresetModels() {
        List<AiModel> list = new ArrayList<>();
        // Best Free Models
        list.add(new AiModel("gemini-3.1-flash-lite", "Gemini 3.1 Flash-Lite (Fastest • Recommended)", AppPreferences.PROVIDER_GEMINI, "Lightning-fast Indian legal analysis & instant statutory citation (Free)", true, true));
        list.add(new AiModel("gemini-3.5-flash-lite", "Gemini 3.5 Flash-Lite", AppPreferences.PROVIDER_GEMINI, "High-efficiency legal intelligence & document review (Free)", true, true));
        list.add(new AiModel("gemini-3-flash-preview", "Gemini 3 Flash", AppPreferences.PROVIDER_GEMINI, "Advanced multimodal Indian statutory reasoning (Free)", true, true));
        list.add(new AiModel("gemini-3.8-flash", "Gemini 3.8 Flash (High Quality)", AppPreferences.PROVIDER_GEMINI, "Comprehensive legal drafting & Supreme Court analysis (Free)", true, true));
        list.add(new AiModel("offline-db", "Built-in Indian Law DB", AppPreferences.PROVIDER_OFFLINE, "Offline BNS/IPC mappings & bail guides (Zero internet/key needed)", true, true));

        // Pro & Paid Models
        list.add(new AiModel("gpt-4o", "OpenAI GPT-4o", AppPreferences.PROVIDER_OPENAI, "State-of-the-art multimodal legal reasoning & contract drafting (Pro)", false, true));
        list.add(new AiModel("gpt-4o-mini", "OpenAI GPT-4o Mini", AppPreferences.PROVIDER_OPENAI, "Affordable, high-speed legal intelligence (Pro)", false, true));
        list.add(new AiModel("claude-3-5-sonnet-20241022", "Claude 3.5 Sonnet", AppPreferences.PROVIDER_ANTHROPIC, "Exceptional legal clause analysis & statutory nuance (Pro)", false, false));
        list.add(new AiModel("openai/gpt-oss-120b", "Groq GPT OSS 120B", AppPreferences.PROVIDER_GROQ, "High-performance open weights model for legal research", false, false));

        return list;
    }

    static class ModelAdapter extends RecyclerView.Adapter<ModelAdapter.ViewHolder> {
        private final Context context;
        private final List<AiModel> list;
        private String currentProvider;
        private String currentModelId;
        private final OnModelSelectedListener listener;

        ModelAdapter(Context context, List<AiModel> list, String currentProvider, String currentModelId, OnModelSelectedListener listener) {
            this.context = context;
            this.list = list;
            this.currentProvider = currentProvider;
            this.currentModelId = currentModelId;
            this.listener = listener;
        }

        void setSelected(String provider, String modelId) {
            this.currentProvider = provider;
            this.currentModelId = modelId;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_model_selector, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AiModel model = list.get(position);
            holder.tvModelName.setText(model.getName());
            holder.tvModelDesc.setText(model.getDescription());

            if (model.isFree()) {
                holder.tvModelBadge.setText("🌟 FREE");
                holder.tvModelBadge.setBackgroundResource(R.drawable.bg_tag_statute);
                holder.tvModelBadge.setTextColor(context.getColor(R.color.statute_tag_text));
            } else {
                holder.tvModelBadge.setText("⚡ PRO");
                holder.tvModelBadge.setBackgroundResource(R.drawable.bg_tag_citation);
                holder.tvModelBadge.setTextColor(context.getColor(R.color.citation_tag_text));
            }

            boolean isSelected = model.getProvider().equals(currentProvider) &&
                    (model.getModelId().equals(currentModelId) || AppPreferences.PROVIDER_OFFLINE.equals(model.getProvider()));
            holder.rbSelectedModel.setChecked(isSelected);

            holder.itemView.setOnClickListener(v -> listener.onModelSelected(model));
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvModelName;
            TextView tvModelDesc;
            TextView tvModelBadge;
            RadioButton rbSelectedModel;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvModelName = itemView.findViewById(R.id.tvModelName);
                tvModelDesc = itemView.findViewById(R.id.tvModelDesc);
                tvModelBadge = itemView.findViewById(R.id.tvModelBadge);
                rbSelectedModel = itemView.findViewById(R.id.rbSelectedModel);
            }
        }
    }
}
