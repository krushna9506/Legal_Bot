package com.legalai.assistant.ui.tasks;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.legalai.assistant.R;
import com.legalai.assistant.data.database.AppDatabase;
import com.legalai.assistant.data.model.TaskItem;

import java.util.ArrayList;
import java.util.List;

public class TasksActivity extends AppCompatActivity implements TaskAdapter.TaskActionListener {

    private AppDatabase db;
    private RecyclerView rvTasks;
    private TaskAdapter adapter;
    private LinearLayout layoutEmpty;
    private List<TaskItem> allTasks = new ArrayList<>();
    private int currentFilter = 0; // 0 = all, 1 = pending, 2 = completed

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tasks);

        db = AppDatabase.getDatabase(this);

        Toolbar toolbar = findViewById(R.id.tasksToolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        rvTasks = findViewById(R.id.rvTasks);
        layoutEmpty = findViewById(R.id.layoutEmptyTasks);

        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TaskAdapter(this, allTasks, this);
        rvTasks.setAdapter(adapter);

        findViewById(R.id.fabAddTask).setOnClickListener(v -> TaskDialog.show(this, this::loadTasks));

        Chip chipAll = findViewById(R.id.chipFilterAll);
        Chip chipPending = findViewById(R.id.chipFilterPending);
        Chip chipCompleted = findViewById(R.id.chipFilterCompleted);

        chipAll.setOnClickListener(v -> {
            currentFilter = 0;
            updateFilterChips(chipAll, chipPending, chipCompleted);
            filterList();
        });

        chipPending.setOnClickListener(v -> {
            currentFilter = 1;
            updateFilterChips(chipPending, chipAll, chipCompleted);
            filterList();
        });

        chipCompleted.setOnClickListener(v -> {
            currentFilter = 2;
            updateFilterChips(chipCompleted, chipAll, chipPending);
            filterList();
        });
    }

    private void updateFilterChips(Chip selected, Chip unselected1, Chip unselected2) {
        selected.setChecked(true);
        unselected1.setChecked(false);
        unselected2.setChecked(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTasks();
    }

    private void loadTasks() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<TaskItem> list = db.taskDao().getAllTasks();
            runOnUiThread(() -> {
                allTasks = list;
                filterList();
            });
        });
    }

    private void filterList() {
        List<TaskItem> filtered = new ArrayList<>();
        for (TaskItem t : allTasks) {
            if (currentFilter == 0) {
                filtered.add(t);
            } else if (currentFilter == 1 && !t.isCompleted()) {
                filtered.add(t);
            } else if (currentFilter == 2 && t.isCompleted()) {
                filtered.add(t);
            }
        }
        adapter.updateList(filtered);

        if (filtered.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvTasks.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvTasks.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onToggleCompleted(TaskItem task, boolean isCompleted) {
        task.setCompleted(isCompleted);
        AppDatabase.databaseWriteExecutor.execute(() -> {
            db.taskDao().update(task);
            loadTasks();
        });
    }

    @Override
    public void onDeleteTask(TaskItem task) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            db.taskDao().delete(task);
            loadTasks();
        });
    }
}
