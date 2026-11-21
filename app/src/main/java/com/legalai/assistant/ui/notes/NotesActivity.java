package com.legalai.assistant.ui.notes;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.legalai.assistant.R;
import com.legalai.assistant.data.database.AppDatabase;
import com.legalai.assistant.data.model.NoteItem;

import java.util.ArrayList;
import java.util.List;

public class NotesActivity extends AppCompatActivity {

    private AppDatabase db;
    private RecyclerView rvNotes;
    private NoteAdapter adapter;
    private LinearLayout layoutEmpty;
    private EditText etSearchNotes;
    private List<NoteItem> noteList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);

        db = AppDatabase.getDatabase(this);

        Toolbar toolbar = findViewById(R.id.notesToolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        rvNotes = findViewById(R.id.rvNotes);
        layoutEmpty = findViewById(R.id.layoutEmptyNotes);
        etSearchNotes = findViewById(R.id.etSearchNotes);

        rvNotes.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NoteAdapter(this, noteList, note -> {
            Intent intent = new Intent(this, NoteEditActivity.class);
            intent.putExtra(NoteEditActivity.EXTRA_NOTE_ID, note.getId());
            startActivity(intent);
        });
        rvNotes.setAdapter(adapter);

        findViewById(R.id.fabAddNote).setOnClickListener(v -> {
            Intent intent = new Intent(this, NoteEditActivity.class);
            startActivity(intent);
        });

        etSearchNotes.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterNotes(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotes();
    }

    private void loadNotes() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<NoteItem> list = db.noteDao().getAllNotes();
            runOnUiThread(() -> {
                noteList = list;
                adapter.updateList(list);
                checkEmptyState();
            });
        });
    }

    private void filterNotes(String query) {
        if (query.trim().isEmpty()) {
            adapter.updateList(noteList);
            checkEmptyState();
            return;
        }

        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<NoteItem> filtered = db.noteDao().searchNotes(query.trim());
            runOnUiThread(() -> {
                adapter.updateList(filtered);
                checkEmptyState();
            });
        });
    }

    private void checkEmptyState() {
        if (adapter.getItemCount() == 0) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvNotes.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvNotes.setVisibility(View.VISIBLE);
        }
    }
}
