package com.legalai.assistant.ui.notes;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.legalai.assistant.R;
import com.legalai.assistant.ai.NaturalLanguageActionParser;
import com.legalai.assistant.data.database.AppDatabase;
import com.legalai.assistant.data.model.NoteItem;
import com.legalai.assistant.data.model.TaskItem;
import com.legalai.assistant.network.AiClientManager;

import java.util.List;

public class NoteEditActivity extends AppCompatActivity {

    public static final String EXTRA_NOTE_ID = "extra_note_id";

    private EditText etNoteTitle;
    private EditText etNoteContent;
    private ProgressBar pbLoading;
    private View btnDeleteNote;

    private AppDatabase db;
    private AiClientManager aiClientManager;
    private int noteId = -1;
    private NoteItem currentNote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_edit);

        db = AppDatabase.getDatabase(this);
        aiClientManager = new AiClientManager(this);

        etNoteTitle = findViewById(R.id.etNoteTitle);
        etNoteContent = findViewById(R.id.etNoteContent);
        pbLoading = findViewById(R.id.pbNoteAiLoading);
        btnDeleteNote = findViewById(R.id.btnDeleteNote);

        findViewById(R.id.noteEditToolbar).setOnClickListener(v -> finish());
        findViewById(R.id.btnSaveNote).setOnClickListener(v -> saveNote());
        btnDeleteNote.setOnClickListener(v -> confirmDelete());

        // AI Tools
        findViewById(R.id.btnAiSummarize).setOnClickListener(v -> performAiAction("Summarize this legal case note concisely into Facts, Issues, Governing Statutes (BNS/IPC/CrPC), and Legal Ratio:"));
        findViewById(R.id.btnAiPolish).setOnClickListener(v -> performAiAction("Rewrite and polish the following legal text using formal Indian Court and advocate terminology:"));
        findViewById(R.id.btnAiExtractTasks).setOnClickListener(v -> extractTasksFromNote());

        noteId = getIntent().getIntExtra(EXTRA_NOTE_ID, -1);
        if (noteId != -1) {
            btnDeleteNote.setVisibility(View.VISIBLE);
            loadNote();
        } else {
            btnDeleteNote.setVisibility(View.GONE);
        }
    }

    private void loadNote() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            currentNote = db.noteDao().getNoteById(noteId);
            if (currentNote != null) {
                runOnUiThread(() -> {
                    etNoteTitle.setText(currentNote.getTitle());
                    etNoteContent.setText(currentNote.getContent());
                });
            }
        });
    }

    private void saveNote() {
        String title = etNoteTitle.getText().toString().trim();
        String content = etNoteContent.getText().toString().trim();

        if (title.isEmpty()) {
            title = "Untitled Legal Note";
        }

        long now = System.currentTimeMillis();
        String finalTitle = title;

        AppDatabase.databaseWriteExecutor.execute(() -> {
            if (currentNote == null) {
                NoteItem newNote = new NoteItem(finalTitle, content, now, now, "Legal Brief");
                db.noteDao().insert(newNote);
            } else {
                currentNote.setTitle(finalTitle);
                currentNote.setContent(content);
                currentNote.setUpdatedAt(now);
                db.noteDao().update(currentNote);
            }
            runOnUiThread(() -> {
                Toast.makeText(this, "Legal note saved", Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Legal Note?")
                .setMessage("Are you sure you want to permanently delete this note?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (currentNote != null) {
                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            db.noteDao().delete(currentNote);
                            runOnUiThread(() -> {
                                Toast.makeText(this, "Note deleted", Toast.LENGTH_SHORT).show();
                                finish();
                            });
                        });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performAiAction(String instruction) {
        String content = etNoteContent.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(this, "Please enter some content first", Toast.LENGTH_SHORT).show();
            return;
        }

        pbLoading.setVisibility(View.VISIBLE);
        String fullPrompt = instruction + "\n\n" + content;

        aiClientManager.sendLegalQuery(fullPrompt, null, new AiClientManager.AiResponseCallback() {
            @Override
            public void onSuccess(String responseText, String detectedCitation) {
                pbLoading.setVisibility(View.GONE);
                etNoteContent.setText(responseText);
                Toast.makeText(NoteEditActivity.this, "AI transformation applied", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String errorMessage) {
                pbLoading.setVisibility(View.GONE);
                Toast.makeText(NoteEditActivity.this, "AI error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void extractTasksFromNote() {
        String content = etNoteContent.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(this, "Note content is empty", Toast.LENGTH_SHORT).show();
            return;
        }

        List<NaturalLanguageActionParser.ParsedAction> actions = NaturalLanguageActionParser.parseActions(content);
        if (actions.isEmpty()) {
            // Suggest default 15-day task
            actions.add(new NaturalLanguageActionParser.ParsedAction("TASK",
                    "Follow up on " + etNoteTitle.getText().toString().trim(),
                    "Review progress and statutory filings for this matter.",
                    "In 7 Days",
                    "Medium"));
        }

        AppDatabase.databaseWriteExecutor.execute(() -> {
            for (NaturalLanguageActionParser.ParsedAction action : actions) {
                TaskItem task = new TaskItem(action.title, action.description, action.dueDate, action.priority, false, false, 0);
                db.taskDao().insert(task);
            }
            runOnUiThread(() -> {
                Toast.makeText(this, "Extracted " + actions.size() + " legal tasks into Task Manager!", Toast.LENGTH_LONG).show();
            });
        });
    }
}
