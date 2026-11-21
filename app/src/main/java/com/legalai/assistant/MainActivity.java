package com.legalai.assistant;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.legalai.assistant.adapter.ChatAdapter;
import com.legalai.assistant.ai.NaturalLanguageActionParser;
import com.legalai.assistant.data.database.AppDatabase;
import com.legalai.assistant.data.model.TaskItem;
import com.legalai.assistant.model.ChatMessage;
import com.legalai.assistant.network.AiClientManager;
import com.legalai.assistant.service.NotificationHelper;
import com.legalai.assistant.ui.chat.ModelSelectorBottomSheet;
import com.legalai.assistant.ui.dashboard.DashboardActivity;
import com.legalai.assistant.ui.notes.NotesActivity;
import com.legalai.assistant.ui.tasks.TasksActivity;
import com.legalai.assistant.util.AppPreferences;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements ChatAdapter.OnSpeakListener {

    private AppPreferences preferences;
    private AiClientManager aiClientManager;
    private TextToSpeech textToSpeech;

    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private final List<ChatMessage> messageList = new ArrayList<>();

    private LinearLayout layoutEmptyState;
    private LinearLayout disclaimerBanner;
    private LinearLayout attachmentBar;
    private ImageView ivAttachmentPreview;
    private TextView tvAttachmentName;
    private TextView tvModelStatus;
    private EditText etMessage;
    private FloatingActionButton btnSend;
    private ProgressBar progressBarLoading;
    private ImageButton btnCamera;
    private ImageButton btnAttach;
    private ImageButton btnMic;
    private ImageButton btnClearChat;

    private Uri pendingAttachmentUri;
    private Uri cameraTempFileUri;

    // Activity Result Launchers
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (!isGranted) {
                    Toast.makeText(this, "Permission required for this feature", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<String> pickDocumentLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    setAttachedMedia(uri, "Selected Document / Image");
                }
            });

    private final ActivityResultLauncher<Uri> takePhotoLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success && cameraTempFileUri != null) {
                    setAttachedMedia(cameraTempFileUri, "Scanned Legal Document");
                }
            });

    private final ActivityResultLauncher<Intent> speechRecognizerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    ArrayList<String> matches = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    if (matches != null && !matches.isEmpty()) {
                        String voiceQuery = matches.get(0);
                        etMessage.setText(voiceQuery);
                        etMessage.setSelection(voiceQuery.length());
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        preferences = new AppPreferences(this);
        aiClientManager = new AiClientManager(this);
        NotificationHelper.createNotificationChannel(this);

        initViews();
        initTts();
        setupRecyclerView();
        setupQuickChips();
        updateProviderStatusDisplay();

        if (preferences.isDisclaimerDismissed()) {
            disclaimerBanner.setVisibility(View.GONE);
        }
    }

    private void initViews() {
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        disclaimerBanner = findViewById(R.id.disclaimerBanner);
        attachmentBar = findViewById(R.id.attachmentBar);
        ivAttachmentPreview = findViewById(R.id.ivAttachmentPreview);
        tvAttachmentName = findViewById(R.id.tvAttachmentName);
        tvModelStatus = findViewById(R.id.tvModelStatus);
        btnClearChat = findViewById(R.id.btnClearChat);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        progressBarLoading = findViewById(R.id.progressBarLoading);
        btnCamera = findViewById(R.id.btnCamera);
        btnAttach = findViewById(R.id.btnAttach);
        btnMic = findViewById(R.id.btnMic);

        ImageButton btnModelSelector = findViewById(R.id.btnModelSelector);
        if (btnModelSelector != null) {
            btnModelSelector.setOnClickListener(v -> showModelSelector());
        }

        View tvQuickKeyHint = findViewById(R.id.tvQuickKeyHint);
        if (tvQuickKeyHint != null) {
            tvQuickKeyHint.setOnClickListener(v -> showModelSelector());
        }

        findViewById(R.id.btnCloseDisclaimer).setOnClickListener(v -> {
            disclaimerBanner.setVisibility(View.GONE);
            preferences.setDisclaimerDismissed(true);
        });

        findViewById(R.id.btnRemoveAttachment).setOnClickListener(v -> removeAttachedMedia());

        btnSend.setOnClickListener(v -> sendMessage());
        btnCamera.setOnClickListener(v -> launchCamera());
        btnAttach.setOnClickListener(v -> launchDocumentPicker());
        btnMic.setOnClickListener(v -> startVoiceRecognition());
        btnClearChat.setOnClickListener(v -> confirmClearChat());

        tvModelStatus.setOnClickListener(v -> showModelSelector());
    }

    private void initTts() {
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(new Locale("en", "IN"));
            }
        });
    }

    private void setupRecyclerView() {
        chatAdapter = new ChatAdapter(this, messageList, this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        chatRecyclerView.setLayoutManager(layoutManager);
        chatRecyclerView.setAdapter(chatAdapter);
    }

    private void setupQuickChips() {
        Chip chipBnsIpc = findViewById(R.id.chipBnsIpc);
        Chip chipBail = findViewById(R.id.chipBail);
        Chip chipNotice = findViewById(R.id.chipNotice);
        Chip chipAudit = findViewById(R.id.chipAudit);
        Chip chipVoice = findViewById(R.id.chipVoice);

        chipBnsIpc.setOnClickListener(v -> {
            etMessage.setText("What are the key section mappings between BNS 2023 and IPC 1860 for Cheating, Murder, and Theft?");
            etMessage.setSelection(etMessage.getText().length());
        });

        chipBail.setOnClickListener(v -> {
            etMessage.setText("Explain the statutory procedure and landmark Supreme Court principles for Anticipatory Bail under BNSS § 482 (formerly CrPC § 438).");
            etMessage.setSelection(etMessage.getText().length());
        });

        chipNotice.setOnClickListener(v -> showNoticeDrafterBottomSheet());

        chipAudit.setOnClickListener(v -> {
            etMessage.setText("Audit this agreement/contract for legal risks, termination penalties, and dispute resolution jurisdiction under Indian Law.");
            etMessage.setSelection(etMessage.getText().length());
        });

        chipVoice.setOnClickListener(v -> startVoiceRecognition());

        Chip chipDashboard = findViewById(R.id.chipDashboard);
        Chip chipNotes = findViewById(R.id.chipNotes);
        Chip chipTasks = findViewById(R.id.chipTasks);
        Chip chipModel = findViewById(R.id.chipModel);

        if (chipDashboard != null) chipDashboard.setOnClickListener(v -> startActivity(new Intent(this, DashboardActivity.class)));
        if (chipNotes != null) chipNotes.setOnClickListener(v -> startActivity(new Intent(this, NotesActivity.class)));
        if (chipTasks != null) chipTasks.setOnClickListener(v -> startActivity(new Intent(this, TasksActivity.class)));
        if (chipModel != null) chipModel.setOnClickListener(v -> showModelSelector());
    }

    private void setAttachedMedia(Uri uri, String displayName) {
        this.pendingAttachmentUri = uri;
        attachmentBar.setVisibility(View.VISIBLE);
        tvAttachmentName.setText(displayName);
        ivAttachmentPreview.setImageURI(uri);
    }

    private void removeAttachedMedia() {
        this.pendingAttachmentUri = null;
        attachmentBar.setVisibility(View.GONE);
    }

    private void launchCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
            return;
        }

        try {
            File cacheDir = new File(getCacheDir(), "camera");
            if (!cacheDir.exists()) cacheDir.mkdirs();
            File photoFile = File.createTempFile("legal_doc_", ".jpg", cacheDir);
            cameraTempFileUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);
            takePhotoLauncher.launch(cameraTempFileUri);
        } catch (IOException e) {
            Toast.makeText(this, "Could not open camera file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void launchDocumentPicker() {
        pickDocumentLauncher.launch("image/*");
    }

    private void startVoiceRecognition() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
            return;
        }

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.listening_prompt));
        try {
            speechRecognizerLauncher.launch(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Speech recognition is not supported on this device", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendMessage() {
        String query = etMessage.getText().toString().trim();
        if (query.isEmpty() && pendingAttachmentUri == null) {
            return;
        }

        if (query.isEmpty()) {
            query = "Please analyze this attached legal instrument or document under Indian Law.";
        }

        ChatMessage userMsg = new ChatMessage(ChatMessage.TYPE_USER, query);
        if (pendingAttachmentUri != null) {
            userMsg.setImageUri(pendingAttachmentUri);
        }

        messageList.add(userMsg);
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        chatRecyclerView.scrollToPosition(messageList.size() - 1);

        layoutEmptyState.setVisibility(View.GONE);
        chatRecyclerView.setVisibility(View.VISIBLE);

        Uri mediaToSend = pendingAttachmentUri;
        removeAttachedMedia();
        etMessage.setText("");

        // Step 1: Immediately insert real-time drafting bubble into the chat
        String currentProvider = preferences.getProvider();
        ChatMessage asstMsg = new ChatMessage(ChatMessage.TYPE_ASSISTANT, "");
        asstMsg.setLoading(true);
        asstMsg.setProviderName(getProviderDisplayName(currentProvider));
        messageList.add(asstMsg);
        final int asstPosition = messageList.size() - 1;
        chatAdapter.notifyItemInserted(asstPosition);
        chatRecyclerView.smoothScrollToPosition(asstPosition);

        btnSend.setEnabled(false);

        aiClientManager.sendLegalQuery(query, mediaToSend, new AiClientManager.AiResponseCallback() {
            @Override
            public void onSuccess(String responseText, String detectedCitation) {
                progressBarLoading.setVisibility(View.GONE);
                btnSend.setEnabled(true);

                // Step 2: Transition from drafting state to line-by-line animated fade-in
                asstMsg.setLoading(false);
                asstMsg.setText(responseText);
                asstMsg.setStatuteCitation(detectedCitation);
                asstMsg.setProviderName(getProviderDisplayName(currentProvider));
                asstMsg.setAnimated(false);
                chatAdapter.notifyItemChanged(asstPosition);
                chatRecyclerView.smoothScrollToPosition(asstPosition);

                // Auto parse and store legal tasks/deadlines
                List<NaturalLanguageActionParser.ParsedAction> actions = NaturalLanguageActionParser.parseActions(responseText);
                if (!actions.isEmpty()) {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        for (NaturalLanguageActionParser.ParsedAction action : actions) {
                            TaskItem autoTask = new TaskItem(action.title, action.description, action.dueDate, action.priority, false, false, 0);
                            AppDatabase.getDatabase(MainActivity.this).taskDao().insert(autoTask);
                        }
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "💡 " + actions.size() + " legal deadline(s) auto-scheduled in Deadlines tab!", Toast.LENGTH_SHORT).show());
                    });
                }
            }

            @Override
            public void onError(String errorMessage) {
                progressBarLoading.setVisibility(View.GONE);
                btnSend.setEnabled(true);

                asstMsg.setLoading(false);
                asstMsg.setText("⚠ " + errorMessage);
                asstMsg.setProviderName("Error Handler");
                asstMsg.setAnimated(true);
                chatAdapter.notifyItemChanged(asstPosition);
                chatRecyclerView.smoothScrollToPosition(asstPosition);
            }
        });
    }

    @Override
    public void onSpeak(String textToSpeak) {
        if (textToSpeech != null) {
            if (textToSpeech.isSpeaking()) {
                textToSpeech.stop();
            } else {
                // Remove Markdown hashes and formatting symbols for cleaner voice
                String cleanSpeech = textToSpeak.replaceAll("[#*`_]", "").trim();
                textToSpeech.speak(cleanSpeech, TextToSpeech.QUEUE_FLUSH, null, "NyayaTts");
                Toast.makeText(this, "Reading legal analysis aloud...", Toast.LENGTH_SHORT).show();
            }
        }
    }



    private void showNoticeDrafterBottomSheet() {
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_notice_drafter, null);
        sheet.setContentView(view);

        Spinner spinnerNoticeType = view.findViewById(R.id.spinnerNoticeType);
        TextInputEditText etSenderName = view.findViewById(R.id.etSenderName);
        TextInputEditText etRecipientName = view.findViewById(R.id.etRecipientName);
        TextInputEditText etNoticeFacts = view.findViewById(R.id.etNoticeFacts);

        String[] noticeTypes = {
                "Cheque Dishonour Notice (Sec 138 Negotiable Instruments Act)",
                "Tenancy Eviction / Rent Default Notice (Transfer of Property Act)",
                "Consumer Deficiency Notice (Consumer Protection Act, 2019)",
                "Recovery of Outstanding Dues / Demand Notice",
                "Employment Unpaid Salary / Wrongful Termination Notice"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, noticeTypes);
        spinnerNoticeType.setAdapter(adapter);

        view.findViewById(R.id.btnCancelNotice).setOnClickListener(v -> sheet.dismiss());
        view.findViewById(R.id.btnGenerateNotice).setOnClickListener(v -> {
            String selectedType = spinnerNoticeType.getSelectedItem().toString();
            String sender = etSenderName.getText() != null ? etSenderName.getText().toString().trim() : "";
            String recipient = etRecipientName.getText() != null ? etRecipientName.getText().toString().trim() : "";
            String facts = etNoticeFacts.getText() != null ? etNoticeFacts.getText().toString().trim() : "";

            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append("Please draft a formal, court-ready Indian Legal Notice for: ").append(selectedType).append("\n\n");
            promptBuilder.append("Sender Details: ").append(!sender.isEmpty() ? sender : "[Sender Name & Address]").append("\n");
            promptBuilder.append("Recipient Details: ").append(!recipient.isEmpty() ? recipient : "[Recipient Name & Address]").append("\n");
            promptBuilder.append("Core Facts & Transaction: ").append(!facts.isEmpty() ? facts : "Defaulted obligation under Indian law").append("\n\n");
            promptBuilder.append("Requirements:\n");
            promptBuilder.append("1. Include statutory reference (e.g. 15-day period under Section 138 NI Act, or Section 106 Transfer of Property Act).\n");
            promptBuilder.append("2. Formal legal structure with legal reservation of civil and criminal remedies.\n");

            etMessage.setText(promptBuilder.toString());
            sheet.dismiss();
            sendMessage();
        });

        sheet.show();
    }

    private void confirmClearChat() {
        new AlertDialog.Builder(this)
                .setTitle("Start New Consultation?")
                .setMessage("Are you sure you want to clear the active legal conversation?")
                .setPositiveButton("Clear", (dialog, which) -> {
                    messageList.clear();
                    chatAdapter.notifyDataSetChanged();
                    layoutEmptyState.setVisibility(View.VISIBLE);
                    chatRecyclerView.setVisibility(View.GONE);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateProviderStatusDisplay() {
        String provider = preferences.getProvider();
        String model = preferences.getModel(provider);
        String customKeyBadge = preferences.hasCustomApiKey(provider) ? " [Custom Key]" : "";
        String text = getProviderDisplayName(provider) + " (" + model + ")" + customKeyBadge + " • India Law";
        tvModelStatus.setText(text);
    }

    private String getProviderDisplayName(String provider) {
        switch (provider) {
            case AppPreferences.PROVIDER_GROQ:
                return "Groq Fast Cloud";
            case AppPreferences.PROVIDER_OPENAI:
                return "OpenAI Engine";
            case AppPreferences.PROVIDER_ANTHROPIC:
                return "Claude Anthropic";
            case AppPreferences.PROVIDER_OFFLINE:
                return "Offline Indian Legal DB";
            case AppPreferences.PROVIDER_GEMINI:
            default:
                return "Google Gemini";
        }
    }

    private void showModelSelector() {
        ModelSelectorBottomSheet sheet = new ModelSelectorBottomSheet();
        sheet.setOnModelSelectedListener(model -> {
            updateProviderStatusDisplay();
            Toast.makeText(this, "Active Engine: " + model.getName(), Toast.LENGTH_SHORT).show();
        });
        sheet.show(getSupportFragmentManager(), "model_selector");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }
}
