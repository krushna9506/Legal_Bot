package com.legalai.assistant.ui.tasks;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.legalai.assistant.R;
import com.legalai.assistant.data.database.AppDatabase;
import com.legalai.assistant.data.model.TaskItem;
import com.legalai.assistant.receiver.AlarmReceiver;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class TaskDialog {

    public interface OnTaskSavedListener {
        void onTaskSaved();
    }

    public static void show(Context context, OnTaskSavedListener listener) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_task_edit, null);
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(view)
                .create();

        TextInputEditText etTitle = view.findViewById(R.id.etTaskTitle);
        TextInputEditText etDesc = view.findViewById(R.id.etTaskDesc);
        TextInputEditText etDueDate = view.findViewById(R.id.etTaskDueDate);
        Spinner spinnerPriority = view.findViewById(R.id.spinnerPriority);
        MaterialSwitch switchReminder = view.findViewById(R.id.switchReminder);

        String[] priorities = {"High", "Medium", "Low"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, priorities);
        spinnerPriority.setAdapter(adapter);

        // Default due date: tomorrow
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, 1);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        etDueDate.setText(sdf.format(cal.getTime()));

        view.findViewById(R.id.btnCancelTask).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btnSaveTask).setOnClickListener(v -> {
            String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
            String desc = etDesc.getText() != null ? etDesc.getText().toString().trim() : "";
            String dueDate = etDueDate.getText() != null ? etDueDate.getText().toString().trim() : "";
            String priority = spinnerPriority.getSelectedItem().toString();
            boolean hasReminder = switchReminder.isChecked();

            if (title.isEmpty()) {
                title = "Statutory Filing Deadline";
            }

            long reminderTime = 0;
            if (hasReminder) {
                // Set alarm for 9:00 AM on due date or 1 minute for demo
                Calendar alarmCal = Calendar.getInstance();
                alarmCal.add(Calendar.MINUTE, 5); // 5 min demo reminder
                reminderTime = alarmCal.getTimeInMillis();
            }

            long finalReminderTime = reminderTime;
            String finalTitle = title;

            AppDatabase.databaseWriteExecutor.execute(() -> {
                TaskItem task = new TaskItem(finalTitle, desc, dueDate, priority, false, hasReminder, finalReminderTime);
                long newId = AppDatabase.getDatabase(context).taskDao().insert(task);

                if (hasReminder) {
                    scheduleAlarm(context, (int) newId, finalTitle, desc, finalReminderTime);
                }

                if (listener != null) {
                    listener.onTaskSaved();
                }
            });

            Toast.makeText(context, "Legal deadline saved", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    private static void scheduleAlarm(Context context, int taskId, String title, String desc, long triggerAtMillis) {
        try {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(context, AlarmReceiver.class);
            intent.putExtra(AlarmReceiver.EXTRA_TASK_ID, taskId);
            intent.putExtra(AlarmReceiver.EXTRA_TITLE, title);
            intent.putExtra(AlarmReceiver.EXTRA_DESC, desc);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context, taskId, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            if (alarmManager != null) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
