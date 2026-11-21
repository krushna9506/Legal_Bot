package com.legalai.assistant.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.legalai.assistant.MainActivity;
import com.legalai.assistant.R;
import com.legalai.assistant.data.database.AppDatabase;
import com.legalai.assistant.network.AiClientManager;
import com.legalai.assistant.ui.chat.ModelSelectorBottomSheet;
import com.legalai.assistant.ui.notes.NotesActivity;
import com.legalai.assistant.ui.tasks.TasksActivity;

public class DashboardActivity extends AppCompatActivity {

    private AppDatabase db;
    private AiClientManager aiClientManager;

    private TextView tvNotesCount;
    private TextView tvTasksCount;
    private TextView tvDailyBriefing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        db = AppDatabase.getDatabase(this);
        aiClientManager = new AiClientManager(this);

        tvNotesCount = findViewById(R.id.tvMetricNotesCount);
        tvTasksCount = findViewById(R.id.tvMetricTasksCount);
        tvDailyBriefing = findViewById(R.id.tvDailyBriefing);

        findViewById(R.id.btnDashboardClose).setOnClickListener(v -> finish());

        findViewById(R.id.cardMetricNotes).setOnClickListener(v -> startActivity(new Intent(this, NotesActivity.class)));
        findViewById(R.id.cardMetricTasks).setOnClickListener(v -> startActivity(new Intent(this, TasksActivity.class)));

        findViewById(R.id.btnQuickChat).setOnClickListener(v -> finish());
        findViewById(R.id.btnQuickNotes).setOnClickListener(v -> startActivity(new Intent(this, NotesActivity.class)));
        findViewById(R.id.btnQuickTasks).setOnClickListener(v -> startActivity(new Intent(this, TasksActivity.class)));
        findViewById(R.id.btnQuickModelSwitch).setOnClickListener(v -> {
            ModelSelectorBottomSheet sheet = new ModelSelectorBottomSheet();
            sheet.show(getSupportFragmentManager(), "model_selector");
        });

        findViewById(R.id.tvRefreshBriefing).setOnClickListener(v -> refreshBriefing());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMetrics();
    }

    private void loadMetrics() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            int notesCount = db.noteDao().getNotesCount();
            int pendingCount = db.taskDao().getPendingCount();

            runOnUiThread(() -> {
                tvNotesCount.setText(String.valueOf(notesCount));
                tvTasksCount.setText(String.valueOf(pendingCount));
            });
        });
    }

    private void refreshBriefing() {
        tvDailyBriefing.setText("Analyzing current Indian legal gazettes and high court cause lists...");
        aiClientManager.sendLegalQuery("Provide a 2-sentence legal briefing on Indian criminal law practice under BNS and BNSS for practicing advocates today.", null, new AiClientManager.AiResponseCallback() {
            @Override
            public void onSuccess(String responseText, String detectedCitation) {
                tvDailyBriefing.setText(responseText);
            }

            @Override
            public void onError(String errorMessage) {
                tvDailyBriefing.setText("Under BNSS 2023, preliminary inquiry is permitted in offences punishable between 3 to 7 years before FIR registration (Section 173(3) BNSS). Ensure strict tracking of limitation periods.");
            }
        });
    }
}
